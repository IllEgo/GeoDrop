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
    struct GroupMetadata: Identifiable, Equatable, Hashable, Codable {
        let group: Group
        let title: String
        let description: String
        let categories: [BusinessCategory]

        var id: String { group.rawValue }
    }
    
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
    let description: String

    static let grouped: [GroupMetadata] = [
        GroupMetadata(
            group: .foodAndBeverage,
            title: Group.foodAndBeverage.displayName,
            description: "Restaurants, cafés, pop-ups, and beverage spots engaging hungry explorers.",
            categories: [
                BusinessCategory(
                    id: "FOOD_RESTAURANTS_CAFES",
                    group: .foodAndBeverage,
                    displayName: "Restaurants / Cafés",
                    description: "Drop coupons, daily specials, and loyalty rewards."
                ),
                BusinessCategory(
                    id: "FOOD_TRUCKS_STREET_VENDORS",
                    group: .foodAndBeverage,
                    displayName: "Food trucks & street vendors",
                    description: "Notify explorers when you're nearby."
                ),
                BusinessCategory(
                    id: "FOOD_BARS_BREWERIES",
                    group: .foodAndBeverage,
                    displayName: "Bars & breweries",
                    description: "Promote happy hour deals and live music nights."
                )
            ]
        ),
        GroupMetadata(
            group: .retailAndShopping,
            title: Group.retailAndShopping.displayName,
            description: "Shops and markets sharing offers, events, and product spotlights.",
            categories: [
                BusinessCategory(
                    id: "RETAIL_LOCAL_SHOPS",
                    group: .retailAndShopping,
                    displayName: "Local shops & boutiques",
                    description: "Offer geofenced discounts for in-store visits."
                ),
                BusinessCategory(
                    id: "RETAIL_MALLS_SHOPPING_CENTERS",
                    group: .retailAndShopping,
                    displayName: "Malls / shopping centers",
                    description: "Share event promotions, scavenger hunts, or coupon codes."
                ),
                BusinessCategory(
                    id: "RETAIL_POP_UPS_MARKETS",
                    group: .retailAndShopping,
                    displayName: "Pop-up stores & markets",
                    description: "Announce flash sales or limited stock."
                )
            ]
        ),
        GroupMetadata(
            group: .hospitalityAndTourism,
            title: Group.hospitalityAndTourism.displayName,
            description: "Hotels, attractions, and museums guiding guests through memorable stays.",
            categories: [
                BusinessCategory(
                    id: "HOSPITALITY_HOTELS_RESORTS",
                    group: .hospitalityAndTourism,
                    displayName: "Hotels & resorts",
                    description: "Send welcome notes, on-site reminders, or hidden perks."
                ),
                BusinessCategory(
                    id: "HOSPITALITY_TOUR_GUIDES_ATTRACTIONS",
                    group: .hospitalityAndTourism,
                    displayName: "Tour guides & attractions",
                    description: "Trigger storytelling, history facts, or scavenger hunts."
                ),
                BusinessCategory(
                    id: "HOSPITALITY_MUSEUMS_GALLERIES",
                    group: .hospitalityAndTourism,
                    displayName: "Museums & galleries",
                    description: "Add interactive notes or trivia around exhibits."
                )
            ]
        ),
        GroupMetadata(
            group: .eventsAndEntertainment,
            title: Group.eventsAndEntertainment.displayName,
            description: "Venues and festivals energizing audiences before, during, and after events.",
            categories: [
                BusinessCategory(
                    id: "EVENTS_CONCERT_VENUES_THEATERS",
                    group: .eventsAndEntertainment,
                    displayName: "Concert venues & theaters",
                    description: "Reward ticket holders with backstage messages or merch coupons."
                ),
                BusinessCategory(
                    id: "EVENTS_FESTIVALS_FAIRS",
                    group: .eventsAndEntertainment,
                    displayName: "Festivals & fairs",
                    description: "Hide digital easter eggs at booths."
                ),
                BusinessCategory(
                    id: "EVENTS_SPORTS_ARENAS",
                    group: .eventsAndEntertainment,
                    displayName: "Sports arenas",
                    description: "Boost fan engagement with stats, trivia, or seat upgrades."
                )
            ]
        ),
        GroupMetadata(
            group: .communityAndEducation,
            title: Group.communityAndEducation.displayName,
            description: "Schools, nonprofits, and libraries sharing resources with neighbors.",
            categories: [
                BusinessCategory(
                    id: "COMMUNITY_SCHOOLS_UNIVERSITIES",
                    group: .communityAndEducation,
                    displayName: "Schools / universities",
                    description: "Share campus notes, event reminders, or safety alerts."
                ),
                BusinessCategory(
                    id: "COMMUNITY_NONPROFITS_CENTERS",
                    group: .communityAndEducation,
                    displayName: "Nonprofits & community centers",
                    description: "Highlight resources and promote upcoming events."
                ),
                BusinessCategory(
                    id: "COMMUNITY_LIBRARIES_CULTURE",
                    group: .communityAndEducation,
                    displayName: "Libraries / cultural centers",
                    description: "Deliver interactive learning drops."
                )
            ]
        ),
        GroupMetadata(
            group: .realEstateAndLocalServices,
            title: Group.realEstateAndLocalServices.displayName,
            description: "Service providers and real estate pros helping residents take action.",
            categories: [
                BusinessCategory(
                    id: "SERVICES_REAL_ESTATE_AGENTS",
                    group: .realEstateAndLocalServices,
                    displayName: "Real estate agents",
                    description: "Drop listing info, photos, and price sheets nearby."
                ),
                BusinessCategory(
                    id: "SERVICES_GYMS_FITNESS_STUDIOS",
                    group: .realEstateAndLocalServices,
                    displayName: "Gyms & fitness studios",
                    description: "Geofence promo trials around your building."
                ),
                BusinessCategory(
                    id: "SERVICES_LOCAL_PROVIDERS",
                    group: .realEstateAndLocalServices,
                    displayName: "Local service providers",
                    description: "Deliver instant coupons for walk-ins."
                )
            ]
        )
        GroupMetadata(
            group: .travelAndTransportation,
            title: Group.travelAndTransportation.displayName,
            description: "Transit hubs and travel operators keeping passengers moving.",
            categories: [
                BusinessCategory(
                    id: "TRAVEL_AIRPORTS_STATIONS",
                    group: .travelAndTransportation,
                    displayName: "Airports & train stations",
                    description: "Send real-time updates or lounge promotions."
                ),
                BusinessCategory(
                    id: "TRAVEL_RENTAL_CAR_SHUTTLES",
                    group: .travelAndTransportation,
                    displayName: "Rental car agencies / tour shuttles",
                    description: "Guide pickup and drop-off zones with geo-notes."
                ),
                BusinessCategory(
                    id: "TRAVEL_CRUISE_LINES_TOUR_BUSES",
                    group: .travelAndTransportation,
                    displayName: "Cruise lines & tour buses",
                    description: "Remind guests about location-based activities."
                )
            ]
        ),
        GroupMetadata(
            group: .marketingAndCampaigns,
            title: Group.marketingAndCampaigns.displayName,
            description: "Campaigns and creators launching playful scavenger hunts and promos.",
            categories: [
                BusinessCategory(
                    id: "MARKETING_GUERRILLA_BRANDS",
                    group: .marketingAndCampaigns,
                    displayName: "Brands doing guerrilla marketing",
                    description: "Hide promo codes around the city."
                ),
                BusinessCategory(
                    id: "MARKETING_INFLUENCERS_CREATORS",
                    group: .marketingAndCampaigns,
                    displayName: "Influencers / content creators",
                    description: "Leave geo-exclusive messages or scavenger hunts."
                ),
                BusinessCategory(
                    id: "MARKETING_LOCAL_GOV_TOURISM",
                    group: .marketingAndCampaigns,
                    displayName: "Local governments / tourism boards",
                    description: "Promote ‘Explore Our City’ challenges."
                )
            ]
        )
    ]
    
    static var all: [BusinessCategory] {
        grouped.flatMap { $0.categories }
    }

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
