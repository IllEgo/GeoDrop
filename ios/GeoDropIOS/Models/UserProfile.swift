import Foundation

struct UserProfile: Equatable {
    var id: String
    var displayName: String?
    var username: String?
    var role: UserRole
    var businessName: String?
    var businessCategories: [BusinessCategory]
    var nsfwEnabled: Bool
    var nsfwEnabledAt: Date?

    init(
        id: String = "",
        displayName: String? = nil,
        username: String? = nil,
        role: UserRole = .explorer,
        businessName: String? = nil,
        businessCategories: [BusinessCategory] = [],
        nsfwEnabled: Bool = false,
        nsfwEnabledAt: Date? = nil
    ) {
        self.id = id
        self.displayName = displayName
        self.username = username
        self.role = role
        self.businessName = businessName
        self.businessCategories = businessCategories
        self.nsfwEnabled = nsfwEnabled
        self.nsfwEnabledAt = nsfwEnabledAt
    }
}

struct BusinessCategory: Identifiable, Equatable, Hashable, Codable {
    let id: String
    let displayName: String

    static let all: [BusinessCategory] = [
        .init(id: "coffee", displayName: "Coffee Shop"),
        .init(id: "restaurant", displayName: "Restaurant"),
        .init(id: "bar", displayName: "Bar"),
        .init(id: "retail", displayName: "Retail"),
        .init(id: "fitness", displayName: "Fitness"),
        .init(id: "arts", displayName: "Arts"),
        .init(id: "tour", displayName: "Tour"),
        .init(id: "other", displayName: "Other")
    ]

    static func from(id: String?) -> BusinessCategory? {
        guard let id = id?.lowercased(), !id.isEmpty else { return nil }
        return all.first { $0.id == id }
    }
}