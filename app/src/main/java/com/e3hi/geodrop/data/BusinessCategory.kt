package com.e3hi.geodrop.data

/**
 * Represents the supported business verticals that can onboard to GeoDrop.
 */
enum class BusinessCategoryGroup(val displayName: String) {
    FOOD_AND_BEVERAGE("Food & Beverage"),
    RETAIL_AND_SHOPPING("Retail & Shopping"),
    HOSPITALITY_AND_TOURISM("Hospitality & Tourism"),
    EVENTS_AND_ENTERTAINMENT("Events & Entertainment"),
    COMMUNITY_AND_EDUCATION("Community & Education"),
    REAL_ESTATE_AND_LOCAL_SERVICES("Real Estate & Local Services"),
    TRAVEL_AND_TRANSPORTATION("Travel & Transportation"),
    MARKETING_AND_CAMPAIGNS("Marketing & Engagement Campaigns")
}

/**
 * Individual business categories that appear within the onboarding picker.
 */
enum class BusinessCategory(
    val group: BusinessCategoryGroup,
    val displayName: String,
    val description: String
) {
    FOOD_RESTAURANTS_CAFES(
        BusinessCategoryGroup.FOOD_AND_BEVERAGE,
        "Restaurants / Cafés",
        "Drop coupons, daily specials, and loyalty rewards."
    ),
    FOOD_TRUCKS_STREET_VENDORS(
        BusinessCategoryGroup.FOOD_AND_BEVERAGE,
        "Food trucks & street vendors",
        "Notify explorers when you're nearby."
    ),
    FOOD_BARS_BREWERIES(
        BusinessCategoryGroup.FOOD_AND_BEVERAGE,
        "Bars & breweries",
        "Promote happy hour deals and live music nights."
    ),
    RETAIL_LOCAL_SHOPS(
        BusinessCategoryGroup.RETAIL_AND_SHOPPING,
        "Local shops & boutiques",
        "Offer geofenced discounts for in-store visits."
    ),
    RETAIL_MALLS_SHOPPING_CENTERS(
        BusinessCategoryGroup.RETAIL_AND_SHOPPING,
        "Malls / shopping centers",
        "Share event promotions, scavenger hunts, or coupon codes."
    ),
    RETAIL_POP_UPS_MARKETS(
        BusinessCategoryGroup.RETAIL_AND_SHOPPING,
        "Pop-up stores & markets",
        "Announce flash sales or limited stock."
    ),
    HOSPITALITY_HOTELS_RESORTS(
        BusinessCategoryGroup.HOSPITALITY_AND_TOURISM,
        "Hotels & resorts",
        "Send welcome notes, on-site reminders, or hidden perks."
    ),
    HOSPITALITY_TOUR_GUIDES_ATTRACTIONS(
        BusinessCategoryGroup.HOSPITALITY_AND_TOURISM,
        "Tour guides & attractions",
        "Trigger storytelling, history facts, or scavenger hunts."
    ),
    HOSPITALITY_MUSEUMS_GALLERIES(
        BusinessCategoryGroup.HOSPITALITY_AND_TOURISM,
        "Museums & galleries",
        "Add interactive notes or trivia around exhibits."
    ),
    EVENTS_CONCERT_VENUES_THEATERS(
        BusinessCategoryGroup.EVENTS_AND_ENTERTAINMENT,
        "Concert venues & theaters",
        "Reward ticket holders with backstage messages or merch coupons."
    ),
    EVENTS_FESTIVALS_FAIRS(
        BusinessCategoryGroup.EVENTS_AND_ENTERTAINMENT,
        "Festivals & fairs",
        "Hide digital easter eggs at booths."
    ),
    EVENTS_SPORTS_ARENAS(
        BusinessCategoryGroup.EVENTS_AND_ENTERTAINMENT,
        "Sports arenas",
        "Boost fan engagement with stats, trivia, or seat upgrades."
    ),
    COMMUNITY_SCHOOLS_UNIVERSITIES(
        BusinessCategoryGroup.COMMUNITY_AND_EDUCATION,
        "Schools / universities",
        "Share campus notes, event reminders, or safety alerts."
    ),
    COMMUNITY_NONPROFITS_CENTERS(
        BusinessCategoryGroup.COMMUNITY_AND_EDUCATION,
        "Nonprofits & community centers",
        "Highlight resources and promote upcoming events."
    ),
    COMMUNITY_LIBRARIES_CULTURE(
        BusinessCategoryGroup.COMMUNITY_AND_EDUCATION,
        "Libraries / cultural centers",
        "Deliver interactive learning drops."
    ),
    SERVICES_REAL_ESTATE_AGENTS(
        BusinessCategoryGroup.REAL_ESTATE_AND_LOCAL_SERVICES,
        "Real estate agents",
        "Drop listing info, photos, and price sheets nearby."
    ),
    SERVICES_GYMS_FITNESS_STUDIOS(
        BusinessCategoryGroup.REAL_ESTATE_AND_LOCAL_SERVICES,
        "Gyms & fitness studios",
        "Geofence promo trials around your building."
    ),
    SERVICES_LOCAL_PROVIDERS(
        BusinessCategoryGroup.REAL_ESTATE_AND_LOCAL_SERVICES,
        "Local service providers",
        "Deliver instant coupons for walk-ins."
    ),
    TRAVEL_AIRPORTS_STATIONS(
        BusinessCategoryGroup.TRAVEL_AND_TRANSPORTATION,
        "Airports & train stations",
        "Send real-time updates or lounge promotions."
    ),
    TRAVEL_RENTAL_CAR_SHUTTLES(
        BusinessCategoryGroup.TRAVEL_AND_TRANSPORTATION,
        "Rental car agencies / tour shuttles",
        "Guide pickup and drop-off zones with geo-notes."
    ),
    TRAVEL_CRUISE_LINES_TOUR_BUSES(
        BusinessCategoryGroup.TRAVEL_AND_TRANSPORTATION,
        "Cruise lines & tour buses",
        "Remind guests about location-based activities."
    ),
    MARKETING_GUERRILLA_BRANDS(
        BusinessCategoryGroup.MARKETING_AND_CAMPAIGNS,
        "Brands doing guerrilla marketing",
        "Hide promo codes around the city."
    ),
    MARKETING_INFLUENCERS_CREATORS(
        BusinessCategoryGroup.MARKETING_AND_CAMPAIGNS,
        "Influencers / content creators",
        "Leave geo-exclusive messages or scavenger hunts."
    ),
    MARKETING_LOCAL_GOV_TOURISM(
        BusinessCategoryGroup.MARKETING_AND_CAMPAIGNS,
        "Local governments / tourism boards",
        "Promote ‘Explore Our City’ challenges."
    );

    val id: String = name

    companion object {
        fun fromId(raw: String?): BusinessCategory? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull { it.id.equals(raw, ignoreCase = true) }
        }
    }
}