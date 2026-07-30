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