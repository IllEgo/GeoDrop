package com.e3hi.geodrop.data

/**
 * Structured templates that help businesses quickly craft drops tailored to their category.
 */
data class BusinessDropTemplate(
    val id: String,
    val category: BusinessCategory,
    val title: String,
    val description: String,
    val dropType: DropType,
    val contentType: DropContentType = DropContentType.TEXT,
    val note: String,
    val caption: String,
    val callToAction: String? = null
)

data class BusinessDropTypeOptionCopy(
    val type: DropType,
    val title: String,
    val description: String
)

private val GENERAL_BUSINESS_TEMPLATES = listOf(
    BusinessDropTemplate(
        id = "general_welcome_note",
        category = BusinessCategory.FOOD_RESTAURANTS_CAFES,
        title = "Welcome explorers",
        description = "Set the tone for anyone discovering your business for the first time.",
        dropType = DropType.COMMUNITY,
        contentType = DropContentType.TEXT,
        note = "Welcome to our spot! Show this message at the counter for a warm hello and let us know what brings you in today.",
        caption = "A warm hello from our team.",
        callToAction = "Personalize this greeting with your brand voice."
    ),
    BusinessDropTemplate(
        id = "general_loyalty_coupon",
        category = BusinessCategory.RETAIL_LOCAL_SHOPS,
        title = "Loyalty reward",
        description = "Encourage repeat visits with a simple redemption code.",
        dropType = DropType.RESTAURANT_COUPON,
        contentType = DropContentType.TEXT,
        note = "Thanks for stopping by! Mention code LOCALLOVE at checkout to unlock a loyalty perk just for GeoDrop explorers.",
        caption = "Unlock a perk just for GeoDrop fans.",
        callToAction = "Update the code and perk to match your offer."
    ),
    BusinessDropTemplate(
        id = "general_behind_scenes",
        category = BusinessCategory.MARKETING_INFLUENCERS_CREATORS,
        title = "Behind-the-scenes moment",
        description = "Capture a quick video or audio snippet that feels exclusive.",
        dropType = DropType.COMMUNITY,
        contentType = DropContentType.VIDEO,
        note = "You're seeing it first! Record a quick clip sharing what we're working on right now and invite explorers to follow along.",
        caption = "Exclusive peek at what we're creating.",
        callToAction = "Record a fresh clip before publishing."
    )
)

