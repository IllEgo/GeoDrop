import Foundation
import FirebaseFirestore
import FirebaseFunctions

// ListenerRegistration is an @objc protocol, so a conforming type must inherit
// NSObject rather than declaring NSObjectProtocol conformance in Swift.
private final class CombinedListenerRegistration: NSObject, ListenerRegistration {
    private let registrations: [ListenerRegistration]

    init(_ registrations: [ListenerRegistration]) {
        self.registrations = registrations
        super.init()
    }

    func remove() {
        registrations.forEach { $0.remove() }
    }
}

final class FirestoreService {
    static let shared = FirestoreService()

    private let db = Firestore.firestore()
    private lazy var drops = db.collection("drops")
    private lazy var users = db.collection("users")
    private lazy var usernames = db.collection("usernames")
    private lazy var reports = db.collection("reports")
    private lazy var functions = Functions.functions()

    private init() {}

    // MARK: - Helpers (Continuations typed explicitly)

    private func getDocuments(_ query: Query) async throws -> QuerySnapshot {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<QuerySnapshot, Error>) in
            query.getDocuments { snapshot, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let snapshot = snapshot {
                    continuation.resume(returning: snapshot)
                } else {
                    continuation.resume(throwing: FirestoreError.missingSnapshot)
                }
            }
        }
    }

    private func getDocument(_ ref: DocumentReference) async throws -> DocumentSnapshot {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<DocumentSnapshot, Error>) in
            ref.getDocument { snapshot, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let snapshot = snapshot {
                    continuation.resume(returning: snapshot)
                } else {
                    continuation.resume(throwing: FirestoreError.missingSnapshot)
                }
            }
        }
    }

    private func setDocument(_ ref: DocumentReference, data: [String: Any], merge: Bool = true) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            ref.setData(data, merge: merge) { error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    private func deleteDocument(_ ref: DocumentReference) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            ref.delete { error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    private func addDocument(_ collection: CollectionReference, data: [String: Any]) async throws -> DocumentReference {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<DocumentReference, Error>) in
            var reference: DocumentReference?
            reference = collection.addDocument(data: data) { error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let reference = reference {
                    continuation.resume(returning: reference)
                } else {
                    continuation.resume(throwing: FirestoreError.missingSnapshot)
                }
            }
        }
    }

    private func callFunction(name: String, data: [String: Any]) async throws -> HTTPSCallableResult {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<HTTPSCallableResult, Error>) in
            functions.httpsCallable(name).call(data) { result, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let result = result {
                    continuation.resume(returning: result)
                } else {
                    continuation.resume(throwing: FirestoreError.missingSnapshot)
                }
            }
        }
    }

    private func normalize(group code: String) -> String? {
        let trimmed = code.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !trimmed.isEmpty else { return nil }
        let allowed = CharacterSet.alphanumerics
        guard trimmed.rangeOfCharacter(from: allowed.inverted) == nil else { return nil }
        return trimmed
    }

    // MARK: - Drops

    func addDrop(_ drop: Drop) async throws -> String {
        let data = drop.toFirestoreData()
        let ref = drops.document()
        try await setDocument(ref, data: data, merge: false)
        return ref.documentID
    }

    func deleteDrop(dropId: String) async throws {
        guard !dropId.isEmpty else { return }
        let updates: [String: Any] = [
            "isDeleted": true,
            "deletedAt": Timestamp(date: Date())
        ]
        try await setDocument(drops.document(dropId), data: updates, merge: true)
    }

    func setDropLike(dropId: String, userId: String, status: DropLikeStatus) async throws {
        guard !dropId.isEmpty, !userId.isEmpty else { return }
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            self.db.runTransaction({ transaction, errorPointer -> Any? in
                do {
                    let docRef = self.drops.document(dropId)
                    let snapshot = try transaction.getDocument(docRef)
                    guard var drop = Drop(document: snapshot), !drop.isDeleted else {
                        throw FirestoreError.dropMissing
                    }

                    let currentStatus = drop.isLiked(by: userId)
                    if currentStatus == status {
                        return true
                    }

                    switch currentStatus {
                    case .liked:
                        drop.likeCount = max(drop.likeCount - 1, 0)
                        drop.likedBy.removeValue(forKey: userId)
                    case .none:
                        break
                    }

                    switch status {
                    case .liked:
                        drop.likeCount += 1
                        drop.likedBy[userId] = true
                    case .none:
                        drop.likedBy.removeValue(forKey: userId)
                    }

                    // The payload carries only like fields. Dislikes were removed at
                    // task 2.6 and firestore.rules rejects writes that touch them.
                    var updates: [String: Any] = [
                        "likeCount": drop.likeCount
                    ]
                    switch status {
                    case .liked:
                        updates["likedBy.\(userId)"] = true
                    case .none:
                        updates["likedBy.\(userId)"] = FieldValue.delete()
                    }
                    transaction.updateData(updates, forDocument: docRef)
                    return true
                } catch {
                    errorPointer?.pointee = error as NSError
                    return nil
                }
            }, completion: { _, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            })
        }
    }

    func markDropCollected(dropId: String, userId: String) async throws {
        guard !dropId.isEmpty, !userId.isEmpty else { return }
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            self.db.runTransaction({ transaction, errorPointer -> Any? in
                do {
                    let docRef = self.drops.document(dropId)
                    let snapshot = try transaction.getDocument(docRef)
                    let fieldPath = FieldPath(["collectedBy", userId])
                    let alreadyCollected = (snapshot.get(fieldPath) as? Bool) == true
                    if alreadyCollected {
                        return false
                    }
                    transaction.updateData(["collectedBy.\(userId)": true], forDocument: docRef)
                    return true
                } catch {
                    errorPointer?.pointee = error as NSError
                    return nil
                }
            }, completion: { _, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            })
        }
    }
    
    /// Task 4.3 (ADR P6) — redemption is server-only. The `redeemDrop` callable issues
    /// a code to this caller alone; it is never stored on the drop, which every reader
    /// can see. There is no code to type any more.
    func redeemDrop(dropId: String, userId: String) async throws -> RedemptionResult {
        guard PilotFeatureFlags.shared.couponsEnabled else {
            return .error("Offers are disabled for this release")
        }
        guard !dropId.isEmpty, !userId.isEmpty else { return .error("Missing identifiers") }

        do {
            let result = try await callFunction(name: "redeemDrop", data: ["dropId": dropId])
            guard let payload = result.data as? [String: Any],
                  let code = payload["code"] as? String, !code.isEmpty else {
                return .error("Redemption failed. Try again.")
            }
            let count = payload["redemptionCount"] as? Int ?? 0
            let limit = payload["redemptionLimit"] as? Int
            let redeemedAt = payload["redeemedAt"] as? Int
                ?? Int(Date().timeIntervalSince1970 * 1000)
            return .success(count: count, limit: limit, redeemedAt: redeemedAt, code: code)
        } catch let error as NSError {
            // FunctionsErrorCode: 6 = alreadyExists, 8 = resourceExhausted,
            // 9 = failedPrecondition.
            switch error.code {
            case 6: return .alreadyRedeemed
            case 8: return .outOfRedemptions
            case 9: return .notEligible
            default: return .error(error.localizedDescription)
            }
        }
    }

    func blockDropCreator(userId: String, creatorId: String) async throws {
        let sanitizedUser = userId.trimmingCharacters(in: .whitespacesAndNewlines)
        let sanitizedCreator = creatorId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !sanitizedUser.isEmpty, !sanitizedCreator.isEmpty else { return }

        let data: [String: Any] = [
            "creatorId": sanitizedCreator,
            "blockedAt": Date().timeIntervalSince1970
        ]

        try await setDocument(
            users.document(sanitizedUser)
                .collection("blockedCreators")
                .document(sanitizedCreator),
            data: data
        )
    }

    func fetchBlockedCreators(userId: String) async throws -> Set<String> {
        let trimmed = userId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [] }
        do {
            let snapshot = try await getDocuments(users.document(trimmed).collection("blockedCreators"))
            let identifiers: [String] = snapshot.documents.compactMap { doc in
                let documentId = doc.documentID.trimmingCharacters(in: .whitespacesAndNewlines)
                if !documentId.isEmpty { return documentId }
                if let stored = doc.get("creatorId") as? String {
                    let sanitized = stored.trimmingCharacters(in: .whitespacesAndNewlines)
                    return sanitized.isEmpty ? nil : sanitized
                }
                return nil
            }
            return Set(identifiers)
        } catch {
            if let nsError = error as NSError?,
               nsError.domain == FirestoreErrorDomain,
               nsError.code == FirestoreErrorCode.permissionDenied.rawValue {
                print("GeoDrop: Missing permission to load blocked creators for \(trimmed); continuing without filters.")
                return []
            }
            throw error
        }
    }

    func submitReport(dropId: String, reporterId: String, reasonCodes: [String], context: [String: Any] = [:]) async throws {
        guard !dropId.isEmpty, !reporterId.isEmpty else { return }

        let sanitizedReasons = reasonCodes
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        let reasons = sanitizedReasons.isEmpty ? ["unspecified"] : sanitizedReasons

        let now = Timestamp(date: Date())
        var report: [String: Any] = [
            "dropId": dropId,
            "reportedBy": reporterId,
            "reportedAt": now,
            "reasonCodes": reasons,
            "status": "pending"
        ]
        if !context.isEmpty { report["context"] = context }

        let dropRef = drops.document(dropId)
        _ = try await addDocument(reports, data: report)

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            self.db.runTransaction({ transaction, errorPointer -> Any? in
                do {
                    let snapshot = try transaction.getDocument(dropRef)
                    guard snapshot.exists else { return false }

                    // The drop's reportedBy map is integer milliseconds, matching
                    // Android and the rest of the schema. It previously stored a
                    // Timestamp here while the model decoded [String: TimeInterval],
                    // so the cast failed and iOS read the map as empty — it could not
                    // tell which drops it had already reported.
                    let reportedAtMillis = Int(Date().timeIntervalSince1970 * 1000)
                    let already = snapshot.get("reportedBy.\(reporterId)") != nil
                    var updates: [String: Any] = ["reportedBy.\(reporterId)": reportedAtMillis]
                    if !already {
                        let current = snapshot.get("reportCount") as? Int
                            ?? (snapshot.get("reportCount") as? NSNumber)?.intValue
                            ?? 0
                        updates["reportCount"] = current + 1
                    }
                    transaction.setData(updates, forDocument: dropRef, merge: true)
                    return true
                } catch {
                    errorPointer?.pointee = error as NSError
                    return nil
                }
            }, completion: { _, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            })
        }
    }

    func listenForDrops(
        userId: String?,
        allowedGroups: Set<String>,
        restrictToGroups: Bool,
        onChange: @escaping ([Drop]) -> Void
    ) -> ListenerRegistration {
        let normalized = Array(Set(allowedGroups.compactMap(self.normalize))).sorted()
        var queries: [Query] = []

        if !restrictToGroups {
            queries.append(
                drops
                    .whereField("isDeleted", isEqualTo: false)
                    .whereField("visibility", isEqualTo: "PUBLIC")
            )
        }

        if userId != nil {
            normalized.forEach { groupCode in
                queries.append(
                    drops
                        .whereField("isDeleted", isEqualTo: false)
                        .whereField("visibility", isEqualTo: "GROUP")
                        .whereField("groupCode", isEqualTo: groupCode)
                )
            }
        }

        // Server-flagged content is never listed; the viewer preference that used to gate
        // this went with the NSFW pilot flag at task 2.8.
        queries = queries.map { $0.whereField("isNsfw", isEqualTo: false) }

        guard !queries.isEmpty else {
            onChange([])
            return CombinedListenerRegistration([])
        }

        let lock = NSLock()
        var documentsByQuery = Array(repeating: [QueryDocumentSnapshot](), count: queries.count)
        var registrations: [ListenerRegistration] = []

        for (index, query) in queries.enumerated() {
            let registration = query.addSnapshotListener { snapshot, error in
                if let error {
                    print("GeoDrop: Failed to listen for drops: \(error.localizedDescription)")
                }

                lock.lock()
                documentsByQuery[index] = snapshot?.documents ?? []
                let documents = documentsByQuery.flatMap { $0 }
                lock.unlock()

                var unique: [String: Drop] = [:]
                documents.compactMap(Drop.init(document:)).forEach { drop in
                    unique[drop.id] = drop
                }
                let filtered = unique.values.filter { drop in
                    if drop.isDeleted { return false }
                    if drop.isNsfw { return false }
                    if drop.hasBeenCollected { return false }
                    if drop.isExpired { return false }
                    return true
                }
                onChange(filtered.sorted { $0.createdAt > $1.createdAt })
            }
            registrations.append(registration)
        }

        return CombinedListenerRegistration(registrations)
    }

    func getDropsForUser(userId: String) async throws -> [Drop] {
        let snapshot = try await getDocuments(
            drops
                .whereField("createdBy", isEqualTo: userId)
                .whereField("isDeleted", isEqualTo: false)
        )
        return snapshot.documents.compactMap(Drop.init(document:))
    }

    func getBusinessDrops(businessId: String) async throws -> [Drop] {
        guard !businessId.isEmpty else { return [] }
        let snapshot = try await getDocuments(
            drops
                .whereField("businessId", isEqualTo: businessId)
                .whereField("createdBy", isEqualTo: businessId)
                .whereField("isDeleted", isEqualTo: false)
        )
        return snapshot.documents.compactMap(Drop.init(document:))
    }

    /// Reads the task 4.4 rollup for every experience this user owns. A missing
    /// summary is returned as zeroes so new experiences are visible immediately.
    func getOwnedExperienceAnalytics(userId: String) async throws -> [ExperienceAnalytics] {
        guard !userId.isEmpty else { return [] }

        let ownedMemberships = try await fetchUserGroupMemberships(userId: userId)
            .filter { $0.role == .owner && $0.ownerId == userId }
        var analytics: [ExperienceAnalytics] = []

        for membership in ownedMemberships {
            let snapshot = try await getDocument(
                db.collection("groups")
                    .document(membership.code)
                    .collection("analytics")
                    .document("summary")
            )
            analytics.append(
                ExperienceAnalytics(groupCode: membership.code, data: snapshot.data())
            )
        }

        return analytics.sorted { $0.groupCode < $1.groupCode }
    }

    // MARK: - Groups

    func fetchUserGroupMemberships(userId: String) async throws -> [GroupMembership] {
        guard !userId.isEmpty else { return [] }
        let snapshot = try await getDocuments(users.document(userId).collection("groups"))
        return snapshot.documents.compactMap { doc in
            let rawCode = (doc.get("code") as? String) ?? doc.documentID
            guard let normalized = normalize(group: rawCode) else { return nil }
            let owner = (doc.get("ownerId") as? String) ?? userId
            let role = GroupRole.from(raw: doc.get("role"))
            let resolvedRole: GroupRole = owner == userId ? .owner : role
            return GroupMembership(code: normalized, ownerId: owner, role: resolvedRole)
        }.sorted { $0.code < $1.code }
    }

    func listenForGroupMemberships(userId: String, onChange: @escaping ([GroupMembership]) -> Void) -> ListenerRegistration? {
        guard !userId.isEmpty else { return nil }
        return users.document(userId).collection("groups").addSnapshotListener { snapshot, error in
            guard let snapshot = snapshot else {
                print("GeoDrop: Failed to listen for groups: \(error?.localizedDescription ?? "unknown")")
                onChange([])
                return
            }
            let memberships = snapshot.documents.compactMap { doc -> GroupMembership? in
                let rawCode = (doc.get("code") as? String) ?? doc.documentID
                guard let normalized = self.normalize(group: rawCode) else { return nil }
                let owner = (doc.get("ownerId") as? String) ?? userId
                let role = GroupRole.from(raw: doc.get("role"))
                return GroupMembership(code: normalized, ownerId: owner, role: owner == userId ? .owner : role)
            }.sorted { $0.code < $1.code }
            onChange(memberships)
        }
    }

    func joinGroup(userId: String, code: String, allowCreate: Bool) async throws -> GroupMembership {
        guard !userId.isEmpty else { throw FirestoreError.invalidInput }
        guard let normalized = normalize(group: code) else { throw FirestoreError.invalidGroupCode }
        let action = allowCreate ? "CREATE" : "JOIN"
        let result = try await functions.httpsCallable("manageGroup").call([
            "action": action,
            "code": normalized
        ])
        guard let payload = result.data as? [String: Any],
              let ownerId = payload["ownerId"] as? String,
              !ownerId.isEmpty else {
            throw FirestoreError.missingSnapshot
        }
        let returnedCode = normalize(group: payload["code"] as? String ?? normalized) ?? normalized
        let role = GroupRole.from(raw: payload["role"])
        return GroupMembership(code: returnedCode, ownerId: ownerId, role: role)
    }

    func leaveGroup(userId: String, code: String) async throws {
        guard !userId.isEmpty else { return }
        guard let normalized = normalize(group: code) else { return }
        _ = try await functions.httpsCallable("manageGroup").call([
            "action": "LEAVE",
            "code": normalized
        ])
    }

    func isGroupOwner(userId: String, code: String) async throws -> Bool {
        guard !userId.isEmpty else { return false }
        guard let normalized = normalize(group: code) else { return false }
        let snapshot = try await getDocument(
            users.document(userId).collection("groups").document(normalized)
        )
        let owner = snapshot.get("ownerId") as? String
        let role = GroupRole.from(raw: snapshot.get("role"))
        return snapshot.exists && owner == userId && role == .owner
    }

    // MARK: - Profiles

    func ensureUserProfile(userId: String, displayName: String?) async throws -> UserProfile {
        guard !userId.isEmpty else { return UserProfile() }

        let ref = users.document(userId)
        let snapshot = try await getDocument(ref)

        let storedRole = UserRole.from(raw: snapshot.get("role"))
        let storedBusinessName = snapshot.get("businessName") as? String
        let storedCategories = (snapshot.get("businessCategories") as? [String])?.compactMap(BusinessCategory.from) ?? []
        let storedUsername = snapshot.get("username") as? String
        let storedDisplayName = snapshot.get("displayName") as? String

        // Only client-authored fields are written here. `businessName` and
        // `businessCategories` are server-authored (task 2.7, see firestore.rules): the
        // `updateBusinessProfile` callable owns them, and a client write is rejected.
        // The NSFW preference is no longer written at all (task 2.8) — nothing reads it,
        // firestore.rules still forces any surviving value to false, and
        // `functions/scripts/backfill-launch-fields.js` clears legacy values server-side.
        var updates: [String: Any] = [:]
        if !snapshot.exists {
            updates["role"] = UserRole.explorer.rawValue
            updates["displayName"] = displayName
        } else {
            if snapshot.get("role") == nil { updates["role"] = storedRole.rawValue }
            if let displayName, storedDisplayName == nil { updates["displayName"] = displayName }
        }

        if !updates.isEmpty {
            try await setDocument(ref, data: updates, merge: true)
        }

        return UserProfile(
            id: userId,
            displayName: storedDisplayName ?? displayName,
            username: storedUsername,
            role: storedRole,
            businessName: storedBusinessName,
            businessCategories: storedCategories
        )
    }

    func updateBusinessProfile(userId: String, name: String, categories: [BusinessCategory]) async throws -> UserProfile {
        let sanitized = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !sanitized.isEmpty else { throw FirestoreError.invalidInput }
        guard !categories.isEmpty else { throw FirestoreError.invalidInput }

        var profile = try await ensureUserProfile(userId: userId, displayName: nil)
        _ = try await callFunction(name: "updateBusinessProfile", data: [
            "businessName": sanitized,
            "businessCategories": categories.map { $0.id }
        ])
        profile.businessName = sanitized
        profile.businessCategories = categories
        profile.role = .business
        return profile
    }

    func updateExplorerUsername(userId: String, desired: String) async throws -> UserProfile {
        let sanitized = try ExplorerUsername.sanitize(desired)
        do {
            _ = try await callFunction(name: "claimExplorerUsername", data: ["desiredUsername": sanitized])
        } catch {
            throw error
        }
        try await setDocument(users.document(userId), data: ["username": sanitized])
        var profile = try await ensureUserProfile(userId: userId, displayName: nil)
        profile.username = sanitized
        return profile
    }

    // Task 4.6 — `migrateExplorerAccount` lived here and copied a display name
    // between profiles. It had no caller on iOS, so an iOS guest lost everything
    // on sign-in with nothing even attempting a repair, and the copy it did
    // perform could not have moved drops anyway: no rule permits rewriting
    // `createdBy`. Continuity now happens in `AuthService`, by linking the
    // anonymous account in place or, where that is impossible, by the
    // `mergeGuestAccount` callable.

    func registerMessagingToken(userId: String, token: String, platform: String) async {
        let trimmedUserId = userId.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedToken = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedUserId.isEmpty, !trimmedToken.isEmpty else { return }
        let payload: [String: Any] = [
            "token": trimmedToken,
            "platform": platform,
            "updatedAt": Timestamp(date: Date())
        ]
        do {
            try await setDocument(
                users.document(trimmedUserId)
                    .collection("notificationTokens")
                    .document(trimmedToken),
                data: payload,
                merge: true
            )
        } catch {
            print("GeoDrop: Failed to register messaging token \(error)")
        }
    }
}

// MARK: - Errors

extension FirestoreService {
    enum FirestoreError: Error {
        case missingSnapshot
        case invalidGroupCode
        case invalidInput
        case groupMissing
        case dropMissing

        var localizedDescription: String {
            switch self {
            case .missingSnapshot: return "Missing snapshot"
            case .invalidGroupCode: return "Invalid group code"
            case .invalidInput: return "Invalid input"
            case .groupMissing: return "Group does not exist"
            case .dropMissing: return "Drop does not exist"
            }
        }
    }
}

enum RedemptionResult: Equatable {
    case success(count: Int, limit: Int?, redeemedAt: Int, code: String?)
    case invalidCode
    case alreadyRedeemed
    case outOfRedemptions
    case notEligible
    case error(String)
}
