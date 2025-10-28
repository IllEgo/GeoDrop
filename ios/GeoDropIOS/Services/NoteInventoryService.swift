import Foundation

final class NoteInventoryService {
    struct Inventory: Codable, Equatable {
        var collectedDrops: [String: Drop] = [:]
        var ignoredDropIDs: Set<String> = []
        var likedStatuses: [String: DropLikeStatus] = [:]
        var redeemedDrops: [String: RedemptionRecord] = [:]

        static let empty = Inventory()
    }

    struct RedemptionRecord: Codable, Equatable {
        var redeemedAt: Date
        var lastCode: String?
        var count: Int
        var limit: Int?
    }

    enum NotificationKeys {
        static let userIdentifier = "userIdentifier"
    }

    static let shared = NoteInventoryService()
    static let inventoryDidChangeNotification = Notification.Name("NoteInventoryService.inventoryDidChange")

    private let defaults: UserDefaults
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder
    private let accessQueue = DispatchQueue(label: "com.geodrop.noteInventory.queue", qos: .userInitiated)
    private var cache: [String: Inventory] = [:]

    init(userDefaults: UserDefaults = .standard) {
        self.defaults = userDefaults
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        self.encoder = encoder
        let decoder = JSONDecoder()
        self.decoder = decoder
    }

    func inventory(for userId: String?) -> Inventory {
        let identifier = storageIdentifier(for: userId)
        return accessQueue.sync {
            if let cached = cache[identifier] {
                return cached
            }
            let loaded = loadInventory(identifier: identifier)
            cache[identifier] = loaded
            return loaded
        }
    }

    func storageIdentifier(for userId: String?) -> String {
        guard let resolved = resolvedUserId(from: userId) else {
            return "anonymous"
        }
        return resolved
    }

    func storeCollected(drop: Drop, for userId: String?) {
        mutateInventory(for: userId) { inventory, resolvedUserId in
            var storedDrop = drop
            if let resolvedUserId {
                let status = inventory.likedStatuses[drop.id] ?? .none
                switch status {
                case .liked:
                    storedDrop.likedBy[resolvedUserId] = true
                    storedDrop.dislikedBy.removeValue(forKey: resolvedUserId)
                case .disliked:
                    storedDrop.dislikedBy[resolvedUserId] = true
                    storedDrop.likedBy.removeValue(forKey: resolvedUserId)
                case .none:
                    storedDrop.likedBy.removeValue(forKey: resolvedUserId)
                    storedDrop.dislikedBy.removeValue(forKey: resolvedUserId)
                }
                if let record = inventory.redeemedDrops[drop.id] {
                    storedDrop.redemptionCount = record.count
                    storedDrop.redemptionLimit = record.limit
                    storedDrop.redeemedBy[resolvedUserId] = record.redeemedAt.timeIntervalSince1970
                }
            }
            inventory.collectedDrops[drop.id] = storedDrop
        }
    }

    func removeCollected(dropId: String, for userId: String?) {
        mutateInventory(for: userId) { inventory, _ in
            inventory.collectedDrops.removeValue(forKey: dropId)
            inventory.likedStatuses.removeValue(forKey: dropId)
            inventory.redeemedDrops.removeValue(forKey: dropId)
        }
    }

    func setIgnored(dropId: String, isIgnored: Bool, for userId: String?) {
        mutateInventory(for: userId) { inventory, _ in
            if isIgnored {
                inventory.ignoredDropIDs.insert(dropId)
            } else {
                inventory.ignoredDropIDs.remove(dropId)
            }
        }
    }

    func setLikeStatus(_ status: DropLikeStatus, dropId: String, drop: Drop? = nil, for userId: String?) {
        mutateInventory(for: userId) { inventory, resolvedUserId in
            let previousStatus = inventory.likedStatuses[dropId] ?? .none
            guard previousStatus != status else { return }

            if status == .none {
                inventory.likedStatuses.removeValue(forKey: dropId)
            } else {
                inventory.likedStatuses[dropId] = status
            }

            func applyStatus(to storedDrop: inout Drop, resolvedUserId: String?) {
                guard let resolvedUserId else { return }

                switch previousStatus {
                case .liked:
                    storedDrop.likeCount = max(storedDrop.likeCount - 1, 0)
                    storedDrop.likedBy.removeValue(forKey: resolvedUserId)
                case .disliked:
                    storedDrop.dislikeCount = max(storedDrop.dislikeCount - 1, 0)
                    storedDrop.dislikedBy.removeValue(forKey: resolvedUserId)
                case .none:
                    break
                }

                switch status {
                case .liked:
                    storedDrop.likeCount += 1
                    storedDrop.likedBy[resolvedUserId] = true
                    storedDrop.dislikedBy.removeValue(forKey: resolvedUserId)
                case .disliked:
                    storedDrop.dislikeCount += 1
                    storedDrop.dislikedBy[resolvedUserId] = true
                    storedDrop.likedBy.removeValue(forKey: resolvedUserId)
                case .none:
                    storedDrop.likedBy.removeValue(forKey: resolvedUserId)
                    storedDrop.dislikedBy.removeValue(forKey: resolvedUserId)
                }
            }

            if var stored = inventory.collectedDrops[dropId] {
                applyStatus(to: &stored, resolvedUserId: resolvedUserId)
                inventory.collectedDrops[dropId] = stored
            } else if var provided = drop {
                applyStatus(to: &provided, resolvedUserId: resolvedUserId)
                inventory.collectedDrops[dropId] = provided
            }
        }
    }

