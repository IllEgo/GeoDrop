import Foundation
import FirebaseFirestore
import FirebaseFunctions

final class FirestoreService {
    static let shared = FirestoreService()

    private let db = Firestore.firestore()
    private lazy var drops = db.collection("drops")
    private lazy var users = db.collection("users")
    private lazy var usernames = db.collection("usernames")
    private lazy var reports = db.collection("reports")
    private lazy var groups = db.collection("groups")
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

                    let shouldLike = status == .liked
                    let alreadyLiked = drop.likedBy[userId] == true

                    // No-op if state unchanged
                    if shouldLike == alreadyLiked {
                        return true
                    }

                    if alreadyLiked {
                        drop.likedBy.removeValue(forKey: userId)
                        drop.likeCount = max(drop.likeCount - 1, 0)
                    }
                    if shouldLike {
                        drop.likedBy[userId] = true
                        drop.likeCount += 1
                    }

                    var updates: [String: Any] = ["likeCount": drop.likeCount]
                    updates["likedBy.\(userId)"] = shouldLike ? true : FieldValue.delete()
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
        let dropSnapshot = try? await getDocument(dropRef)
        if let drop = dropSnapshot.flatMap(Drop.init(document:)) {
            report["dropSnapshot"] = drop.toFirestoreData()
        }

        _ = try await addDocument(reports, data: report)

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            self.db.runTransaction({ transaction, errorPointer -> Any? in
                do {
                    let snapshot = try transaction.getDocument(dropRef)
                    guard snapshot.exists else { return false }

                    let already = (snapshot.get("reportedBy.\(reporterId)") as? Timestamp) != nil
                    var updates: [String: Any] = ["reportedBy.\(reporterId)": now]
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

    func listenForDrops(userId: String?, allowedGroups: Set<String>, allowNsfw: Bool, onChange: @escaping ([Drop]) -> Void) -> ListenerRegistration {
        drops.whereField("isDeleted", isEqualTo: false).addSnapshotListener { snapshot, error in
            guard let documents = snapshot?.documents else {
                print("GeoDrop: Failed to listen for drops: \(error?.localizedDescription ?? "unknown error")")
                onChange([])
                return
            }
            let normalized = allowedGroups.compactMap(self.normalize)
            let filtered = documents.compactMap(Drop.init(document:)).filter { drop in
                if drop.isDeleted { return false }
                if let group = drop.groupCode, !normalized.contains(group), group != "PUBLIC" { return false }
                if drop.isNsfw && !allowNsfw && drop.createdBy != userId { return false }
                if drop.isExpired { return false }
                return true
            }
            onChange(filtered.sorted { $0.createdAt > $1.createdAt })
        }
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
                .whereField("isDeleted", isEqualTo: false)
        )
        return snapshot.documents.compactMap(Drop.init(document:))
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

        return try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<GroupMembership, Error>) in
            self.db.runTransaction({ transaction, errorPointer -> Any? in
                do {
                    let groupRef = self.groups.document(normalized)
                    let snapshot = try transaction.getDocument(groupRef)
                    let now = Timestamp(date: Date())

                    let existingOwner = snapshot.get("ownerId") as? String
                    let creating = !snapshot.exists || existingOwner == nil

                    let resolvedOwner: String
                    if creating {
                        guard allowCreate else { throw FirestoreError.groupMissing }
                        resolvedOwner = userId
                        transaction.setData([
                            "ownerId": userId,
                            "createdAt": now,
                            "updatedAt": now
                        ], forDocument: groupRef, merge: true)
                    } else {
                        resolvedOwner = existingOwner ?? userId
                        transaction.setData(["updatedAt": now], forDocument: groupRef, merge: true)
                    }

                    let role: GroupRole = resolvedOwner == userId ? .owner : .subscriber
                    transaction.setData([
                        "code": normalized,
                        "role": role.rawValue,
                        "ownerId": resolvedOwner,
                        "updatedAt": now
                    ], forDocument: self.users.document(userId).collection("groups").document(normalized), merge: true)

                    return GroupMembership(code: normalized, ownerId: resolvedOwner, role: role)
                } catch {
                    errorPointer?.pointee = error as NSError
                    return nil
                }
            }, completion: { result, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let membership = result as? GroupMembership {
                    continuation.resume(returning: membership)
                } else {
                    continuation.resume(throwing: FirestoreError.missingSnapshot)
                }
            })
        }
    }