private val BUSINESS_DROP_TEMPLATES: Map<BusinessCategory, List<BusinessDropTemplate>> = mapOf(
    BusinessCategory.FOOD_RESTAURANTS_CAFES to listOf(
        BusinessDropTemplate(
            id = "restaurants_morning_special",
            category = BusinessCategory.FOOD_RESTAURANTS_CAFES,
            title = "Morning special",
            description = "Highlight a limited-time breakfast or coffee offer.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Rise and shine! Flash code MORNINGBREW before 11am for 20% off any pastry + coffee combo today only.",
            caption = "Start your day with our morning treat.",
            callToAction = "Adjust the code and time window before posting."
        ),
        BusinessDropTemplate(
            id = "restaurants_chef_story",
            category = BusinessCategory.FOOD_RESTAURANTS_CAFES,
            title = "Chef's table note",
            description = "Share a short story about today's menu.",
            dropType = DropType.COMMUNITY,
            note = "Chef's highlight: Our seasonal soup is simmering with roasted squash and sage. Ask about the story behind the recipe!",
            caption = "Today's menu story from the chef."
        )
    ),
    BusinessCategory.FOOD_TRUCKS_STREET_VENDORS to listOf(
        BusinessDropTemplate(
            id = "foodtruck_location_ping",
            category = BusinessCategory.FOOD_TRUCKS_STREET_VENDORS,
            title = "Today's parking spot",
            description = "Let fans know exactly where to find you.",
            dropType = DropType.COMMUNITY,
            note = "We rolled up at the corner of 5th & Oak until 2pm. Show this drop for a free topping upgrade!",
            caption = "Find us parked nearby today."
        ),
        BusinessDropTemplate(
            id = "foodtruck_secret_code",
            category = BusinessCategory.FOOD_TRUCKS_STREET_VENDORS,
            title = "Secret menu bite",
            description = "Reward people who tracked you down.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Ask for the 'GeoDrop bite' and say code STREETFIND for an off-menu sampler while supplies last.",
            caption = "Ask for the GeoDrop bite."
        )
    ),
    BusinessCategory.FOOD_BARS_BREWERIES to listOf(
        BusinessDropTemplate(
            id = "bar_happy_hour",
            category = BusinessCategory.FOOD_BARS_BREWERIES,
            title = "Happy hour cheers",
            description = "Promote a timed deal on drinks or bites.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Happy hour just for you! Mention code GEOHOPS between 4–6pm for $2 off our featured pint.",
            caption = "Cheers to exclusive happy hour savings."
        ),
        BusinessDropTemplate(
            id = "bar_live_music",
            category = BusinessCategory.FOOD_BARS_BREWERIES,
            title = "Live music lineup",
            description = "Tease tonight's entertainment.",
            dropType = DropType.COMMUNITY,
            note = "Tonight at 8pm: The Riverside Duo takes the stage. Drop by early for the best seats and a sneak peek at the setlist.",
            caption = "Catch tonight's live vibes."
        )
    ),
    BusinessCategory.RETAIL_LOCAL_SHOPS to listOf(
        BusinessDropTemplate(
            id = "retail_flash_sale",
            category = BusinessCategory.RETAIL_LOCAL_SHOPS,
            title = "Flash sale",
            description = "Drive immediate foot traffic with a surprise discount.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "GeoDrop exclusive! Say code SHOPLOCAL for 15% off any two items before close tonight.",
            caption = "Snag the flash sale before it's gone."
        ),
        BusinessDropTemplate(
            id = "retail_story",
            category = BusinessCategory.RETAIL_LOCAL_SHOPS,
            title = "Maker spotlight",
            description = "Share the story behind a featured product.",
            dropType = DropType.COMMUNITY,
            note = "Meet the maker: This week's featured artisan is River & Clay. Ask us to show you how they craft each piece.",
            caption = "Hear the maker's story up close."
        )
    ),
    BusinessCategory.RETAIL_MALLS_SHOPPING_CENTERS to listOf(
        BusinessDropTemplate(
            id = "mall_event_pass",
            category = BusinessCategory.RETAIL_MALLS_SHOPPING_CENTERS,
            title = "Event passport",
            description = "Encourage visitors to explore multiple stops.",
            dropType = DropType.TOUR_STOP,
            note = "Stamp stop 3 of our Shop & Win trail! Collect all five codes from participating stores and redeem at guest services.",
            caption = "Collect your next event passport stamp."
        ),
        BusinessDropTemplate(
            id = "mall_coupon",
            category = BusinessCategory.RETAIL_MALLS_SHOPPING_CENTERS,
            title = "Center-wide perk",
            description = "Offer a perk funded by participating tenants.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Show this at the info desk to claim a surprise from our weekend pop-ups. Mention code MALLMOMENT.",
            caption = "Claim a center-wide surprise perk."
        )
    ),
    BusinessCategory.RETAIL_POP_UPS_MARKETS to listOf(
        BusinessDropTemplate(
            id = "popup_drop",
            category = BusinessCategory.RETAIL_POP_UPS_MARKETS,
            title = "Limited release alert",
            description = "Build urgency for limited inventory.",
            dropType = DropType.COMMUNITY,
            note = "Only 30 jars of today's small-batch jam! Tell us you found the GeoDrop to reserve one while you're here.",
            caption = "Limited jars—grab yours fast."
        ),
        BusinessDropTemplate(
            id = "popup_loyalty",
            category = BusinessCategory.RETAIL_POP_UPS_MARKETS,
            title = "Market loyalty stamp",
            description = "Reward explorers who visit multiple stalls.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Collect three GeoDrop codes around the market and say MARKETTRAIL here for a bundle discount.",
            caption = "Earn your market loyalty reward."
        )
    ),
    BusinessCategory.HOSPITALITY_HOTELS_RESORTS to listOf(
        BusinessDropTemplate(
            id = "hotel_welcome",
            category = BusinessCategory.HOSPITALITY_HOTELS_RESORTS,
            title = "Guest welcome",
            description = "Greet arrivals with a concierge note.",
            dropType = DropType.COMMUNITY,
            note = "Welcome to your stay! Stop by the front desk and mention SUNRISE to unlock a late checkout upgrade (based on availability).",
            caption = "Enjoy a warm welcome upgrade."
        ),
        BusinessDropTemplate(
            id = "hotel_pool_club",
            category = BusinessCategory.HOSPITALITY_HOTELS_RESORTS,
            title = "Poolside perk",
            description = "Encourage on-property exploration.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Head to the rooftop pool bar and use code SPLASH for a complimentary mocktail this afternoon.",
            caption = "Splash into a rooftop perk."
        )
    ),
    BusinessCategory.HOSPITALITY_TOUR_GUIDES_ATTRACTIONS to listOf(
        BusinessDropTemplate(
            id = "tour_story",
            category = BusinessCategory.HOSPITALITY_TOUR_GUIDES_ATTRACTIONS,
            title = "Guided story stop",
            description = "Share narration when visitors arrive at a landmark.",
            dropType = DropType.TOUR_STOP,
            note = "You're standing where the city was founded. Tap play on our audio guide or ask your guide for the 'Origins' story.",
            caption = "Listen to the origin story here."
        ),
        BusinessDropTemplate(
            id = "tour_next_clue",
            category = BusinessCategory.HOSPITALITY_TOUR_GUIDES_ATTRACTIONS,
            title = "Scavenger clue",
            description = "Keep groups moving to the next point of interest.",
            dropType = DropType.TOUR_STOP,
            note = "Clue unlocked! Count the arches above you and bring the number to the fountain on Market Street for the next tale.",
            caption = "Follow this clue to your next stop."
        )
    ),
    BusinessCategory.HOSPITALITY_MUSEUMS_GALLERIES to listOf(
        BusinessDropTemplate(
            id = "museum_fact",
            category = BusinessCategory.HOSPITALITY_MUSEUMS_GALLERIES,
            title = "Behind-the-art fact",
            description = "Reveal context that isn't on the placard.",
            dropType = DropType.TOUR_STOP,
            note = "Look closer: this painting hides a message from the artist's mentor. Ask a docent about the signature in the corner.",
            caption = "Discover the art secret in front of you."
        ),
        BusinessDropTemplate(
            id = "museum_audio",
            category = BusinessCategory.HOSPITALITY_MUSEUMS_GALLERIES,
            title = "Curator voice note",
            description = "Record a quick audio insight to accompany the exhibit.",
            dropType = DropType.TOUR_STOP,
            contentType = DropContentType.AUDIO,
            note = "Record a 60-second curator tip highlighting what makes this piece significant to our collection.",
            caption = "Press play for a curator insight."
        )
    ),
    BusinessCategory.EVENTS_CONCERT_VENUES_THEATERS to listOf(
        BusinessDropTemplate(
            id = "venue_setlist",
            category = BusinessCategory.EVENTS_CONCERT_VENUES_THEATERS,
            title = "Setlist teaser",
            description = "Get fans excited before showtime.",
            dropType = DropType.COMMUNITY,
            note = "Tonight's secret song? Check the merch table and whisper ENCORE to learn the surprise closer.",
            caption = "Get the inside scoop on tonight's set."
        ),
        BusinessDropTemplate(
            id = "venue_merch",
            category = BusinessCategory.EVENTS_CONCERT_VENUES_THEATERS,
            title = "Merch booth perk",
            description = "Drive sales during intermission.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Flash this drop at the lobby merch booth and use code BACKSTAGE for 10% off one item tonight only.",
            caption = "Flash this for a merch perk."
        )
    ),
    BusinessCategory.EVENTS_FESTIVALS_FAIRS to listOf(
        BusinessDropTemplate(
            id = "festival_clue",
            category = BusinessCategory.EVENTS_FESTIVALS_FAIRS,
            title = "Hidden booth clue",
            description = "Encourage exploration across the grounds.",
            dropType = DropType.COMMUNITY,
            note = "Seek out the booth with the sunflower flag. Tell them BLOOM to grab a festival-exclusive sticker.",
            caption = "Hunt down the hidden sunflower booth."
        ),
        BusinessDropTemplate(
            id = "festival_map",
            category = BusinessCategory.EVENTS_FESTIVALS_FAIRS,
            title = "Trail highlight",
            description = "Guide attendees toward the next activity.",
            dropType = DropType.TOUR_STOP,
            note = "You're on the art walk! Follow the lanterns to the community mural wall for live painting demos.",
            caption = "Follow the lanterns to the next activity."
        )
    ),
    BusinessCategory.EVENTS_SPORTS_ARENAS to listOf(
        BusinessDropTemplate(
            id = "arena_upgrade",
            category = BusinessCategory.EVENTS_SPORTS_ARENAS,
            title = "Seat upgrade lottery",
            description = "Surprise lucky fans during the game.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "You've found the hidden drop! Visit section 104 and mention code FANUPGRADE for a chance at better seats.",
            caption = "Score a shot at better seats."
        ),
        BusinessDropTemplate(
            id = "arena_trivia",
            category = BusinessCategory.EVENTS_SPORTS_ARENAS,
            title = "Team trivia",
            description = "Boost engagement during timeouts.",
            dropType = DropType.COMMUNITY,
            note = "Trivia time: Who holds the arena scoring record? Tell guest services your answer to enter our merch raffle.",
            caption = "Test your team trivia knowledge."
        )
    ),
    BusinessCategory.COMMUNITY_SCHOOLS_UNIVERSITIES to listOf(
        BusinessDropTemplate(
            id = "school_orientation",
            category = BusinessCategory.COMMUNITY_SCHOOLS_UNIVERSITIES,
            title = "Orientation stop",
            description = "Support campus tours with additional info.",
            dropType = DropType.TOUR_STOP,
            note = "Welcome to the student union! Find the mural inside and snap a photo to collect points for your orientation scavenger hunt.",
            caption = "Welcome stop on your campus trail."
        ),
        BusinessDropTemplate(
            id = "school_alert",
            category = BusinessCategory.COMMUNITY_SCHOOLS_UNIVERSITIES,
            title = "Event reminder",
            description = "Promote upcoming meetings or safety info.",
            dropType = DropType.COMMUNITY,
            note = "Campus update: Free tutoring tonight in Room 210 from 6–8pm. Share this drop with a friend who needs a boost.",
            caption = "Don't miss tonight's campus event."
        )
    ),
    BusinessCategory.COMMUNITY_NONPROFITS_CENTERS to listOf(
        BusinessDropTemplate(
            id = "nonprofit_volunteer",
            category = BusinessCategory.COMMUNITY_NONPROFITS_CENTERS,
            title = "Volunteer call",
            description = "Invite neighbors to lend a hand.",
            dropType = DropType.COMMUNITY,
            note = "We're preparing weekend care kits. Drop in Saturday at 10am and mention 'GeoDrop helpers' to join the volunteer crew.",
            caption = "Join our weekend volunteer crew."
        ),
        BusinessDropTemplate(
            id = "nonprofit_resources",
            category = BusinessCategory.COMMUNITY_NONPROFITS_CENTERS,
            title = "Resource spotlight",
            description = "Share how to access key services.",
            dropType = DropType.COMMUNITY,
            note = "Need support? This week we're offering free career coaching. Visit the front desk and reference code CONNECT for details.",
            caption = "Ask about this week's free resources."
        )
    ),
    BusinessCategory.COMMUNITY_LIBRARIES_CULTURE to listOf(
        BusinessDropTemplate(
            id = "library_storytime",
            category = BusinessCategory.COMMUNITY_LIBRARIES_CULTURE,
            title = "Storytime reminder",
            description = "Promote programming across the space.",
            dropType = DropType.COMMUNITY,
            note = "Storytime starts at 11am downstairs. Whisper BOOKWORM at the desk for a themed bookmark while supplies last.",
            caption = "Storytime kicks off downstairs soon."
        ),
        BusinessDropTemplate(
            id = "library_readinglist",
            category = BusinessCategory.COMMUNITY_LIBRARIES_CULTURE,
            title = "Reading list",
            description = "Recommend related titles for explorers to check out.",
            dropType = DropType.COMMUNITY,
            note = "Love this exhibit? Ask a librarian for the GeoDrop reading list featuring three must-read picks connected to it.",
            caption = "Grab our curated reading list."
        )
    ),
    BusinessCategory.SERVICES_REAL_ESTATE_AGENTS to listOf(
        BusinessDropTemplate(
            id = "realestate_openhouse",
            category = BusinessCategory.SERVICES_REAL_ESTATE_AGENTS,
            title = "Open house stop",
            description = "Guide self-led tours through listings.",
            dropType = DropType.TOUR_STOP,
            note = "Welcome in! Start in the kitchen and look for the chalkboard for renovation notes. Scan each room's drop for extra insights.",
            caption = "Start your self-guided tour here."
        ),
        BusinessDropTemplate(
            id = "realestate_feature",
            category = BusinessCategory.SERVICES_REAL_ESTATE_AGENTS,
            title = "Feature focus",
            description = "Highlight a hidden gem in the property.",
            dropType = DropType.COMMUNITY,
            note = "Don't miss the backyard studio—it's wired and climate-controlled. Ask about converting it into your dream workspace.",
            caption = "Peek at this property's hidden gem."
        )
    ),
    BusinessCategory.SERVICES_GYMS_FITNESS_STUDIOS to listOf(
        BusinessDropTemplate(
            id = "gym_trial",
            category = BusinessCategory.SERVICES_GYMS_FITNESS_STUDIOS,
            title = "Free class pass",
            description = "Invite explorers to try a session.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Ready to sweat? Mention code TRYFIT for a complimentary drop-in class this week.",
            caption = "Try a free class on us."
        ),
        BusinessDropTemplate(
            id = "gym_challenge",
            category = BusinessCategory.SERVICES_GYMS_FITNESS_STUDIOS,
            title = "Challenge of the day",
            description = "Gamify the visit with a quick task.",
            dropType = DropType.COMMUNITY,
            note = "Today's challenge: 20 wall balls + 10 burpees. Post a pic and tag us to enter our monthly gear giveaway.",
            caption = "Take on today's fitness challenge."
        )
    ),
    BusinessCategory.SERVICES_LOCAL_PROVIDERS to listOf(
        BusinessDropTemplate(
            id = "services_walkin",
            category = BusinessCategory.SERVICES_LOCAL_PROVIDERS,
            title = "Walk-in special",
            description = "Encourage immediate bookings.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Drop-in deal: Mention code QUICKFIX for 10% off same-day appointments.",
            caption = "Walk in now for a same-day perk."
        ),
        BusinessDropTemplate(
            id = "services_tip",
            category = BusinessCategory.SERVICES_LOCAL_PROVIDERS,
            title = "Pro tip",
            description = "Share expertise to build trust.",
            dropType = DropType.COMMUNITY,
            note = "Pro tip from our team: Replace your filters every 60 days. Show us this note for a free reminder sticker.",
            caption = "Grab a quick pro tip from our team."
        )
    ),
    BusinessCategory.TRAVEL_AIRPORTS_STATIONS to listOf(
        BusinessDropTemplate(
            id = "airport_gate_update",
            category = BusinessCategory.TRAVEL_AIRPORTS_STATIONS,
            title = "Gate-side update",
            description = "Share timely travel information.",
            dropType = DropType.COMMUNITY,
            note = "Flight updates: Announcements post here first. Check in with our desk and mention code TRAVELSMART for lounge tips.",
            caption = "Stay in the know before you board."
        ),
        BusinessDropTemplate(
            id = "airport_lounge",
            category = BusinessCategory.TRAVEL_AIRPORTS_STATIONS,
            title = "Lounge invitation",
            description = "Promote premium experiences.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Relax before you fly. Present code RUNWAYREST at the lounge door for a complimentary drink upgrade.",
            caption = "Upgrade your wait with lounge perks."
        )
    ),
    BusinessCategory.TRAVEL_RENTAL_CAR_SHUTTLES to listOf(
        BusinessDropTemplate(
            id = "rental_pickup",
            category = BusinessCategory.TRAVEL_RENTAL_CAR_SHUTTLES,
            title = "Pickup instructions",
            description = "Give clear steps when travelers arrive.",
            dropType = DropType.TOUR_STOP,
            note = "Welcome to pickup zone B. Text us your stall number when parked and we'll pull your car around in minutes.",
            caption = "Follow these easy pickup steps."
        ),
        BusinessDropTemplate(
            id = "rental_return",
            category = BusinessCategory.TRAVEL_RENTAL_CAR_SHUTTLES,
            title = "Return reminder",
            description = "Help guests wrap up their rental with ease.",
            dropType = DropType.COMMUNITY,
            note = "Returning tonight? Mention code FUELCHECK and we'll top off the tank for you at our discounted rate.",
            caption = "Ask about a seamless return perk."
        )
    ),
    BusinessCategory.TRAVEL_CRUISE_LINES_TOUR_BUSES to listOf(
        BusinessDropTemplate(
            id = "cruise_daily",
            category = BusinessCategory.TRAVEL_CRUISE_LINES_TOUR_BUSES,
            title = "Daily schedule",
            description = "Share what's happening right now onboard.",
            dropType = DropType.COMMUNITY,
            note = "Today's onboard highlight: Sunset deck yoga at 6pm. Check in with the host and say code SEAFOCUS for a reserved mat.",
            caption = "Join today's can't-miss onboard event."
        ),
        BusinessDropTemplate(
            id = "cruise_port_tip",
            category = BusinessCategory.TRAVEL_CRUISE_LINES_TOUR_BUSES,
            title = "Port insider tip",
            description = "Offer guidance before guests disembark.",
            dropType = DropType.TOUR_STOP,
            note = "Next port secret: Take the blue tram to the local market and look for the coral arch—our partner shop has samples waiting.",
            caption = "Get the insider port game plan."
        )
    ),
    BusinessCategory.MARKETING_GUERRILLA_BRANDS to listOf(
        BusinessDropTemplate(
            id = "guerrilla_secret_code",
            category = BusinessCategory.MARKETING_GUERRILLA_BRANDS,
            title = "Secret code drop",
            description = "Hide a reward in plain sight.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "You've cracked the mission. Whisper CODEWORD IMPACT at our next pop-up for limited swag.",
            caption = "Whisper the mission code for swag."
        ),
        BusinessDropTemplate(
            id = "guerrilla_clue",
            category = BusinessCategory.MARKETING_GUERRILLA_BRANDS,
            title = "Next clue",
            description = "Guide fans to the next experience.",
            dropType = DropType.COMMUNITY,
            note = "The trail continues near the mural with neon wings. Scan for our next drop to unlock what's coming.",
            caption = "Track the next clue on the trail."
        )
    ),
    BusinessCategory.MARKETING_INFLUENCERS_CREATORS to listOf(
        BusinessDropTemplate(
            id = "creator_meetup",
            category = BusinessCategory.MARKETING_INFLUENCERS_CREATORS,
            title = "Pop-up meetup",
            description = "Invite followers to a quick hangout.",
            dropType = DropType.COMMUNITY,
            contentType = DropContentType.VIDEO,
            note = "Record a clip inviting explorers to meet you here at 5pm for a selfie session and surprise shout-outs.",
            caption = "Meet me IRL for selfies later."
        ),
        BusinessDropTemplate(
            id = "creator_code",
            category = BusinessCategory.MARKETING_INFLUENCERS_CREATORS,
            title = "Exclusive code",
            description = "Reward loyal fans with a limited perk.",
            dropType = DropType.RESTAURANT_COUPON,
            note = "Say code CREWLOVE at checkout for a fan-only discount on today's merch drop.",
            caption = "Use the fan code for a drop deal."
        )
    ),
    BusinessCategory.MARKETING_LOCAL_GOV_TOURISM to listOf(
        BusinessDropTemplate(
            id = "tourism_challenge",
            category = BusinessCategory.MARKETING_LOCAL_GOV_TOURISM,
            title = "Explore challenge",
            description = "Encourage residents to discover hidden gems.",
            dropType = DropType.TOUR_STOP,
            note = "City quest stop! Snap a photo with the sculpture and tag #GeoDropCity for a chance to win local gift cards.",
            caption = "Take the city quest challenge here."
        ),
        BusinessDropTemplate(
            id = "tourism_event",
            category = BusinessCategory.MARKETING_LOCAL_GOV_TOURISM,
            title = "Event spotlight",
            description = "Promote upcoming civic programming.",
            dropType = DropType.COMMUNITY,
            note = "Don't miss Friday's night market on Main Street. Mention code HELLOCITY at the welcome tent for a visitor guide.",
            caption = "Make plans for Friday's night market."
        )
    )
)

