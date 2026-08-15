import Foundation
import Combine
import FirebaseAuth
import FirebaseFirestore
import UIKit
import CoreLocation

@MainActor
final class AppViewModel: ObservableObject {
    private static let dropPreviewRadiusMeters: CLLocationDistance = 30
    /// How far the Nearby list reaches for ambient public drops. Fixed, and identical to
    /// Android's NEARBY_LIST_RADIUS_METERS — the two clients must agree about how far
    /// "nearby" reaches, or two attendees standing together see different lists.
    /// Drops in an experience the user joined, and business drops, ignore this entirely.
    private static let nearbyListRadiusMeters: CLLocationDistance = 300
    /// A fix older than this is not trusted for a pickup. Matches
    /// `DropDecisionReceiver.LOCATION_STALE_THRESHOLD_MILLIS` on Android.
    private static let locationStaleThresholdSeconds: TimeInterval = 120
    enum AuthState {
        case loading
        case signedOut
        case signedIn(UserSession)
    }
    
    enum UserMode: String {
        case guest
        case signedIn

        var isReadOnly: Bool { self != .signedIn }
        var canParticipate: Bool { self == .signedIn }
    }

    struct UserSession {
        var user: AuthenticatedUser
        var profile: UserProfile
    }

    struct AuthenticatedUser {
        let uid: String
        let email: String?
        let displayName: String?
    }
    
    struct LikePermission {
        let allowed: Bool
        let message: String?
    }

    enum DropActionError: LocalizedError {
        case notSignedIn
        case invalidInput(String)
        case missingDrop
        case missingCreator

        var errorDescription: String? {
            switch self {
            case .notSignedIn:
                return "Sign in to continue."
            case .invalidInput(let message):
                return message
            case .missingDrop:
                return "Drop information is missing."
            case .missingCreator:
                return "Creator information is unavailable."
            }
        }
    }

    @Published private(set) var authState: AuthState = .loading
    @Published private(set) var userMode: UserMode?
    @Published private(set) var hasAcceptedTerms: Bool
    @Published private(set) var hasCompletedOnboarding: Bool
    @Published var groups: [GroupMembership] = []
    @Published var selectedGroupCode: String? {
        didSet {
            guard selectedGroupCode != oldValue else { return }
            Task { [weak self] in
                await self?.refreshDrops()
            }
        }
    }
    @Published var drops: [Drop] = [] {
        didSet { rebuildExplorerCollections() }
    }
    @Published private(set) var blockedCreatorIDs: Set<String> = []
    @Published private(set) var inventory: NoteInventoryService.Inventory
    @Published private(set) var explorerMyDrops: [Drop] = []
    @Published private(set) var explorerCollectedDrops: [Drop] = []
    @Published private(set) var legalManifest: LegalPolicyManifest?
    @Published private(set) var legalManifestError: String?
    @Published private(set) var isLoadingLegalManifest: Bool = false
    @Published private(set) var isRecordingLegalAcceptance: Bool = false
    @Published var selectedExplorerDestination: ExplorerDestination = .nearby
    @Published var explorerRestrictionMessage: String?
    /// Drops whose proximity was proven this session by a precise fix taken at the
    /// moment of the attempt. Only the fact of the unlock is kept, never the position.
    @Published private(set) var unlockedDropIDs: Set<String> = []
    @Published private(set) var unlockingDropID: String?
    @Published private(set) var nearbyAlertsEnabled: Bool
    @Published var errorMessage: String?
    @Published var isPerformingAction: Bool = false
    @Published var pendingAccountRole: UserRole?
    @Published var isAuthenticating: Bool = false
    @Published var isGoogleSigningIn: Bool = false
    @Published var authFlowError: String?
    @Published var authFlowStatus: String?
    @Published var isShowingAccountMenu: Bool = false
    @Published var isShowingAccountData: Bool = false
    @Published var isShowingDropComposer: Bool = false
    @Published var isShowingGroupMenu: Bool = false
    @Published var isShowingGroupManagement: Bool = false
    @Published var isShowingTutorialSlides: Bool = false
    @Published var isShowingFaq: Bool = false
    @Published var infoMenuURL: URL?
    @Published private(set) var lastAccountDeletionReceipt: AccountDeletionReceipt?

    private let authService = AuthService.shared
    private let firestore = FirestoreService.shared
    private let messagingService = MessagingService.shared
    private let locationService = LocationService.shared
    private let legalConsentService = LegalConsentService.shared
    private let featureFlags = PilotFeatureFlags.shared
    private lazy var safeSearch = SafeSearchService()
    private let inventoryService = NoteInventoryService.shared
    private let notificationPreferences: NotificationPreferences
    private var groupListener: ListenerRegistration?
    private var dropsListener: ListenerRegistration?
    private var cancellables: Set<AnyCancellable> = []
    private let defaults: UserDefaults
    private var inventoryUserId: String?

    private enum DefaultsKeys {
        static let acceptedLegalVersion = "geodrop.acceptedLegalVersion"
        static let serverAcceptedLegalVersionPrefix = "geodrop.serverAcceptedLegalVersion."
        static let completedOnboarding = "geodrop.onboardingCompleted"
        static let userMode = "geodrop.userMode"
    }

