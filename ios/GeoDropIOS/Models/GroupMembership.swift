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

enum GroupRole: String, Codable {
    case owner = "OWNER"
    case editor = "EDITOR"
    case subscriber = "SUBSCRIBER"

    static func from(raw: Any?) -> GroupRole {
        guard let value = (raw as? String)?.uppercased() else { return .owner }
        return GroupRole(rawValue: value) ?? .owner
    }
}

enum UserRole: String, Codable {
    case explorer = "EXPLORER"
    case business = "BUSINESS"

    static func from(raw: Any?) -> UserRole {
        guard let value = (raw as? String)?.uppercased() else { return .explorer }
        return UserRole(rawValue: value) ?? .explorer
    }
}