fun dropTemplatesFor(categories: Collection<BusinessCategory>): List<BusinessDropTemplate> {
    if (categories.isEmpty()) {
        return GENERAL_BUSINESS_TEMPLATES
    }

    val ordered = LinkedHashMap<String, BusinessDropTemplate>()
    categories.forEach { category ->
        BUSINESS_DROP_TEMPLATES[category].orEmpty().forEach { template ->
            ordered.putIfAbsent(template.id, template)
        }
    }

    if (ordered.size < GENERAL_BUSINESS_TEMPLATES.size) {
        GENERAL_BUSINESS_TEMPLATES.forEach { template ->
            ordered.putIfAbsent(template.id, template)
        }
    }

    return ordered.values.toList()
}

private data class DropTypeCopy(
    val title: String,
    val description: String
)

private val DEFAULT_DROP_TYPE_COPIES: Map<DropType, DropTypeCopy> = mapOf(
    DropType.COMMUNITY to DropTypeCopy(
        title = "Community",
        description = "Share timely stories, tips, or updates for explorers nearby."
    ),
    DropType.RESTAURANT_COUPON to DropTypeCopy(
        title = "Offer",
        description = "Provide a code or perk that visitors can redeem in person."
    ),
    DropType.TOUR_STOP to DropTypeCopy(
        title = "Tour stop",
        description = "Guide explorers with narrative cues, directions, or media prompts."
    )
)