    init(userDefaults: UserDefaults = .standard) {
        self.defaults = userDefaults
        let initialUserId = authService.currentUser?.uid
        self.inventoryUserId = initialUserId
        self.inventory = inventoryService.inventory(for: initialUserId)
        let storedNotificationPreferences = NotificationPreferences(userDefaults: userDefaults)
        self.notificationPreferences = storedNotificationPreferences
        self.nearbyAlertsEnabled = PilotFeatureFlags.shared.notificationsEnabled &&
            storedNotificationPreferences.nearbyAlertsEnabled()
        self.legalManifest = nil
        self.legalManifestError = nil
        // Acceptance is version-bound and cannot be restored until the approved
        // server manifest has loaded successfully.
        self.hasAcceptedTerms = false
        let onboarding = userDefaults.bool(forKey: DefaultsKeys.completedOnboarding)
        self.hasCompletedOnboarding = onboarding
        if let rawMode = userDefaults.string(forKey: DefaultsKeys.userMode),
           let restoredMode = UserMode(rawValue: rawMode) {
            if restoredMode == .signedIn, authService.currentUser == nil {
                self.userMode = nil
            } else {
                self.userMode = restoredMode
            }
        } else {
            self.userMode = nil
        }
        
        NotificationCenter.default.publisher(for: NoteInventoryService.inventoryDidChangeNotification)
            .compactMap { $0.userInfo?[NoteInventoryService.NotificationKeys.userIdentifier] as? String }
            .receive(on: DispatchQueue.main)
            .sink { [weak self] identifier in
                guard let self else { return }
                if identifier == self.inventoryService.storageIdentifier(for: self.inventoryUserId) {
                    self.inventory = self.inventoryService.inventory(for: self.inventoryUserId)
                    self.rebuildExplorerCollections()
                }
            }
            .store(in: &cancellables)

        NotificationCenter.default.publisher(for: PilotFeatureFlags.didUpdateNotification)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                guard let self else { return }
                if !self.featureFlags.notificationsEnabled {
                    self.notificationPreferences.setNearbyAlertsEnabled(false)
                    self.nearbyAlertsEnabled = false
                }
                self.objectWillChange.send()
                Task { await self.refreshDrops() }
            }
            .store(in: &cancellables)
        
