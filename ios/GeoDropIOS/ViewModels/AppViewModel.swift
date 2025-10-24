import Foundation
import Combine
import FirebaseAuth
import FirebaseFirestore
import UIKit

@MainActor
final class AppViewModel: ObservableObject {
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
    @Published var selectedGroupCode: String?
    @Published var drops: [Drop] = []
    @Published private(set) var blockedCreatorIDs: Set<String> = []
    @Published private(set) var inventory: NoteInventoryService.Inventory
    @Published var allowNsfw: Bool = false
    @Published var errorMessage: String?
    @Published var isPerformingAction: Bool = false
    @Published var pendingAccountRole: UserRole?
    @Published var isAuthenticating: Bool = false
    @Published var isGoogleSigningIn: Bool = false
    @Published var authFlowError: String?
    @Published var authFlowStatus: String?

    private let authService = AuthService.shared
    private let firestore = FirestoreService.shared
    private let messagingService = MessagingService.shared
    private let locationService = LocationService.shared
    private lazy var safeSearch = SafeSearchService(apiKey: AppConfiguration.shared.visionApiKey)
    private let inventoryService = NoteInventoryService.shared
    private var groupListener: ListenerRegistration?
    private var dropsListener: ListenerRegistration?
    private var cancellables: Set<AnyCancellable> = []
    private let defaults: UserDefaults
    private var inventoryUserId: String?

    private enum DefaultsKeys {
        static let acceptedTerms = "geodrop.termsAccepted"
        static let completedOnboarding = "geodrop.onboardingCompleted"
        static let userMode = "geodrop.userMode"
    }