    func setRedeemed(dropId: String, count: Int, limit: Int?, code: String?, redeemedAt: Date, drop: Drop? = nil, for userId: String?) {
        mutateInventory(for: userId) { inventory, resolvedUserId in
            let record = RedemptionRecord(redeemedAt: redeemedAt, lastCode: code, count: count, limit: limit)
            inventory.redeemedDrops[dropId] = record

            if var stored = inventory.collectedDrops[dropId] {
                stored.redemptionCount = count
                stored.redemptionLimit = limit
                if let resolvedUserId {
                    stored.redeemedBy[resolvedUserId] = redeemedAt.timeIntervalSince1970
                }
                inventory.collectedDrops[dropId] = stored
            } else if var provided = drop {
                provided.redemptionCount = count
                provided.redemptionLimit = limit
                if let resolvedUserId {
                    provided.redeemedBy[resolvedUserId] = redeemedAt.timeIntervalSince1970
                }
                inventory.collectedDrops[dropId] = provided
            }
        }
    }

    func clearRedemptionState(dropId: String, for userId: String?) {
        mutateInventory(for: userId) { inventory, resolvedUserId in
            inventory.redeemedDrops.removeValue(forKey: dropId)
            if let resolvedUserId, var stored = inventory.collectedDrops[dropId] {
                stored.redeemedBy.removeValue(forKey: resolvedUserId)
                inventory.collectedDrops[dropId] = stored
            }
        }
    }

    func merge(remoteDrops: [Drop], for userId: String?) {
        guard !remoteDrops.isEmpty else { return }
        mutateInventory(for: userId) { inventory, resolvedUserId in
            guard let resolvedUserId else { return }

            for drop in remoteDrops {
                let isRemoteCollected = drop.collectedBy[resolvedUserId] == true
                let wasPreviouslyCollected = inventory.collectedDrops[drop.id] != nil

                if isRemoteCollected {
                    inventory.collectedDrops[drop.id] = drop
                } else if wasPreviouslyCollected {
                    var mergedDrop = drop
                    mergedDrop.collectedBy[resolvedUserId] = true
                    inventory.collectedDrops[drop.id] = mergedDrop
                } else {
                    inventory.collectedDrops.removeValue(forKey: drop.id)
                    inventory.likedStatuses.removeValue(forKey: drop.id)
                    inventory.redeemedDrops.removeValue(forKey: drop.id)
                    continue
                }

                let likeStatus = drop.isLiked(by: resolvedUserId)
                if likeStatus == .none {
                    inventory.likedStatuses.removeValue(forKey: drop.id)
                } else {
                    inventory.likedStatuses[drop.id] = likeStatus
                }

                if var stored = inventory.collectedDrops[drop.id] {
                    switch likeStatus {
                    case .liked:
                        stored.likedBy[resolvedUserId] = true
                        stored.dislikedBy.removeValue(forKey: resolvedUserId)
                    case .disliked:
                        stored.dislikedBy[resolvedUserId] = true
                        stored.likedBy.removeValue(forKey: resolvedUserId)
                    case .none:
                        stored.likedBy.removeValue(forKey: resolvedUserId)
                        stored.dislikedBy.removeValue(forKey: resolvedUserId)
                    }

                    if let redeemedTimestamp = drop.redeemedBy[resolvedUserId] {
                        stored.redemptionCount = drop.redemptionCount
                        stored.redemptionLimit = drop.redemptionLimit
                        stored.redeemedBy[resolvedUserId] = redeemedTimestamp
                        let existingCode = inventory.redeemedDrops[drop.id]?.lastCode
                        let record = RedemptionRecord(
                            redeemedAt: Date(timeIntervalSince1970: redeemedTimestamp),
                            lastCode: existingCode,
                            count: drop.redemptionCount,
                            limit: drop.redemptionLimit
                        )
                        inventory.redeemedDrops[drop.id] = record
                    } else {
                        stored.redeemedBy.removeValue(forKey: resolvedUserId)
                        inventory.redeemedDrops.removeValue(forKey: drop.id)
                    }

                    inventory.collectedDrops[drop.id] = stored
                }
            }
        }
    }

    func clearCache() {
        accessQueue.sync {
            cache.removeAll()
        }
    }

    // MARK: - Private helpers

    private func mutateInventory(for userId: String?, _ mutation: (inout Inventory, String?) -> Void) {
        let identifier = storageIdentifier(for: userId)
        let resolvedUserId = resolvedUserId(from: userId)
        accessQueue.sync {
            var inventory = cache[identifier] ?? loadInventory(identifier: identifier)
            let original = inventory
            mutation(&inventory, resolvedUserId)
            guard inventory != original else { return }
            cache[identifier] = inventory
            persist(inventory, identifier: identifier)
            notifyChange(identifier: identifier)
        }
    }

    private func notifyChange(identifier: String) {
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: Self.inventoryDidChangeNotification,
                object: self,
                userInfo: [NotificationKeys.userIdentifier: identifier]
            )
        }
    }

    private func loadInventory(identifier: String) -> Inventory {
        let key = storageKey(for: identifier)
        guard let data = defaults.data(forKey: key) else { return .empty }
        do {
            return try decoder.decode(Inventory.self, from: data)
        } catch {
            defaults.removeObject(forKey: key)
            return .empty
        }
    }

    private func persist(_ inventory: Inventory, identifier: String) {
        let key = storageKey(for: identifier)
        do {
            let data = try encoder.encode(inventory)
            defaults.set(data, forKey: key)
        } catch {
            defaults.removeObject(forKey: key)
        }
    }

    private func storageKey(for identifier: String) -> String {
        "geodrop.noteInventory.\(identifier)"
    }

    private func resolvedUserId(from userId: String?) -> String? {
        guard let trimmed = userId?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else {
            return nil
        }
        return trimmed
    }
}
