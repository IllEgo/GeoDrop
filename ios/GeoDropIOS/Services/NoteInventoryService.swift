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
                if inventory.likedStatuses[drop.id] == .liked {
                    storedDrop.likedBy[resolvedUserId] = true
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
            if status == .none {
                inventory.likedStatuses.removeValue(forKey: dropId)
            } else {
                inventory.likedStatuses[dropId] = status
            }

            if var stored = inventory.collectedDrops[dropId] {
                if let resolvedUserId {
                    if status == .none {
                        stored.likedBy.removeValue(forKey: resolvedUserId)
                    } else {
                        stored.likedBy[resolvedUserId] = true
                    }
                }
                inventory.collectedDrops[dropId] = stored
            } else if var provided = drop {
                if let resolvedUserId {
                    provided.likedBy[resolvedUserId] = (status == .liked)
                }
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
                if drop.collectedBy[resolvedUserId] == true {
                    inventory.collectedDrops[drop.id] = drop
                } else {
                    inventory.collectedDrops.removeValue(forKey: drop.id)
                    inventory.likedStatuses.removeValue(forKey: drop.id)
                    inventory.redeemedDrops.removeValue(forKey: drop.id)
                    continue
                }

                let likeStatus = drop.isLiked(by: resolvedUserId)
                if likeStatus == .liked {
                    inventory.likedStatuses[drop.id] = .liked
                } else {
                    inventory.likedStatuses.removeValue(forKey: drop.id)
                }

                if var stored = inventory.collectedDrops[drop.id] {
                    if inventory.likedStatuses[drop.id] == .liked {
                        stored.likedBy[resolvedUserId] = true
                    } else {
                        stored.likedBy.removeValue(forKey: resolvedUserId)
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