    init(userDefaults: UserDefaults = .standard) {
        self.defaults = userDefaults
        let initialUserId = authService.currentUser?.uid
        self.inventoryUserId = initialUserId
        self.inventory = inventoryService.inventory(for: initialUserId)
        let accepted = userDefaults.bool(forKey: DefaultsKeys.acceptedTerms)
        self.hasAcceptedTerms = accepted
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
                }
            }
            .store(in: &cancellables)
    }
    
    var currentUserID: String? {
        if case let .signedIn(session) = authState {
            return session.user.uid
        }
        return nil
    }

    func bootstrap() {
        messagingService.requestAuthorization()
        locationService.requestAuthorization()
        locationService.startUpdating()

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
    
    // MARK: - Onboarding & Mode Selection

    func acceptTerms() {
        hasAcceptedTerms = true
        defaults.set(true, forKey: DefaultsKeys.acceptedTerms)
    }

    func completeOnboarding() {
        hasCompletedOnboarding = true
        defaults.set(true, forKey: DefaultsKeys.completedOnboarding)
    }

    func selectGuestMode() {
        pendingAccountRole = nil
        resetAuthFlowMessages()
        setUserMode(.guest)
    }

    func beginAuthentication(for role: UserRole) {
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

    func signIn(email: String, password: String) {
        isAuthenticating = true
        resetAuthFlowMessages()
        Task {
            do {
                try await authService.signIn(email: email, password: password)
                await MainActor.run {
                    self.authFlowStatus = "Signed in successfully."
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
                try await authService.createAccount(email: email, password: password)
                await MainActor.run {
                    self.authFlowStatus = "Account created successfully."
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
            setUserMode(nil)
            pendingAccountRole = nil
            isAuthenticating = false
            isGoogleSigningIn = false
            resetAuthFlowMessages()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func signInWithGoogle(presenting viewController: UIViewController) {
        isGoogleSigningIn = true
        resetAuthFlowMessages()
        Task {
            do {
                try await authService.signInWithGoogle(presenting: viewController)
                await MainActor.run {
                    self.authFlowStatus = "Signed in with Google."
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
            allowNsfw: allowNsfw
        ) { [weak self] drops in
            DispatchQueue.main.async {
                guard let self = self else { return }
                let filtered = drops.filter { drop in
                    let creator = drop.createdBy.trimmingCharacters(in: .whitespacesAndNewlines)
                    return creator.isEmpty || !self.blockedCreatorIDs.contains(creator)
                }
                self.inventoryService.merge(remoteDrops: filtered, for: self.inventoryUserId)
                self.reloadInventorySnapshot()
                self.drops = filtered
            }
        }
    }

    func createDrop(request: NewDropRequest) async {
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
            groupCode = "PUBLIC"
        case .group(let code):
            groupCode = code
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
            redemptionCode: request.redemptionCode,
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
                        let wasLiked = value.isLiked(by: userId) == .liked
                        if status == .liked && !wasLiked {
                            value.likeCount += 1
                        } else if status == .none && wasLiked {
                            value.likeCount = max(value.likeCount - 1, 0)
                        }
                        if status == .liked {
                            value.likedBy[userId] = true
                        } else {
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

    func markCollected(drop: Drop) {
        guard case let .signedIn(session) = authState else { return }
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
                value.reportedBy[userId] = Date().timeIntervalSince1970
                if !already {
                    value.reportCount += 1
                }
            }
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func redeem(drop: Drop, code: String) async -> RedemptionResult {
        guard let userId = currentUserID else {
            return .error("Sign in to redeem offers.")
        }
        let trimmed = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return .invalidCode }
        do {
            let result = try await firestore.redeemDrop(dropId: drop.id, userId: userId, providedCode: trimmed)
            if case let .success(count, limit, redeemedAt) = result {
                let redemptionDate = Date(timeIntervalSince1970: redeemedAt)
                var updatedDrop = drop
                updatedDrop.redemptionCount = count
                updatedDrop.redemptionLimit = limit
                updatedDrop.redeemedBy[userId] = redeemedAt
                inventoryService.setRedeemed(
                    dropId: drop.id,
                    count: count,
                    limit: limit,
                    code: trimmed,
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
        guard drop.isNsfw else { return false }
        if let ownerId = currentUserID, ownerId == drop.createdBy {
            return false
        }
        return !allowNsfw
    }

    func likePermission(for drop: Drop) -> LikePermission {
        guard let userId = currentUserID else {
            return LikePermission(allowed: false, message: "Sign in to like drops.")
        }
        if let mode = userMode, !mode.canParticipate {
            return LikePermission(allowed: false, message: "Upgrade to a full account to like drops.")
        }
        if drop.createdBy == userId {
            return LikePermission(allowed: false, message: "You can't like your own drop.")
        }
        if shouldHideContent(for: drop) {
            return LikePermission(allowed: false, message: "Enable adult content in Profile to interact with this drop.")
        }
        guard hasCollected(drop: drop) else {
            return LikePermission(allowed: false, message: "Collect this drop to like it.")
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
    
    private func reloadInventorySnapshot() {
        inventory = inventoryService.inventory(for: inventoryUserId)
    }

    private func switchInventory(to userId: String?) {
        inventoryUserId = userId
        reloadInventorySnapshot()
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
                    self.selectedGroupCode = membership.code
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
                        self.selectedGroupCode = self.groups.first?.code
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

    func setAllowNsfw(_ enabled: Bool) {
        guard case var .signedIn(session) = authState else { return }
        Task {
            do {
                let updated = try await firestore.updateNsfwPreference(userId: session.user.uid, enabled: enabled)
                session.profile = updated
                await MainActor.run {
                    self.authState = .signedIn(session)
                    self.allowNsfw = enabled
                    Task { await self.refreshDrops() }
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
    
    // MARK: - Private

    private func handleAuthChange(user: FirebaseAuth.User?) async {
        groupListener?.remove()
        dropsListener?.remove()
        if let user = user {
            authState = .loading
            await loadSession(user: user)
        } else {
            groups = []
            drops = []
            selectedGroupCode = nil
            blockedCreatorIDs = []
            switchInventory(to: nil)
            authState = .signedOut
            isAuthenticating = false
            isGoogleSigningIn = false
            let persistedMode = persistedUserMode()
            if persistedMode == .guest {
                setUserMode(.guest, persist: false)
            } else {
                setUserMode(nil)
            }
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
            allowNsfw = profile.nsfwEnabled
            groups = memberships
            selectedGroupCode = memberships.first?.code
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
                    if self?.selectedGroupCode == nil {
                        self?.selectedGroupCode = memberships.first?.code
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
        guard persist else { return }
        if let mode {
            defaults.set(mode.rawValue, forKey: DefaultsKeys.userMode)
        } else {
            defaults.removeObject(forKey: DefaultsKeys.userMode)
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
    var redemptionCode: String?
    var redemptionLimit: Int?
    var decayDays: Int?
    var visibility: Visibility
}
