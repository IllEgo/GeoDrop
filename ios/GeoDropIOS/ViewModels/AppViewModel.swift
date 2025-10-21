import Foundation
import Combine
import FirebaseAuth
import FirebaseFirestore

@MainActor
final class AppViewModel: ObservableObject {
    enum AuthState {
        case loading
        case signedOut
        case signedIn(UserSession)
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

    @Published private(set) var authState: AuthState = .loading
    @Published var groups: [GroupMembership] = []
    @Published var selectedGroupCode: String?
    @Published var drops: [Drop] = []
    @Published var allowNsfw: Bool = false
    @Published var errorMessage: String?
    @Published var isPerformingAction: Bool = false

    private let authService = AuthService.shared
    private let firestore = FirestoreService.shared
    private let messagingService = MessagingService.shared
    private let locationService = LocationService.shared
    private lazy var safeSearch = SafeSearchService(apiKey: AppConfiguration.shared.visionApiKey)
    private var groupListener: ListenerRegistration?
    private var dropsListener: ListenerRegistration?
    private var cancellables: Set<AnyCancellable> = []

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

    deinit {
        groupListener?.remove()
        dropsListener?.remove()
        authService.stopObserving()
    }

    // MARK: - Auth

    func signIn(email: String, password: String) {
        Task {
            do {
                try await authService.signIn(email: email, password: password)
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func createAccount(email: String, password: String) {
        Task {
            do {
                try await authService.createAccount(email: email, password: password)
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func signOut() {
        do {
            try authService.signOut()
        } catch {
            errorMessage = error.localizedDescription
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
                self?.drops = drops
            }
        }
    }

    func createDrop(request: NewDropRequest) async {
        guard case let .signedIn(session) = authState else { return }
        guard let location = locationService.currentLocation else {
            errorMessage = "Current location unavailable"
            return
        }
        guard let groupCode = selectedGroupCode else {
            errorMessage = "Join a group before dropping."
            return
        }
        isPerformingAction = true
        defer { isPerformingAction = false }

        var drop = Drop(
            text: request.text,
            description: request.description,
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            createdBy: session.user.uid,
            createdAt: Date(),
            dropperUsername: session.profile.username,
            isAnonymous: request.isAnonymous,
            groupCode: groupCode,
            dropType: request.dropType,
            businessId: session.profile.role == .business ? session.user.uid : nil,
            businessName: session.profile.businessName,
            contentType: request.contentType,
            isNsfw: false
        )

        var mediaDataString: String?
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
                mediaDataString = "data:\(media.mimeType);base64,\(base64)"
            } catch {
                errorMessage = "Upload failed: \(error.localizedDescription)"
                return
            }
        }

        let assessment = await safeSearch.assess(
            contentType: drop.contentType,
            mediaMimeType: drop.mediaMimeType,
            mediaData: mediaDataString,
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
        Task {
            do {
                try await firestore.setDropLike(dropId: drop.id, userId: session.user.uid, status: status)
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
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func report(drop: Drop, reasons: [String], context: [String: Any] = [:]) {
        guard case let .signedIn(session) = authState else { return }
        Task {
            do {
                try await firestore.submitReport(dropId: drop.id, reporterId: session.user.uid, reasonCodes: reasons, context: context)
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
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
            authState = .signedOut
        }
    }

    private func loadSession(user: FirebaseAuth.User) async {
        do {
            let profile = try await firestore.ensureUserProfile(userId: user.uid, displayName: user.displayName)
            let memberships = try await firestore.fetchUserGroupMemberships(userId: user.uid)
            let session = UserSession(
                user: AuthenticatedUser(uid: user.uid, email: user.email, displayName: user.displayName),
                profile: profile
            )
            allowNsfw = profile.nsfwEnabled
            groups = memberships
            selectedGroupCode = memberships.first?.code
            authState = .signedIn(session)

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
}

struct NewDropRequest {
    struct MediaPayload {
        let data: Data
        let mimeType: String
        let fileExtension: String
    }

    var text: String
    var description: String?
    var isAnonymous: Bool
    var dropType: DropType
    var contentType: DropContentType
    var media: MediaPayload?
}