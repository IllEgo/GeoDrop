import Foundation

struct GroupMembership: Identifiable, Equatable {
    var id: String { code }
    let code: String
    let ownerId: String
    var role: GroupRole

    init(code: String, ownerId: String, role: GroupRole) {
        self.code = code
        self.ownerId = ownerId
        self.role = role
    }
}

/// Owner-only aggregate written by the task 4.4 backend at
/// `groups/{groupCode}/analytics/summary`.
struct ExperienceAnalytics: Identifiable, Equatable {
    var id: String { groupCode }
    let groupCode: String
    let drops: Int
    let collects: Int
    let redemptions: Int
    let updatedAtMilliseconds: Int64?
    let reconciledAtMilliseconds: Int64?

    init(groupCode: String, data: [String: Any]?) {
        func count(_ field: String) -> Int {
            max(0, (data?[field] as? NSNumber)?.intValue ?? 0)
        }

        func timestamp(_ field: String) -> Int64? {
            guard let value = (data?[field] as? NSNumber)?.int64Value, value > 0 else {
                return nil
            }
            return value
        }

        self.groupCode = groupCode
        self.drops = count("drops")
        self.collects = count("collects")
        self.redemptions = count("redemptions")
        self.updatedAtMilliseconds = timestamp("updatedAt")
        self.reconciledAtMilliseconds = timestamp("reconciledAt")
    }
}

/// Group membership roles, as written by the `manageGroup` callable — the only writer
/// of these documents. It emits `OWNER` or `SUBSCRIBER`; there is no editor role
/// (task 2.7).
enum GroupRole: String, Codable {
    case owner = "OWNER"
    case subscriber = "SUBSCRIBER"

    /// Unrecognized or missing values resolve to the least-privileged role.
    static func from(raw: Any?) -> GroupRole {
        guard let value = raw as? String else { return .subscriber }
        return GroupRole(rawValue: value) ?? .subscriber
    }
}

/// The launch scope has exactly two account types. `role` is server-authored:
/// promotion to `.business` happens only in the `updateBusinessProfile` callable.
enum UserRole: String, Codable {
    case explorer = "EXPLORER"
    case business = "BUSINESS"

    /// firestore.rules compares the stored string exactly (`role == 'BUSINESS'`), so no
    /// case folding here — anything off-model resolves to the least-privileged type.
    static func from(raw: Any?) -> UserRole {
        guard let value = raw as? String else { return .explorer }
        return UserRole(rawValue: value) ?? .explorer
    }
}