    func leaveGroup(userId: String, code: String) async throws {
        guard !userId.isEmpty else { return }
        guard let normalized = normalize(group: code) else { return }
        try await deleteDocument(users.document(userId).collection("groups").document(normalized))
        try await setDocument(groups.document(normalized), data: ["updatedAt": Timestamp(date: Date())])
    }

    func isGroupOwner(userId: String, code: String) async throws -> Bool {
        guard !userId.isEmpty else { return false }
        guard let normalized = normalize(group: code) else { return false }
        let snapshot = try await getDocument(groups.document(normalized))
        let owner = snapshot.get("ownerId") as? String
        return owner == userId
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
        let nsfwEnabled = snapshot.get("nsfwEnabled") as? Bool ?? false
        let nsfwEnabledAt = (snapshot.get("nsfwEnabledAt") as? Timestamp)?.dateValue()

        var updates: [String: Any] = [:]
        if !snapshot.exists {
            updates["role"] = storedRole.rawValue
            updates["displayName"] = displayName
            updates["businessName"] = storedBusinessName
            updates["businessCategories"] = storedCategories.map { $0.id }
            updates["username"] = storedUsername
            updates["nsfwEnabled"] = nsfwEnabled
            if let date = nsfwEnabledAt { updates["nsfwEnabledAt"] = Timestamp(date: date) }
        } else {
            if snapshot.get("role") == nil { updates["role"] = storedRole.rawValue }
            if let displayName, storedDisplayName == nil { updates["displayName"] = displayName }
            if snapshot.get("businessCategories") == nil { updates["businessCategories"] = storedCategories.map { $0.id } }
            if snapshot.get("nsfwEnabled") == nil { updates["nsfwEnabled"] = nsfwEnabled }
            if snapshot.get("nsfwEnabledAt") == nil, let date = nsfwEnabledAt { updates["nsfwEnabledAt"] = Timestamp(date: date) }
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
            businessCategories: storedCategories,
            nsfwEnabled: nsfwEnabled,
            nsfwEnabledAt: nsfwEnabledAt
        )
    }

    func updateNsfwPreference(userId: String, enabled: Bool) async throws -> UserProfile {
        var profile = try await ensureUserProfile(userId: userId, displayName: nil)
        if enabled {
            try await setDocument(users.document(userId), data: [
                "nsfwEnabled": true,
                "nsfwEnabledAt": Timestamp(date: Date())
            ])
            profile.nsfwEnabled = true
            profile.nsfwEnabledAt = Date()
        } else {
            try await setDocument(users.document(userId), data: [
                "nsfwEnabled": false,
                "nsfwEnabledAt": FieldValue.delete()
            ])
            profile.nsfwEnabled = false
            profile.nsfwEnabledAt = nil
        }
        return profile
    }

    func updateBusinessProfile(userId: String, name: String, categories: [BusinessCategory]) async throws -> UserProfile {
        let sanitized = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !sanitized.isEmpty else { throw FirestoreError.invalidInput }
        guard !categories.isEmpty else { throw FirestoreError.invalidInput }

        var profile = try await ensureUserProfile(userId: userId, displayName: nil)
        try await setDocument(users.document(userId), data: [
            "businessName": sanitized,
            "businessCategories": categories.map { $0.id },
            "role": UserRole.business.rawValue
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

    func migrateExplorerAccount(previousUserId: String, newUserId: String) async {
        guard !previousUserId.isEmpty, !newUserId.isEmpty, previousUserId != newUserId else { return }
        do {
            let snapshot = try await getDocument(users.document(previousUserId))
            guard snapshot.exists, let data = snapshot.data() else { return }
            try await setDocument(users.document(newUserId), data: data, merge: true)
        } catch {
            print("GeoDrop: Failed to migrate explorer account \(error)")
        }
    }

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