        rebuildExplorerCollections()
    }
    
    var currentUserID: String? {
        if case let .signedIn(session) = authState {
            return session.user.uid
        }
        return nil
    }
    
    func distanceToDrop(_ drop: Drop) -> CLLocationDistance? {
        guard let location = locationService.currentLocation else { return nil }
        let dropLocation = CLLocation(latitude: drop.latitude, longitude: drop.longitude)
        return location.distance(from: dropLocation)
    }

    /// Tasks 3.2/3.3 — `currentLocation` is an approximate fix now, so it cannot decide
    /// a 30 m question. Content is revealed only once `attemptUnlock` has proven
    /// proximity with a precise fix.
    func canPreview(drop: Drop, distance: CLLocationDistance? = nil) -> Bool {
        if isOwner(of: drop) { return true }
        if hasCollected(drop: drop) { return true }
        return unlockedDropIDs.contains(drop.id)
    }

    func previewRestrictionMessage(for drop: Drop, distance: CLLocationDistance? = nil) -> String? {
        if isOwner(of: drop) { return nil }
        if hasCollected(drop: drop) { return nil }

        let resolvedDistance = distance ?? distanceToDrop(drop)
        if let resolvedDistance {
            guard resolvedDistance > Self.dropPreviewRadiusMeters else { return nil }
            return nil
        }
        return "Enable location services to preview this drop."
    }

    func bootstrap() {
        // Refresh existing grants without displaying a system prompt at startup.
        // Nearby discovery and alert setup own their contextual request flows.
        messagingService.refreshAuthorizationStatus()
        locationService.refreshAuthorizationStatus()
        Task { await refreshLegalManifest() }

        messagingService.$currentToken
            .receive(on: DispatchQueue.main)
            .sink { [weak self] token in
                Task { await self?.syncMessagingToken(token) }
            }
            .store(in: &cancellables)

        authService.observeAuthChanges { [weak self] user in
            Task { await self?.handleAuthChange(user: user) }
        }
    }
    
    var isAuthLoading: Bool {
        if case .loading = authState {
            return true
        }
        return false
    }
    
    func setExplorerDestination(_ destination: ExplorerDestination) {
        guard canAccess(destination: destination) else {
            explorerRestrictionMessage = destination.restrictionMessage(for: userMode)
            return
        }

        explorerRestrictionMessage = nil
        selectedExplorerDestination = destination
    }

    func explorerDrops(for destination: ExplorerDestination) -> [Drop] {
        switch destination {
        case .nearby:
            let ignoredIDs = inventory.ignoredDropIDs
            let collectedIDs = Set(inventory.collectedDrops.keys)
            let userID = currentUserID

            guard let currentLocation = locationService.currentLocation else {
                return []
            }

            return drops.filter { drop in
                guard !ignoredIDs.contains(drop.id) else { return false }
                guard !collectedIDs.contains(drop.id) else { return false }
                guard !drop.hasBeenCollected else { return false }

                // A drop in an experience the user joined is nearby by definition — the
                // event supplies the bounded geography. Only ambient public drops are
                // distance-bounded. This filter was commented out entirely, which left
                // the iOS Nearby list unbounded while Android's was not.
                let isExperienceDrop = !(drop.groupCode ?? "").isEmpty
                if !isExperienceDrop && drop.dropType == .community {
                    let dropLocation = CLLocation(latitude: drop.latitude, longitude: drop.longitude)
                    guard currentLocation.distance(from: dropLocation)
                        <= Self.nearbyListRadiusMeters else { return false }
                }

                // Keep `drops` as the authoritative source of truth so that the
                // explorer collections can still build the "My Drops" and
                // "Collected" tabs. The Nearby view applies additional
                // filtering to hide the signed-in user's drops as well as drops
                // they have reported.
                if let userID, !userID.isEmpty {
                    if drop.createdBy == userID { return false }
                    if drop.reportedBy[userID] != nil { return false }
                }

                return true
            }
        case .myDrops:
            return explorerMyDrops
        case .collected:
            return explorerCollectedDrops
        }
    }

    func explorerCount(for destination: ExplorerDestination) -> Int {
        explorerDrops(for: destination).count
    }

    private func canAccess(destination: ExplorerDestination) -> Bool {
        guard destination.requiresAuthentication else { return true }
        guard let mode = userMode else { return false }
        return mode.canParticipate
    }
    
    // MARK: - Onboarding & Mode Selection

    func retryLegalManifest() {
        Task { await refreshLegalManifest() }
    }

    var isDropCreationEnabled: Bool {
        featureFlags.creationEnabled
    }

    func acceptTerms() {
        guard let manifest = legalManifest else {
            legalManifestError = "Kithe's approved legal policies are unavailable. Try again later."
            hasAcceptedTerms = false
            return
        }

        isRecordingLegalAcceptance = true
        legalManifestError = nil
        Task {
            do {
                if let userID = authService.currentUser?.uid {
                    try await legalConsentService.recordAcceptance(policyVersion: manifest.version)
                    defaults.set(
                        manifest.version,
                        forKey: DefaultsKeys.serverAcceptedLegalVersionPrefix + userID
                    )
                }
                defaults.set(manifest.version, forKey: DefaultsKeys.acceptedLegalVersion)
                hasAcceptedTerms = true
            } catch {
                hasAcceptedTerms = false
                legalManifestError = error.localizedDescription
            }
            isRecordingLegalAcceptance = false
        }
    }

    func completeOnboarding() {
        hasCompletedOnboarding = true
        defaults.set(true, forKey: DefaultsKeys.completedOnboarding)
    }

    func selectGuestMode() {
        hideTransientOverlays()
        pendingAccountRole = nil
        resetAuthFlowMessages()
        setUserMode(.guest)
    }

    func beginAuthentication(for role: UserRole) {
        hideTransientOverlays()
        pendingAccountRole = role
        resetAuthFlowMessages()
    }

    func cancelAuthenticationFlow() {
        pendingAccountRole = nil
        isAuthenticating = false
        isGoogleSigningIn = false
        resetAuthFlowMessages()
    }

    func resetAuthFlowMessages() {
        authFlowError = nil
        authFlowStatus = nil
    }

    deinit {
        groupListener?.remove()
        dropsListener?.remove()
        authService.stopObserving()
    }

    // MARK: - Auth

    /// Task 4.6 — say what happened to the guest's activity, but only when there
    /// is something to say. Linking is silent because the uid never changed; a
    /// failed merge is not, because the sign-in looks successful and the drops
    /// they made as a guest are exactly what they would notice missing.
    private static func signInStatus(
        _ base: String,
        _ outcome: AuthService.GuestContentOutcome
    ) -> String {
        switch outcome {
        case .notApplicable, .linked:
            return base
        case .merged:
            return "\(base) Your guest drops and collections moved to this account."
        case .mergeFailed:
            return "\(base) Your guest drops and collections couldn't be moved — " +
                "sign in again from the same device to retry."
        }
    }

    func signIn(email: String, password: String) {
        isAuthenticating = true
        resetAuthFlowMessages()
        Task {
            do {
                let outcome = try await authService.signIn(email: email, password: password)
                await MainActor.run {
                    self.authFlowStatus = AppViewModel.signInStatus(
                        "Signed in successfully.",
                        outcome
                    )
                    self.isAuthenticating = false
                }
            } catch {
                await MainActor.run {
                    self.authFlowError = error.localizedDescription
                    self.isAuthenticating = false
                }
            }
        }
    }

    func createAccount(email: String, password: String) {
        isAuthenticating = true
        resetAuthFlowMessages()
        Task {
            do {
                let outcome = try await authService.createAccount(email: email, password: password)
                await MainActor.run {
                    self.authFlowStatus = AppViewModel.signInStatus(
                        "Account created successfully.",
                        outcome
                    )
                    self.isAuthenticating = false
                }
            } catch {
                await MainActor.run {
                    self.authFlowError = error.localizedDescription
                    self.isAuthenticating = false
                }
            }
        }
    }

    func signOut() {
        do {
            try authService.signOut()
            hideTransientOverlays()
            setUserMode(nil)
            pendingAccountRole = nil
            isAuthenticating = false
            isGoogleSigningIn = false
            resetAuthFlowMessages()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func completeAccountDeletion(_ receipt: AccountDeletionReceipt) {
        lastAccountDeletionReceipt = receipt
        groupListener?.remove()
        groupListener = nil
        dropsListener?.remove()
        dropsListener = nil

        do {
            try authService.signOut()
        } catch {
            print("GeoDrop: Account was deleted, but Firebase local sign-out reported: \(error)")
        }

        groups = []
        drops = []
        selectedGroupCode = nil
        blockedCreatorIDs = []
        switchInventory(to: nil)
        authState = .signedOut
        hideTransientOverlays()
        setUserMode(nil)
        pendingAccountRole = nil
        isAuthenticating = false
        isGoogleSigningIn = false
        resetAuthFlowMessages()
    }

    func dismissAccountDeletionReceipt() {
        lastAccountDeletionReceipt = nil
    }
    
    func signInWithGoogle(presenting viewController: UIViewController) {
        isGoogleSigningIn = true
        resetAuthFlowMessages()
        Task {
            do {
                let outcome = try await authService.signInWithGoogle(presenting: viewController)
                await MainActor.run {
                    self.authFlowStatus = AppViewModel.signInStatus(
                        "Signed in with Google.",
                        outcome
                    )
                    self.isGoogleSigningIn = false
                    self.isAuthenticating = false
                }
            } catch {
                await MainActor.run {
                    self.authFlowError = error.localizedDescription
                    self.isGoogleSigningIn = false
                    self.isAuthenticating = false
                }
            }
        }
    }

    // MARK: - Drops

    func refreshDrops() async {
        guard case let .signedIn(session) = authState else { return }
        let groupCodes: Set<String>
        if let selected = selectedGroupCode {
            groupCodes = [selected]
        } else {
            groupCodes = Set(groups.map { $0.code })
        }
        dropsListener?.remove()
        dropsListener = firestore.listenForDrops(
            userId: session.user.uid,
            allowedGroups: groupCodes,
            restrictToGroups: selectedGroupCode != nil
        ) { [weak self] drops in
            DispatchQueue.main.async {
                guard let self = self else { return }
                let filtered = drops.filter { drop in
                    let creator = drop.createdBy.trimmingCharacters(in: .whitespacesAndNewlines)
                    let creatorAllowed = creator.isEmpty || !self.blockedCreatorIDs.contains(creator)
                    let couponAllowed = self.featureFlags.couponsEnabled ||
                        drop.dropType != .restaurantCoupon
                    let mediaAllowed = self.featureFlags.mediaEnabled ||
                        drop.contentType == .text
                    let nsfwAllowed = !drop.isNsfw
                    return creatorAllowed && couponAllowed && mediaAllowed && nsfwAllowed
                }
                self.inventoryService.merge(remoteDrops: filtered, for: self.inventoryUserId)
                self.reloadInventorySnapshot()
                self.drops = filtered
            }
        }
    }

    func createDrop(request: NewDropRequest) async {
        guard featureFlags.creationEnabled else {
            errorMessage = "Drop creation is disabled for this release."
            return
        }
        guard featureFlags.couponsEnabled || request.dropType != .restaurantCoupon else {
            errorMessage = "Offers are disabled for this release."
            return
        }
        guard featureFlags.mediaEnabled || request.media == nil else {
            errorMessage = "Media drops are disabled for this release."
            return
        }
        guard case let .signedIn(session) = authState else { return }
        guard let location = locationService.currentLocation else {
            errorMessage = "Current location unavailable"
            return
        }
        
        isPerformingAction = true
        defer { isPerformingAction = false }
        
        let groupCode: String?
        switch request.visibility {
        case .public:
            groupCode = nil
        case .group(let code):
            groupCode = code
        }

        if groupCode != nil && request.media != nil {
            errorMessage = "Private group drops are text-only during the market pilot."
            return
        }

        var drop = Drop(
            text: request.text,
            description: request.description,
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            createdBy: session.user.uid,
            createdAt: Date(),
            dropperUsername: session.profile.username,
            isAnonymous: request.isAnonymous,
            decayDays: request.decayDays,
            groupCode: groupCode,
            dropType: request.dropType,
            businessId: session.profile.role == .business ? session.user.uid : nil,
            businessName: session.profile.businessName,
            contentType: request.contentType,
            mediaData: nil,
            isNsfw: false,
            redemptionLimit: request.redemptionLimit
        )

        var safeSearchPayload: String?
        if let media = request.media {
            do {
                let upload = try await StorageService.shared.uploadMedia(
                    data: media.data,
                    mimeType: media.mimeType,
                    fileExtension: media.fileExtension,
                    userId: session.user.uid
                )
                drop.mediaURL = upload.url
                drop.mediaMimeType = media.mimeType
                drop.mediaStoragePath = upload.path
                let base64 = media.data.base64EncodedString()
                switch request.contentType {
                case .photo:
                    safeSearchPayload = "data:\(media.mimeType);base64,\(base64)"
                case .audio:
                    drop.mediaData = base64
                    safeSearchPayload = "data:\(media.mimeType);base64,\(base64)"
                case .video:
                    safeSearchPayload = nil
                case .text:
                    safeSearchPayload = nil
                }
            } catch {
                errorMessage = "Upload failed: \(error.localizedDescription)"
                return
            }
        }
        
        let textForSafety: String? = {
            let components = [request.text, request.description].compactMap { value -> String? in
                guard let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else {
                    return nil
                }
                return trimmed
            }
            return components.isEmpty ? nil : components.joined(separator: "\n")
        }()

        let assessment = await safeSearch.assess(
            text: textForSafety,
            contentType: drop.contentType,
            mediaMimeType: drop.mediaMimeType,
            mediaData: safeSearchPayload,
            mediaUrl: drop.mediaURL?.absoluteString
        )
        drop.isNsfw = assessment.isNsfw
        drop.nsfwLabels = assessment.reasons

        if assessment.isNsfw {
            if let path = drop.mediaStoragePath {
                await StorageService.shared.delete(path: path)
            }
            errorMessage = "Mature content cannot be published during the market pilot."
            return
        }

        do {
            _ = try await firestore.addDrop(drop)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func delete(drop: Drop) {
        Task {
            do {
                try await firestore.deleteDrop(dropId: drop.id)
                if let path = drop.mediaStoragePath {
                    await StorageService.shared.delete(path: path)
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func like(drop: Drop, status: DropLikeStatus) {
        guard case let .signedIn(session) = authState else { return }
        let permission = likePermission(for: drop)
        guard permission.allowed else { return }
        let userId = session.user.uid
        Task {
            do {
                try await firestore.setDropLike(dropId: drop.id, userId: userId, status: status)
                await MainActor.run {
                    self.inventoryService.setLikeStatus(status, dropId: drop.id, drop: drop, for: userId)
                    self.reloadInventorySnapshot()
                    self.mutateDrop(withId: drop.id) { value in
                        let previousStatus = value.isLiked(by: userId)
                        guard previousStatus != status else { return }

                        switch previousStatus {
                        case .liked:
                            value.likeCount = max(value.likeCount - 1, 0)
                            value.likedBy.removeValue(forKey: userId)
                        case .none:
                            break
                        }

                        switch status {
                        case .liked:
                            value.likeCount += 1
                            value.likedBy[userId] = true
                        case .none:
                            value.likedBy.removeValue(forKey: userId)
                        }
                    }
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    /// The unlock attempt (task 3.3, direction doc steps 2–5): request precise location
    /// now, answer the proximity question, then let the fix go. Fail-closed throughout,
    /// matching Android's DropDecisionReceiver.
    private func preciseFixForUnlock() async -> CLLocation? {
        await withCheckedContinuation { continuation in
            locationService.requestPreciseFix { continuation.resume(returning: $0) }
        }
    }

    @discardableResult
    func markCollected(drop: Drop) async -> DropActionError? {
        guard case let .signedIn(session) = authState else { return .notSignedIn }

        let radius = Self.dropPreviewRadiusMeters
        unlockingDropID = drop.id
        let fix = await preciseFixForUnlock()
        unlockingDropID = nil

        guard let location = fix else {
            return .invalidInput("Can't confirm your location yet. Try again in a moment.")
        }
        guard Date().timeIntervalSince(location.timestamp) <= Self.locationStaleThresholdSeconds else {
            return .invalidInput("Your location reading is out of date. Try again in a moment.")
        }
        // A negative horizontalAccuracy means the fix is invalid. A large one means the
        // user declined temporary full accuracy, where a 30 m check cannot mean anything.
        guard location.horizontalAccuracy >= 0, location.horizontalAccuracy <= radius else {
            return .invalidInput(
                "Your location isn't precise enough to pick this up. Allow precise location and try again."
            )
        }
        let dropLocation = CLLocation(latitude: drop.latitude, longitude: drop.longitude)
        guard location.distance(from: dropLocation) <= radius + location.horizontalAccuracy else {
            return .invalidInput("Move within \(Int(radius.rounded())) meters to pick up this drop.")
        }

        unlockedDropIDs.insert(drop.id)

        Task {
            do {
                try await firestore.markDropCollected(dropId: drop.id, userId: session.user.uid)
                await MainActor.run {
                    var storedDrop = drop
                    storedDrop.collectedBy[session.user.uid] = true
                    self.inventoryService.storeCollected(drop: storedDrop, for: session.user.uid)
                    self.reloadInventorySnapshot()
                    self.mutateDrop(withId: drop.id) { value in
                        value.collectedBy[session.user.uid] = true
                    }
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
        
        return nil
    }
    
    func setIgnored(drop: Drop, isIgnored: Bool) {
        inventoryService.setIgnored(dropId: drop.id, isIgnored: isIgnored, for: inventoryUserId)
        reloadInventorySnapshot()
    }

    func report(drop: Drop, reasonCodes: Set<String>, additionalContext: [String: Any] = [:]) async -> Result<Void, Error> {
        guard let userId = currentUserID else {
            return .failure(DropActionError.notSignedIn)
        }
        let sanitizedReasons = reasonCodes
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        guard !sanitizedReasons.isEmpty else {
            return .failure(DropActionError.invalidInput("Select at least one reason."))
        }

        do {
            try await firestore.submitReport(
                dropId: drop.id,
                reporterId: userId,
                reasonCodes: sanitizedReasons,
                context: additionalContext
            )
            mutateDrop(withId: drop.id) { value in
                let already = value.reportedBy[userId] != nil
                value.reportedBy[userId] = Int(Date().timeIntervalSince1970 * 1000)
                if !already {
                    value.reportCount += 1
                }
            }
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func redeem(drop: Drop) async -> RedemptionResult {
        guard featureFlags.couponsEnabled else {
            return .error("Offers are disabled for this release.")
        }
        guard let userId = currentUserID else {
            return .error("Sign in to redeem offers.")
        }
        do {
            let result = try await firestore.redeemDrop(dropId: drop.id, userId: userId)
            if case let .success(count, limit, redeemedAt, issuedCode) = result {
                // redeemedAt is integer milliseconds now; Date wants seconds.
                let redemptionDate = Date(timeIntervalSince1970: Double(redeemedAt) / 1000)
                var updatedDrop = drop
                updatedDrop.redemptionCount = count
                updatedDrop.redemptionLimit = limit
                updatedDrop.redeemedBy[userId] = redeemedAt
                inventoryService.setRedeemed(
                    dropId: drop.id,
                    count: count,
                    limit: limit,
                    code: issuedCode,
                    redeemedAt: redemptionDate,
                    drop: updatedDrop,
                    for: userId
                )
                reloadInventorySnapshot()
                mutateDrop(withId: drop.id) { value in
                    value.redemptionCount = count
                    value.redemptionLimit = limit
                    value.redeemedBy[userId] = redeemedAt
                }
            }
            return result
        } catch {
            return .error(error.localizedDescription)
        }
    }

    func blockCreator(of drop: Drop) async -> Result<Void, Error> {
        guard let userId = currentUserID else {
            return .failure(DropActionError.notSignedIn)
        }
        let creatorId = drop.createdBy.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !creatorId.isEmpty else {
            return .failure(DropActionError.missingCreator)
        }
        if creatorId == userId {
            return .failure(DropActionError.invalidInput("You can't block your own drops."))
        }

        do {
            try await firestore.blockDropCreator(userId: userId, creatorId: creatorId)
            blockedCreatorIDs.insert(creatorId)
            drops.removeAll { current in
                current.createdBy.trimmingCharacters(in: .whitespacesAndNewlines) == creatorId
            }
            return .success(())
        } catch {
            return .failure(error)
        }
    }

    // MARK: - Drop Helpers

    func hasCollected(drop: Drop) -> Bool {
        if inventory.collectedDrops[drop.id] != nil { return true }
        guard let userId = currentUserID else { return false }
        if drop.collectedBy[userId] == true {
            var storedDrop = drop
            storedDrop.collectedBy[userId] = true
            inventoryService.storeCollected(drop: storedDrop, for: userId)
            reloadInventorySnapshot()
            return true
        }
        return false
    }

    func shouldHideContent(for drop: Drop) -> Bool {
        return drop.isNsfw
    }

    func likePermission(for drop: Drop) -> LikePermission {
        guard let userId = currentUserID else {
            return LikePermission(allowed: false, message: "Sign in to react to drops.")
        }
        if let mode = userMode, !mode.canParticipate {
            return LikePermission(allowed: false, message: "Upgrade to a full account to react to drops.")
        }
        if drop.createdBy == userId {
            return LikePermission(allowed: false, message: "You can't react to your own drop.")
        }
        if shouldHideContent(for: drop) {
            return LikePermission(allowed: false, message: "Enable adult content in Profile to interact with this drop.")
        }
        guard hasCollected(drop: drop) else {
            return LikePermission(allowed: false, message: "Collect this drop to react to it.")
        }
        return LikePermission(allowed: true, message: nil)
    }

    func isOwner(of drop: Drop) -> Bool {
        guard let userId = currentUserID else { return false }
        return drop.createdBy == userId
    }

    private func mutateDrop(withId id: Drop.ID, apply: (inout Drop) -> Void) {
        guard let index = drops.firstIndex(where: { $0.id == id }) else { return }
        var updated = drops[index]
        apply(&updated)
        drops[index] = updated
    }
    
    private func matchesSelectedGroup(_ drop: Drop) -> Bool {
        guard let selected = selectedGroupCode?.trimmingCharacters(in: .whitespacesAndNewlines), !selected.isEmpty else {
            return true
        }
        guard let group = drop.groupCode?.trimmingCharacters(in: .whitespacesAndNewlines), !group.isEmpty else {
            return false
        }
        return group.caseInsensitiveCompare(selected) == .orderedSame
    }
    
    private func reloadInventorySnapshot() {
        inventory = inventoryService.inventory(for: inventoryUserId)
        rebuildExplorerCollections()
    }

    private func switchInventory(to userId: String?) {
        inventoryUserId = userId
        reloadInventorySnapshot()
    }
    
    private func rebuildExplorerCollections() {
        let ignored = inventory.ignoredDropIDs
        let visibleDrops = drops.filter { matchesSelectedGroup($0) }
        if let userId = currentUserID {
            explorerMyDrops = visibleDrops.filter { $0.createdBy == userId && !ignored.contains($0.id) }
        } else {
            explorerMyDrops = []
        }

        var collected: [Drop] = []
        var seen: Set<String> = []
        let stored = inventory.collectedDrops
        if let userId = currentUserID {
            for drop in visibleDrops {
                if ignored.contains(drop.id) { continue }
                if drop.collectedBy[userId] == true {
                    collected.append(drop)
                    seen.insert(drop.id)
                }
            }
        }

        for (dropId, storedDrop) in stored.sorted(by: { $0.value.displayTitle < $1.value.displayTitle }) {
            guard !ignored.contains(dropId) else { continue }
            if seen.contains(dropId) { continue }
            if !matchesSelectedGroup(storedDrop) { continue }
            if storedDrop.isNsfw { continue }
            collected.append(storedDrop)
        }

        explorerCollectedDrops = collected
    }
    
    // MARK: - Groups

    func joinGroup(code: String, allowCreate: Bool) {
        guard case let .signedIn(session) = authState else { return }
        Task {
            do {
                let membership = try await firestore.joinGroup(userId: session.user.uid, code: code, allowCreate: allowCreate)
                await MainActor.run {
                    if !self.groups.contains(where: { $0.code == membership.code }) {
                        self.groups.append(membership)
                    }
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func leaveGroup(code: String) {
        guard case let .signedIn(session) = authState else { return }
        Task {
            do {
                try await firestore.leaveGroup(userId: session.user.uid, code: code)
                await MainActor.run {
                    self.groups.removeAll { $0.code == code }
                    if self.selectedGroupCode == code {
                        self.selectedGroupCode = nil
                    }
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    // MARK: - Profile

    func updateProfile(_ update: @escaping (UserProfile) async throws -> UserProfile) {
        guard case var .signedIn(session) = authState else { return }
        Task {
            do {
                let updated = try await update(session.profile)
                session.profile = updated
                await MainActor.run {
                    self.authState = .signedIn(session)
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func updateExplorerUsername(to desired: String) {
        guard case var .signedIn(session) = authState else { return }
        Task {
            do {
                let updated = try await firestore.updateExplorerUsername(userId: session.user.uid, desired: desired)
                session.profile = updated
                await MainActor.run {
                    self.authState = .signedIn(session)
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func updateBusinessProfile(name: String, categories: [BusinessCategory]) {
        guard case var .signedIn(session) = authState else { return }
        Task {
            do {
                let updated = try await firestore.updateBusinessProfile(userId: session.user.uid, name: name, categories: categories)
                session.profile = updated
                await MainActor.run {
                    self.authState = .signedIn(session)
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    func fetchBusinessDrops() async throws -> [Drop] {
        guard case let .signedIn(session) = authState else { return [] }
        guard session.profile.role == .business else { return [] }
        return try await firestore.getBusinessDrops(businessId: session.user.uid)
    }

    func fetchOwnedExperienceAnalytics() async throws -> [ExperienceAnalytics] {
        guard case let .signedIn(session) = authState else { return [] }
        guard session.profile.role == .business else { return [] }
        return try await firestore.getOwnedExperienceAnalytics(userId: session.user.uid)
    }
    
    // MARK: - Private

    private func refreshLegalManifest() async {
        isLoadingLegalManifest = true
        legalManifestError = nil
        hasAcceptedTerms = false
        do {
            let manifest = try await legalConsentService.fetchManifest()
            legalManifest = manifest
            await reconcileLegalAcceptance(for: authService.currentUser?.uid)
        } catch {
            legalManifest = nil
            hasAcceptedTerms = false
            legalManifestError = error.localizedDescription
        }
        isLoadingLegalManifest = false
    }

    private func reconcileLegalAcceptance(for userID: String?) async {
        guard let manifest = legalManifest else {
            hasAcceptedTerms = false
            return
        }
        guard defaults.string(forKey: DefaultsKeys.acceptedLegalVersion) == manifest.version else {
            hasAcceptedTerms = false
            return
        }
        guard let userID else {
            hasAcceptedTerms = true
            return
        }

        let serverKey = DefaultsKeys.serverAcceptedLegalVersionPrefix + userID
        if defaults.string(forKey: serverKey) == manifest.version {
            hasAcceptedTerms = true
            return
        }

        do {
            try await legalConsentService.recordAcceptance(policyVersion: manifest.version)
            defaults.set(manifest.version, forKey: serverKey)
            hasAcceptedTerms = true
        } catch {
            // Signed-in participation fails closed until the callable stores the
            // current acceptance with a server timestamp.
            hasAcceptedTerms = false
            legalManifestError = error.localizedDescription
        }
    }

    private func handleAuthChange(user: FirebaseAuth.User?) async {
        groupListener?.remove()
        dropsListener?.remove()
        if let user = user {
            if let manifest = legalManifest {
                let serverKey = DefaultsKeys.serverAcceptedLegalVersionPrefix + user.uid
                if defaults.string(forKey: serverKey) != manifest.version {
                    hasAcceptedTerms = false
                }
            } else {
                hasAcceptedTerms = false
            }
            authState = .loading
            await loadSession(user: user)
            await reconcileLegalAcceptance(for: user.uid)
        } else {
            groups = []
            drops = []
            selectedGroupCode = nil
            blockedCreatorIDs = []
            switchInventory(to: nil)
            authState = .signedOut
            isAuthenticating = false
            isGoogleSigningIn = false
            hideTransientOverlays()
            let persistedMode = persistedUserMode()
            if persistedMode == .guest {
                setUserMode(.guest, persist: false)
            } else {
                setUserMode(nil)
            }
            await reconcileLegalAcceptance(for: nil)
        }
    }

    private func loadSession(user: FirebaseAuth.User) async {
        do {
            let profile = try await firestore.ensureUserProfile(userId: user.uid, displayName: user.displayName)
            let memberships = try await firestore.fetchUserGroupMemberships(userId: user.uid)
            let blocked = try await firestore.fetchBlockedCreators(userId: user.uid)
            let session = UserSession(
                user: AuthenticatedUser(uid: user.uid, email: user.email, displayName: user.displayName),
                profile: profile
            )
            groups = memberships
            selectedGroupCode = nil
            blockedCreatorIDs = blocked
            switchInventory(to: session.user.uid)
            authState = .signedIn(session)
            pendingAccountRole = nil
            resetAuthFlowMessages()
            setUserMode(.signedIn)

            groupListener?.remove()
            groupListener = firestore.listenForGroupMemberships(userId: user.uid) { [weak self] memberships in
                DispatchQueue.main.async {
                    self?.groups = memberships
                    if let selected = self?.selectedGroupCode,
                       !memberships.contains(where: { $0.code == selected }) {
                        self?.selectedGroupCode = nil
                    }
                    Task { await self?.refreshDrops() }
                }
            }
            await refreshDrops()
        } catch {
            errorMessage = error.localizedDescription
            authState = .signedOut
        }
    }

    private func syncMessagingToken(_ token: String?) async {
        guard let token = token, case let .signedIn(session) = authState else { return }
        await firestore.registerMessagingToken(userId: session.user.uid, token: token, platform: "ios")
    }
    
    private func persistedUserMode() -> UserMode? {
        guard let rawValue = defaults.string(forKey: DefaultsKeys.userMode) else { return nil }
        return UserMode(rawValue: rawValue)
    }

    private func setUserMode(_ mode: UserMode?, persist: Bool = true) {
        userMode = mode
        if !canAccess(destination: selectedExplorerDestination) {
            selectedExplorerDestination = .nearby
        }
        if mode?.canParticipate == true {
            explorerRestrictionMessage = nil
        } else {
            hideParticipationDependentOverlays()
        }
        guard persist else { return }
        if let mode {
            defaults.set(mode.rawValue, forKey: DefaultsKeys.userMode)
        } else {
            defaults.removeObject(forKey: DefaultsKeys.userMode)
        }
    }
    
    // MARK: - Navigation & Preferences

    func toggleAccountMenu() {
        if isShowingAccountMenu {
            isShowingAccountMenu = false
        } else {
            isShowingAccountMenu = true
            isShowingGroupMenu = false
        }
    }

    func openAccountData() {
        guard case .signedIn = authState else {
            errorMessage = "Sign in to manage account data."
            return
        }
        isShowingAccountMenu = false
        isShowingGroupMenu = false
        isShowingAccountData = true
    }

    func dismissAccountData() {
        isShowingAccountData = false
    }

    func presentDropComposer() {
        guard featureFlags.creationEnabled else {
            errorMessage = "Drop creation is disabled for this release."
            return
        }
        guard canParticipate else {
            errorMessage = participationRestrictionMessage(action: "share drops")
            return
        }

        guard case .signedIn = authState else {
            beginAuthentication(for: .explorer)
            return
        }

        isShowingDropComposer = true
        isShowingAccountMenu = false
        isShowingGroupMenu = false
    }

    func dismissDropComposer() {
        isShowingDropComposer = false
    }

    func toggleGroupMenu() {
        guard canParticipate else {
            errorMessage = participationRestrictionMessage(action: "manage groups")
            return
        }

        if isShowingGroupMenu {
            isShowingGroupMenu = false
        } else {
            isShowingGroupMenu = true
            isShowingAccountMenu = false
        }
    }

    func openGroupManagement() {
        guard canParticipate else {
            errorMessage = participationRestrictionMessage(action: "manage groups")
            return
        }

        isShowingGroupMenu = false
        isShowingGroupManagement = true
    }

    func dismissGroupManagement() {
        isShowingGroupManagement = false
    }

    func setNearbyAlertsEnabled(_ enabled: Bool) {
        let allowedValue = featureFlags.notificationsEnabled && enabled
        notificationPreferences.setNearbyAlertsEnabled(allowedValue)
        nearbyAlertsEnabled = allowedValue
    }
    
    func showTutorialSlides() {
        hideTransientOverlays()
        isShowingTutorialSlides = true
    }

    func dismissTutorialSlides() {
        isShowingTutorialSlides = false
    }

    func showFaq() {
        hideTransientOverlays()
        isShowingFaq = true
    }

    func dismissFaq() {
        isShowingFaq = false
    }

    func showTermsOfService() {
        hideTransientOverlays()
        guard let url = legalManifest?.terms else {
            errorMessage = "Approved Terms are currently unavailable."
            return
        }
        infoMenuURL = url
    }

    func showPrivacyPolicy() {
        hideTransientOverlays()
        guard let url = legalManifest?.privacy else {
            errorMessage = "The approved Privacy Policy is currently unavailable."
            return
        }
        infoMenuURL = url
    }

    func dismissInfoMenuLink() {
        infoMenuURL = nil
    }

    func participationRestrictionMessage(action: String) -> String {
        switch userMode {
        case .guest:
            return "Create an account to \(action)."
        case .signedIn:
            return "Sign in to \(action)."
        case .none:
            return "Sign in to \(action)."
        }
    }

    private var canParticipate: Bool {
        userMode?.canParticipate == true
    }

    private func hideTransientOverlays() {
        isShowingAccountMenu = false
        isShowingAccountData = false
        isShowingDropComposer = false
        isShowingGroupMenu = false
        isShowingGroupManagement = false
    }

    private func hideParticipationDependentOverlays() {
        if !canParticipate {
            isShowingDropComposer = false
            isShowingGroupMenu = false
            isShowingGroupManagement = false
            }
    }
}

struct NewDropRequest {
    struct MediaPayload {
        let data: Data
        let mimeType: String
        let fileExtension: String
    }
    
    enum Visibility {
        case `public`
        case group(String)
    }

    var text: String
    var description: String?
    var isAnonymous: Bool
    var dropType: DropType
    var contentType: DropContentType
    var media: MediaPayload?
    var redemptionLimit: Int?
    var decayDays: Int?
    var visibility: Visibility
}
