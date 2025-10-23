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
    enum Group: String, CaseIterable, Codable {
        case foodAndBeverage = "FOOD_AND_BEVERAGE"
        case retailAndShopping = "RETAIL_AND_SHOPPING"
        case hospitalityAndTourism = "HOSPITALITY_AND_TOURISM"
        case eventsAndEntertainment = "EVENTS_AND_ENTERTAINMENT"
        case communityAndEducation = "COMMUNITY_AND_EDUCATION"
        case realEstateAndLocalServices = "REAL_ESTATE_AND_LOCAL_SERVICES"
        case travelAndTransportation = "TRAVEL_AND_TRANSPORTATION"
        case marketingAndCampaigns = "MARKETING_AND_CAMPAIGNS"

        var displayName: String {
            switch self {
            case .foodAndBeverage: return "Food & Beverage"
            case .retailAndShopping: return "Retail & Shopping"
            case .hospitalityAndTourism: return "Hospitality & Tourism"
            case .eventsAndEntertainment: return "Events & Entertainment"
            case .communityAndEducation: return "Community & Education"
            case .realEstateAndLocalServices: return "Real Estate & Local Services"
            case .travelAndTransportation: return "Travel & Transportation"
            case .marketingAndCampaigns: return "Marketing & Engagement Campaigns"
            }
        }
    }
    
    let id: String
    let group: Group
    let displayName: String
    let detail: String

    static let all: [BusinessCategory] = [
        .init(
            id: "FOOD_RESTAURANTS_CAFES",
            group: .foodAndBeverage,
            displayName: "Restaurants / Cafés",
            detail: "Drop coupons, daily specials, and loyalty rewards."
        ),
        .init(
            id: "FOOD_TRUCKS_STREET_VENDORS",
            group: .foodAndBeverage,
            displayName: "Food trucks & street vendors",
            detail: "Notify explorers when you're nearby."
        ),
        .init(
            id: "FOOD_BARS_BREWERIES",
            group: .foodAndBeverage,
            displayName: "Bars & breweries",
            detail: "Promote happy hour deals and live music nights."
        ),
        .init(
            id: "RETAIL_LOCAL_SHOPS",
            group: .retailAndShopping,
            displayName: "Local shops & boutiques",
            detail: "Offer geofenced discounts for in-store visits."
        ),
        .init(
            id: "RETAIL_MALLS_SHOPPING_CENTERS",
            group: .retailAndShopping,
            displayName: "Malls / shopping centers",
            detail: "Share event promotions, scavenger hunts, or coupon codes."
        ),
        .init(
            id: "RETAIL_POP_UPS_MARKETS",
            group: .retailAndShopping,
            displayName: "Pop-up stores & markets",
            detail: "Announce flash sales or limited stock."
        ),
        .init(
            id: "HOSPITALITY_HOTELS_RESORTS",
            group: .hospitalityAndTourism,
            displayName: "Hotels & resorts",
            detail: "Send welcome notes, on-site reminders, or hidden perks."
        ),
        .init(
            id: "HOSPITALITY_TOUR_GUIDES_ATTRACTIONS",
            group: .hospitalityAndTourism,
            displayName: "Tour guides & attractions",
            detail: "Trigger storytelling, history facts, or scavenger hunts."
        ),
        .init(
            id: "HOSPITALITY_MUSEUMS_GALLERIES",
            group: .hospitalityAndTourism,
            displayName: "Museums & galleries",
            detail: "Add interactive notes or trivia around exhibits."
        ),
        .init(
            id: "EVENTS_CONCERT_VENUES_THEATERS",
            group: .eventsAndEntertainment,
            displayName: "Concert venues & theaters",
            detail: "Reward ticket holders with backstage messages or merch coupons."
        ),
        .init(
            id: "EVENTS_FESTIVALS_FAIRS",
            group: .eventsAndEntertainment,
            displayName: "Festivals & fairs",
            detail: "Hide digital easter eggs at booths."
        ),
        .init(
            id: "EVENTS_SPORTS_ARENAS",
            group: .eventsAndEntertainment,
            displayName: "Sports arenas",
            detail: "Boost fan engagement with stats, trivia, or seat upgrades."
        ),
        .init(
            id: "COMMUNITY_SCHOOLS_UNIVERSITIES",
            group: .communityAndEducation,
            displayName: "Schools / universities",
            detail: "Share campus notes, event reminders, or safety alerts."
        ),
        .init(
            id: "COMMUNITY_NONPROFITS_CENTERS",
            group: .communityAndEducation,
            displayName: "Nonprofits & community centers",
            detail: "Highlight resources and promote upcoming events."
        ),
        .init(
            id: "COMMUNITY_LIBRARIES_CULTURE",
            group: .communityAndEducation,
            displayName: "Libraries / cultural centers",
            detail: "Deliver interactive learning drops."
        ),
        .init(
            id: "SERVICES_REAL_ESTATE_AGENTS",
            group: .realEstateAndLocalServices,
            displayName: "Real estate agents",
            detail: "Drop listing info, photos, and price sheets nearby."
        ),
        .init(
            id: "SERVICES_GYMS_FITNESS_STUDIOS",
            group: .realEstateAndLocalServices,
            displayName: "Gyms & fitness studios",
            detail: "Geofence promo trials around your building."
        ),
        .init(
            id: "SERVICES_LOCAL_PROVIDERS",
            group: .realEstateAndLocalServices,
            displayName: "Local service providers",
            detail: "Deliver instant coupons for walk-ins."
        ),
        .init(
            id: "TRAVEL_AIRPORTS_STATIONS",
            group: .travelAndTransportation,
            displayName: "Airports & train stations",
            detail: "Send real-time updates or lounge promotions."
        ),
        .init(
            id: "TRAVEL_RENTAL_CAR_SHUTTLES",
            group: .travelAndTransportation,
            displayName: "Rental car agencies / tour shuttles",
            detail: "Guide pickup and drop-off zones with geo-notes."
        ),
        .init(
            id: "TRAVEL_CRUISE_LINES_TOUR_BUSES",
            group: .travelAndTransportation,
            displayName: "Cruise lines & tour buses",
            detail: "Remind guests about location-based activities."
        ),
        .init(
            id: "MARKETING_GUERRILLA_BRANDS",
            group: .marketingAndCampaigns,
            displayName: "Brands doing guerrilla marketing",
            detail: "Hide promo codes around the city."
        ),
        .init(
            id: "MARKETING_INFLUENCERS_CREATORS",
            group: .marketingAndCampaigns,
            displayName: "Influencers / content creators",
            detail: "Leave geo-exclusive messages or scavenger hunts."
        ),
        .init(
            id: "MARKETING_LOCAL_GOV_TOURISM",
            group: .marketingAndCampaigns,
            displayName: "Local governments / tourism boards",
            detail: "Promote ‘Explore Our City’ challenges."
        )
    ]

    static func from(id: String?) -> BusinessCategory? {
        guard let raw = id?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty else { return nil }
        if let match = all.first(where: { $0.id.caseInsensitiveCompare(raw) == .orderedSame }) {
            return match
        }

        // Backwards compatibility for legacy identifiers used in earlier iOS builds
        let legacyMap: [String: String] = [
            "coffee": "FOOD_RESTAURANTS_CAFES",
            "restaurant": "FOOD_RESTAURANTS_CAFES",
            "bar": "FOOD_BARS_BREWERIES",
            "retail": "RETAIL_LOCAL_SHOPS",
            "fitness": "SERVICES_GYMS_FITNESS_STUDIOS",
            "arts": "COMMUNITY_LIBRARIES_CULTURE",
            "tour": "HOSPITALITY_TOUR_GUIDES_ATTRACTIONS",
            "other": "MARKETING_GUERRILLA_BRANDS"
        ]
        if let mapped = legacyMap[raw.lowercased()] {
            return all.first { $0.id == mapped }
        }
        return nil
    }
}