private val DROP_TYPE_COPY_OVERRIDES: Map<BusinessCategoryGroup, Map<DropType, DropTypeCopy>> = mapOf(
    BusinessCategoryGroup.FOOD_AND_BEVERAGE to mapOf(
        DropType.RESTAURANT_COUPON to DropTypeCopy(
            title = "Offer or BOGO",
            description = "Delight guests with coupons, loyalty rewards, or BOGO bites they can redeem on the spot."
        ),
        DropType.COMMUNITY to DropTypeCopy(
            title = "Menu moment",
            description = "Share chef notes, daily specials, or behind-the-counter stories to keep foodies in the know."
        )
    ),
    BusinessCategoryGroup.RETAIL_AND_SHOPPING to mapOf(
        DropType.RESTAURANT_COUPON to DropTypeCopy(
            title = "In-store perk",
            description = "Drop promo codes, bundle deals, or limited gifts that motivate shoppers to check out now."
        ),
        DropType.COMMUNITY to DropTypeCopy(
            title = "Product spotlight",
            description = "Highlight new arrivals, maker stories, or market-day news right where customers browse."
        ),
        DropType.TOUR_STOP to DropTypeCopy(
            title = "Shopping trail stop",
            description = "Guide explorers between booths or boutiques with scavenger-hunt style instructions."
        )
    ),
    BusinessCategoryGroup.HOSPITALITY_AND_TOURISM to mapOf(
        DropType.TOUR_STOP to DropTypeCopy(
            title = "Guided stop",
            description = "Lead guests through each waypoint with audio, video, or step-by-step storytelling."
        ),
        DropType.COMMUNITY to DropTypeCopy(
            title = "Guest welcome",
            description = "Deliver concierge-style notes, reminders, or surprise perks during their stay."
        ),
        DropType.RESTAURANT_COUPON to DropTypeCopy(
            title = "On-site perk",
            description = "Unlock stay enhancements or limited-time offers reserved for guests in range."
        )
    ),
    BusinessCategoryGroup.EVENTS_AND_ENTERTAINMENT to mapOf(
        DropType.COMMUNITY to DropTypeCopy(
            title = "Event hype",
            description = "Share setlist teasers, trivia, or live updates that energize the crowd."
        ),
        DropType.RESTAURANT_COUPON to DropTypeCopy(
            title = "Merch or concession perk",
            description = "Reward attendees with discounts or upgrades they can redeem between acts."
        ),
        DropType.TOUR_STOP to DropTypeCopy(
            title = "Venue trail stop",
            description = "Direct guests around the venue with guided checkpoints or scavenger clues."
        )
    ),
    BusinessCategoryGroup.COMMUNITY_AND_EDUCATION to mapOf(
        DropType.COMMUNITY to DropTypeCopy(
            title = "Community update",
            description = "Promote resources, reminders, or program highlights for neighbors and students."
        ),
        DropType.TOUR_STOP to DropTypeCopy(
            title = "Self-guided tour",
            description = "Lead visitors across campus or exhibits with prompts and reflection questions."
        )
    ),
    BusinessCategoryGroup.REAL_ESTATE_AND_LOCAL_SERVICES to mapOf(
        DropType.TOUR_STOP to DropTypeCopy(
            title = "Self-guided showing",
            description = "Walk prospects through key locations with staged talking points and media."
        ),
        DropType.COMMUNITY to DropTypeCopy(
            title = "Expert tip",
            description = "Share advice, maintenance reminders, or service spotlights to build trust."
        ),
        DropType.RESTAURANT_COUPON to DropTypeCopy(
            title = "Intro offer",
            description = "Drop first-visit incentives or referral rewards to convert curious passersby."
        )
    ),
    BusinessCategoryGroup.TRAVEL_AND_TRANSPORTATION to mapOf(
        DropType.COMMUNITY to DropTypeCopy(
            title = "Travel update",
            description = "Broadcast gate changes, timing alerts, or journey tips exactly where travelers need them."
        ),
        DropType.RESTAURANT_COUPON to DropTypeCopy(
            title = "Lounge perk",
            description = "Share lounge invites, upgrades, or amenity codes for folks already on-site."
        ),
        DropType.TOUR_STOP to DropTypeCopy(
            title = "Wayfinding stop",
            description = "Guide riders through pickup points or next steps with clear navigation prompts."
        )
    ),
    BusinessCategoryGroup.MARKETING_AND_CAMPAIGNS to mapOf(
        DropType.COMMUNITY to DropTypeCopy(
            title = "Campaign clue",
            description = "Reveal storytelling beats, puzzles, or mission updates to keep fans engaged."
        ),
        DropType.RESTAURANT_COUPON to DropTypeCopy(
            title = "Unlockable reward",
            description = "Hide promo codes or swag unlocks that superfans can redeem in person."
        ),
        DropType.TOUR_STOP to DropTypeCopy(
            title = "City trail stop",
            description = "Plot location-based missions with media prompts that drive explorers to the next reveal."
        )
    )
)

private val DROP_TYPE_ORDER = listOf(
    DropType.COMMUNITY,
    DropType.RESTAURANT_COUPON,
    DropType.TOUR_STOP
)

private fun resolveDropTypeCopy(
    groups: Set<BusinessCategoryGroup>,
    type: DropType
): DropTypeCopy {
    BusinessCategoryGroup.entries
        .filter { groups.contains(it) }
        .forEach { group ->
            val override = DROP_TYPE_COPY_OVERRIDES[group]?.get(type)
            if (override != null) {
                return override
            }
        }
    return DEFAULT_DROP_TYPE_COPIES.getValue(type)
}

fun businessDropTypeOptionsFor(
    categories: Collection<BusinessCategory>
): List<BusinessDropTypeOptionCopy> {
    val uniqueCategories = categories.toSet()
    val groups = uniqueCategories.map { it.group }.toSet()
    val recommendedTypes = if (uniqueCategories.isEmpty()) {
        DropType.entries.toSet()
    } else {
        uniqueCategories
            .flatMap { category ->
                BUSINESS_DROP_TEMPLATES[category].orEmpty().map { it.dropType }
            }
            .toSet()
    }

    val dropTypes = if (recommendedTypes.isEmpty()) {
        DropType.entries.toSet()
    } else {
        recommendedTypes
    }

    val orderedTypes = DROP_TYPE_ORDER.filter { dropTypes.contains(it) }
        .ifEmpty { listOf(DropType.COMMUNITY) }

    return orderedTypes.map { type ->
        val copy = resolveDropTypeCopy(groups, type)
        BusinessDropTypeOptionCopy(
            type = type,
            title = copy.title,
            description = copy.description
        )
    }
}