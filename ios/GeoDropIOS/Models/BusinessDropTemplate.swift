import Foundation

struct BusinessDropTemplate: Identifiable, Equatable {
    let id: String
    let category: BusinessCategory
    let title: String
    let description: String
    let dropType: DropType
    let contentType: DropContentType
    let note: String
    let caption: String
    let callToAction: String?
}

enum BusinessDropTemplates {
    static func suggestions(for categories: [BusinessCategory]) -> [BusinessDropTemplate] {
        var seenCategoryIds = Set<String>()
        let categoryIds = categories.reduce(into: [String]()) { partialResult, category in
            if seenCategoryIds.insert(category.id).inserted {
                partialResult.append(category.id)
            }
        }

        var seenTemplates = Set<String>()
        var ordered: [BusinessDropTemplate] = []

        func append(_ template: BusinessDropTemplate) {
            if seenTemplates.insert(template.id).inserted {
                ordered.append(template)
            }
        }

        for template in starterTemplates {
            append(template)
        }

        for categoryId in categoryIds {
            guard let templates = categoryTemplates[categoryId] else { continue }
            for template in templates {
                append(template)
            }
        }

        return ordered
    }

    private static func category(_ id: String) -> BusinessCategory {
        BusinessCategory.from(id: id) ?? BusinessCategory.all.first!
    }
    
    
    static var generalTemplates: [BusinessDropTemplate] {
        starterTemplates
    }

    static func templates(for category: BusinessCategory) -> [BusinessDropTemplate] {
        categoryTemplates[category.id] ?? []
    }

    static func templates(for categories: [BusinessCategory]) -> [BusinessDropTemplate] {
        var ordered = [BusinessDropTemplate]()
        var seen = Set<String>()
        for category in categories {
            for template in templates(for: category) {
                if seen.insert(template.id).inserted {
                    ordered.append(template)
                }
            }
        }
        return ordered
    }

    private static let starterTemplates: [BusinessDropTemplate] = [
        BusinessDropTemplate(
            id: "general_welcome_note",
            category: category("FOOD_RESTAURANTS_CAFES"),
            title: "Welcome explorers",
            description: "Set the tone for anyone discovering your business for the first time.",
            dropType: .community,
            contentType: .text,
            note: "Welcome to our spot! Show this message at the counter for a warm hello and let us know what brings you in today.",
            caption: "A warm hello from our team.",
            callToAction: "Personalize this greeting with your brand voice.",
        ),
        BusinessDropTemplate(
            id: "general_loyalty_coupon",
            category: category("RETAIL_LOCAL_SHOPS"),
            title: "Loyalty reward",
            description: "Encourage repeat visits with a simple redemption code.",
            dropType: .restaurantCoupon,
            contentType: .text,
            note: "Thanks for stopping by! Mention code LOCALLOVE at checkout to unlock a loyalty perk just for GeoDrop explorers.",
            caption: "Unlock a perk just for GeoDrop fans.",
            callToAction: "Update the code and perk to match your offer.",
        ),
        BusinessDropTemplate(
            id: "general_behind_scenes",
            category: category("MARKETING_INFLUENCERS_CREATORS"),
            title: "Behind-the-scenes moment",
            description: "Capture a quick video or audio snippet that feels exclusive.",
            dropType: .community,
            contentType: .video,
            note: "You're seeing it first! Record a quick clip sharing what we're working on right now and invite explorers to follow along.",
            caption: "Exclusive peek at what we're creating.",
            callToAction: "Record a fresh clip before publishing.",
        )
    ]

    private static let categoryTemplates: [String: [BusinessDropTemplate]] = [
        "COMMUNITY_LIBRARIES_CULTURE": [
            BusinessDropTemplate(
                id: "library_storytime",
                category: category("COMMUNITY_LIBRARIES_CULTURE"),
                title: "Storytime reminder",
                description: "Promote programming across the space.",
                dropType: .community,
                contentType: .text,
                note: "Storytime starts at 11am downstairs. Whisper BOOKWORM at the desk for a themed bookmark while supplies last.",
                caption: "Storytime kicks off downstairs soon.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "library_readinglist",
                category: category("COMMUNITY_LIBRARIES_CULTURE"),
                title: "Reading list",
                description: "Recommend related titles for explorers to check out.",
                dropType: .community,
                contentType: .text,
                note: "Love this exhibit? Ask a librarian for the GeoDrop reading list featuring three must-read picks connected to it.",
                caption: "Grab our curated reading list.",
                callToAction: nil,
            )
        ],
        "COMMUNITY_NONPROFITS_CENTERS": [
            BusinessDropTemplate(
                id: "nonprofit_volunteer",
                category: category("COMMUNITY_NONPROFITS_CENTERS"),
                title: "Volunteer call",
                description: "Invite neighbors to lend a hand.",
                dropType: .community,
                contentType: .text,
                note: "We're preparing weekend care kits. Drop in Saturday at 10am and mention 'GeoDrop helpers' to join the volunteer crew.",
                caption: "Join our weekend volunteer crew.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "nonprofit_resources",
                category: category("COMMUNITY_NONPROFITS_CENTERS"),
                title: "Resource spotlight",
                description: "Share how to access key services.",
                dropType: .community,
                contentType: .text,
                note: "Need support? This week we're offering free career coaching. Visit the front desk and reference code CONNECT for details.",
                caption: "Ask about this week's free resources.",
                callToAction: nil,
            )
        ],
        "COMMUNITY_SCHOOLS_UNIVERSITIES": [
            BusinessDropTemplate(
                id: "school_orientation",
                category: category("COMMUNITY_SCHOOLS_UNIVERSITIES"),
                title: "Orientation stop",
                description: "Support campus tours with additional info.",
                dropType: .tourStop,
                contentType: .text,
                note: "Welcome to the student union! Find the mural inside and snap a photo to collect points for your orientation scavenger hunt.",
                caption: "Welcome stop on your campus trail.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "school_alert",
                category: category("COMMUNITY_SCHOOLS_UNIVERSITIES"),
                title: "Event reminder",
                description: "Promote upcoming meetings or safety info.",
                dropType: .community,
                contentType: .text,
                note: "Campus update: Free tutoring tonight in Room 210 from 6–8pm. Share this drop with a friend who needs a boost.",
                caption: "Don't miss tonight's campus event.",
                callToAction: nil,
            )
        ],
        "EVENTS_CONCERT_VENUES_THEATERS": [
            BusinessDropTemplate(
                id: "venue_setlist",
                category: category("EVENTS_CONCERT_VENUES_THEATERS"),
                title: "Setlist teaser",
                description: "Get fans excited before showtime.",
                dropType: .community,
                contentType: .text,
                note: "Tonight's secret song? Check the merch table and whisper ENCORE to learn the surprise closer.",
                caption: "Get the inside scoop on tonight's set.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "venue_merch",
                category: category("EVENTS_CONCERT_VENUES_THEATERS"),
                title: "Merch booth perk",
                description: "Drive sales during intermission.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Flash this drop at the lobby merch booth and use code BACKSTAGE for 10% off one item tonight only.",
                caption: "Flash this for a merch perk.",
                callToAction: nil,
            )
        ],
        "EVENTS_FESTIVALS_FAIRS": [
            BusinessDropTemplate(
                id: "festival_clue",
                category: category("EVENTS_FESTIVALS_FAIRS"),
                title: "Hidden booth clue",
                description: "Encourage exploration across the grounds.",
                dropType: .community,
                contentType: .text,
                note: "Seek out the booth with the sunflower flag. Tell them BLOOM to grab a festival-exclusive sticker.",
                caption: "Hunt down the hidden sunflower booth.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "festival_map",
                category: category("EVENTS_FESTIVALS_FAIRS"),
                title: "Trail highlight",
                description: "Guide attendees toward the next activity.",
                dropType: .tourStop,
                contentType: .text,
                note: "You're on the art walk! Follow the lanterns to the community mural wall for live painting demos.",
                caption: "Follow the lanterns to the next activity.",
                callToAction: nil,
            )
        ],
        "EVENTS_SPORTS_ARENAS": [
            BusinessDropTemplate(
                id: "arena_upgrade",
                category: category("EVENTS_SPORTS_ARENAS"),
                title: "Seat upgrade lottery",
                description: "Surprise lucky fans during the game.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "You've found the hidden drop! Visit section 104 and mention code FANUPGRADE for a chance at better seats.",
                caption: "Score a shot at better seats.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "arena_trivia",
                category: category("EVENTS_SPORTS_ARENAS"),
                title: "Team trivia",
                description: "Boost engagement during timeouts.",
                dropType: .community,
                contentType: .text,
                note: "Trivia time: Who holds the arena scoring record? Tell guest services your answer to enter our merch raffle.",
                caption: "Test your team trivia knowledge.",
                callToAction: nil,
            )
        ],
        "FOOD_BARS_BREWERIES": [
            BusinessDropTemplate(
                id: "bar_happy_hour",
                category: category("FOOD_BARS_BREWERIES"),
                title: "Happy hour cheers",
                description: "Promote a timed deal on drinks or bites.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Happy hour just for you! Mention code GEOHOPS between 4–6pm for $2 off our featured pint.",
                caption: "Cheers to exclusive happy hour savings.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "bar_live_music",
                category: category("FOOD_BARS_BREWERIES"),
                title: "Live music lineup",
                description: "Tease tonight's entertainment.",
                dropType: .community,
                contentType: .text,
                note: "Tonight at 8pm: The Riverside Duo takes the stage. Drop by early for the best seats and a sneak peek at the setlist.",
                caption: "Catch tonight's live vibes.",
                callToAction: nil,
            )
        ],
        "FOOD_RESTAURANTS_CAFES": [
            BusinessDropTemplate(
                id: "restaurants_morning_special",
                category: category("FOOD_RESTAURANTS_CAFES"),
                title: "Morning special",
                description: "Highlight a limited-time breakfast or coffee offer.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Rise and shine! Flash code MORNINGBREW before 11am for 20% off any pastry + coffee combo today only.",
                caption: "Start your day with our morning treat.",
                callToAction: "Adjust the code and time window before posting.",
            ),
            BusinessDropTemplate(
                id: "restaurants_chef_story",
                category: category("FOOD_RESTAURANTS_CAFES"),
                title: "Chef's table note",
                description: "Share a short story about today's menu.",
                dropType: .community,
                contentType: .text,
                note: "Chef's highlight: Our seasonal soup is simmering with roasted squash and sage. Ask about the story behind the recipe!",
                caption: "Today's menu story from the chef.",
                callToAction: nil,
            )
        ],
        "FOOD_TRUCKS_STREET_VENDORS": [
            BusinessDropTemplate(
                id: "foodtruck_location_ping",
                category: category("FOOD_TRUCKS_STREET_VENDORS"),
                title: "Today's parking spot",
                description: "Let fans know exactly where to find you.",
                dropType: .community,
                contentType: .text,
                note: "We rolled up at the corner of 5th & Oak until 2pm. Show this drop for a free topping upgrade!",
                caption: "Find us parked nearby today.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "foodtruck_secret_code",
                category: category("FOOD_TRUCKS_STREET_VENDORS"),
                title: "Secret menu bite",
                description: "Reward people who tracked you down.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Ask for the 'GeoDrop bite' and say code STREETFIND for an off-menu sampler while supplies last.",
                caption: "Ask for the GeoDrop bite.",
                callToAction: nil,
            )
        ],
        "HOSPITALITY_HOTELS_RESORTS": [
            BusinessDropTemplate(
                id: "hotel_welcome",
                category: category("HOSPITALITY_HOTELS_RESORTS"),
                title: "Guest welcome",
                description: "Greet arrivals with a concierge note.",
                dropType: .community,
                contentType: .text,
                note: "Welcome to your stay! Stop by the front desk and mention SUNRISE to unlock a late checkout upgrade (based on availability).",
                caption: "Enjoy a warm welcome upgrade.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "hotel_pool_club",
                category: category("HOSPITALITY_HOTELS_RESORTS"),
                title: "Poolside perk",
                description: "Encourage on-property exploration.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Head to the rooftop pool bar and use code SPLASH for a complimentary mocktail this afternoon.",
                caption: "Splash into a rooftop perk.",
                callToAction: nil,
            )
        ],
        "HOSPITALITY_MUSEUMS_GALLERIES": [
            BusinessDropTemplate(
                id: "museum_fact",
                category: category("HOSPITALITY_MUSEUMS_GALLERIES"),
                title: "Behind-the-art fact",
                description: "Reveal context that isn't on the placard.",
                dropType: .tourStop,
                contentType: .text,
                note: "Look closer: this painting hides a message from the artist's mentor. Ask a docent about the signature in the corner.",
                caption: "Discover the art secret in front of you.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "museum_audio",
                category: category("HOSPITALITY_MUSEUMS_GALLERIES"),
                title: "Curator voice note",
                description: "Record a quick audio insight to accompany the exhibit.",
                dropType: .tourStop,
                contentType: .audio,
                note: "Record a 60-second curator tip highlighting what makes this piece significant to our collection.",
                caption: "Press play for a curator insight.",
                callToAction: nil,
            )
        ],
        "HOSPITALITY_TOUR_GUIDES_ATTRACTIONS": [
            BusinessDropTemplate(
                id: "tour_story",
                category: category("HOSPITALITY_TOUR_GUIDES_ATTRACTIONS"),
                title: "Guided story stop",
                description: "Share narration when visitors arrive at a landmark.",
                dropType: .tourStop,
                contentType: .text,
                note: "You're standing where the city was founded. Tap play on our audio guide or ask your guide for the 'Origins' story.",
                caption: "Listen to the origin story here.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "tour_next_clue",
                category: category("HOSPITALITY_TOUR_GUIDES_ATTRACTIONS"),
                title: "Scavenger clue",
                description: "Keep groups moving to the next point of interest.",
                dropType: .tourStop,
                contentType: .text,
                note: "Clue unlocked! Count the arches above you and bring the number to the fountain on Market Street for the next tale.",
                caption: "Follow this clue to your next stop.",
                callToAction: nil,
            )
        ],
        "MARKETING_GUERRILLA_BRANDS": [
            BusinessDropTemplate(
                id: "guerrilla_secret_code",
                category: category("MARKETING_GUERRILLA_BRANDS"),
                title: "Secret code drop",
                description: "Hide a reward in plain sight.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "You've cracked the mission. Whisper CODEWORD IMPACT at our next pop-up for limited swag.",
                caption: "Whisper the mission code for swag.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "guerrilla_clue",
                category: category("MARKETING_GUERRILLA_BRANDS"),
                title: "Next clue",
                description: "Guide fans to the next experience.",
                dropType: .community,
                contentType: .text,
                note: "The trail continues near the mural with neon wings. Scan for our next drop to unlock what's coming.",
                caption: "Track the next clue on the trail.",
                callToAction: nil,
            )
        ],
        "MARKETING_INFLUENCERS_CREATORS": [
            BusinessDropTemplate(
                id: "creator_meetup",
                category: category("MARKETING_INFLUENCERS_CREATORS"),
                title: "Pop-up meetup",
                description: "Invite followers to a quick hangout.",
                dropType: .community,
                contentType: .video,
                note: "Record a clip inviting explorers to meet you here at 5pm for a selfie session and surprise shout-outs.",
                caption: "Meet me IRL for selfies later.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "creator_code",
                category: category("MARKETING_INFLUENCERS_CREATORS"),
                title: "Exclusive code",
                description: "Reward loyal fans with a limited perk.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Say code CREWLOVE at checkout for a fan-only discount on today's merch drop.",
                caption: "Use the fan code for a drop deal.",
                callToAction: nil,
            )
        ],
        "MARKETING_LOCAL_GOV_TOURISM": [
            BusinessDropTemplate(
                id: "tourism_challenge",
                category: category("MARKETING_LOCAL_GOV_TOURISM"),
                title: "Explore challenge",
                description: "Encourage residents to discover hidden gems.",
                dropType: .tourStop,
                contentType: .text,
                note: "City quest stop! Snap a photo with the sculpture and tag #GeoDropCity for a chance to win local gift cards.",
                caption: "Take the city quest challenge here.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "tourism_event",
                category: category("MARKETING_LOCAL_GOV_TOURISM"),
                title: "Event spotlight",
                description: "Promote upcoming civic programming.",
                dropType: .community,
                contentType: .text,
                note: "Don't miss Friday's night market on Main Street. Mention code HELLOCITY at the welcome tent for a visitor guide.",
                caption: "Make plans for Friday's night market.",
                callToAction: nil,
            )
        ],
        "RETAIL_LOCAL_SHOPS": [
            BusinessDropTemplate(
                id: "retail_flash_sale",
                category: category("RETAIL_LOCAL_SHOPS"),
                title: "Flash sale",
                description: "Drive immediate foot traffic with a surprise discount.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "GeoDrop exclusive! Say code SHOPLOCAL for 15% off any two items before close tonight.",
                caption: "Snag the flash sale before it's gone.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "retail_story",
                category: category("RETAIL_LOCAL_SHOPS"),
                title: "Maker spotlight",
                description: "Share the story behind a featured product.",
                dropType: .community,
                contentType: .text,
                note: "Meet the maker: This week's featured artisan is River & Clay. Ask us to show you how they craft each piece.",
                caption: "Hear the maker's story up close.",
                callToAction: nil,
            )
        ],
        "RETAIL_MALLS_SHOPPING_CENTERS": [
            BusinessDropTemplate(
                id: "mall_event_pass",
                category: category("RETAIL_MALLS_SHOPPING_CENTERS"),
                title: "Event passport",
                description: "Encourage visitors to explore multiple stops.",
                dropType: .tourStop,
                contentType: .text,
                note: "Stamp stop 3 of our Shop & Win trail! Collect all five codes from participating stores and redeem at guest services.",
                caption: "Collect your next event passport stamp.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "mall_coupon",
                category: category("RETAIL_MALLS_SHOPPING_CENTERS"),
                title: "Center-wide perk",
                description: "Offer a perk funded by participating tenants.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Show this at the info desk to claim a surprise from our weekend pop-ups. Mention code MALLMOMENT.",
                caption: "Claim a center-wide surprise perk.",
                callToAction: nil,
            )
        ],
        "RETAIL_POP_UPS_MARKETS": [
            BusinessDropTemplate(
                id: "popup_drop",
                category: category("RETAIL_POP_UPS_MARKETS"),
                title: "Limited release alert",
                description: "Build urgency for limited inventory.",
                dropType: .community,
                contentType: .text,
                note: "Only 30 jars of today's small-batch jam! Tell us you found the GeoDrop to reserve one while you're here.",
                caption: "Limited jars—grab yours fast.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "popup_loyalty",
                category: category("RETAIL_POP_UPS_MARKETS"),
                title: "Market loyalty stamp",
                description: "Reward explorers who visit multiple stalls.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Collect three GeoDrop codes around the market and say MARKETTRAIL here for a bundle discount.",
                caption: "Earn your market loyalty reward.",
                callToAction: nil,
            )
        ],
        "SERVICES_GYMS_FITNESS_STUDIOS": [
            BusinessDropTemplate(
                id: "gym_trial",
                category: category("SERVICES_GYMS_FITNESS_STUDIOS"),
                title: "Free class pass",
                description: "Invite explorers to try a session.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Ready to sweat? Mention code TRYFIT for a complimentary drop-in class this week.",
                caption: "Try a free class on us.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "gym_challenge",
                category: category("SERVICES_GYMS_FITNESS_STUDIOS"),
                title: "Challenge of the day",
                description: "Gamify the visit with a quick task.",
                dropType: .community,
                contentType: .text,
                note: "Today's challenge: 20 wall balls + 10 burpees. Post a pic and tag us to enter our monthly gear giveaway.",
                caption: "Take on today's fitness challenge.",
                callToAction: nil,
            )
        ],
        "SERVICES_LOCAL_PROVIDERS": [
            BusinessDropTemplate(
                id: "services_walkin",
                category: category("SERVICES_LOCAL_PROVIDERS"),
                title: "Walk-in special",
                description: "Encourage immediate bookings.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Drop-in deal: Mention code QUICKFIX for 10% off same-day appointments.",
                caption: "Walk in now for a same-day perk.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "services_tip",
                category: category("SERVICES_LOCAL_PROVIDERS"),
                title: "Pro tip",
                description: "Share expertise to build trust.",
                dropType: .community,
                contentType: .text,
                note: "Pro tip from our team: Replace your filters every 60 days. Show us this note for a free reminder sticker.",
                caption: "Grab a quick pro tip from our team.",
                callToAction: nil,
            )
        ],
        "SERVICES_REAL_ESTATE_AGENTS": [
            BusinessDropTemplate(
                id: "realestate_openhouse",
                category: category("SERVICES_REAL_ESTATE_AGENTS"),
                title: "Open house stop",
                description: "Guide self-led tours through listings.",
                dropType: .tourStop,
                contentType: .text,
                note: "Welcome in! Start in the kitchen and look for the chalkboard for renovation notes. Scan each room's drop for extra insights.",
                caption: "Start your self-guided tour here.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "realestate_feature",
                category: category("SERVICES_REAL_ESTATE_AGENTS"),
                title: "Feature focus",
                description: "Highlight a hidden gem in the property.",
                dropType: .community,
                contentType: .text,
                note: "Don't miss the backyard studio—it's wired and climate-controlled. Ask about converting it into your dream workspace.",
                caption: "Peek at this property's hidden gem.",
                callToAction: nil,
            )
        ],
        "TRAVEL_AIRPORTS_STATIONS": [
            BusinessDropTemplate(
                id: "airport_gate_update",
                category: category("TRAVEL_AIRPORTS_STATIONS"),
                title: "Gate-side update",
                description: "Share timely travel information.",
                dropType: .community,
                contentType: .text,
                note: "Flight updates: Announcements post here first. Check in with our desk and mention code TRAVELSMART for lounge tips.",
                caption: "Stay in the know before you board.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "airport_lounge",
                category: category("TRAVEL_AIRPORTS_STATIONS"),
                title: "Lounge invitation",
                description: "Promote premium experiences.",
                dropType: .restaurantCoupon,
                contentType: .text,
                note: "Relax before you fly. Present code RUNWAYREST at the lounge door for a complimentary drink upgrade.",
                caption: "Upgrade your wait with lounge perks.",
                callToAction: nil,
            )
        ],
        "TRAVEL_CRUISE_LINES_TOUR_BUSES": [
            BusinessDropTemplate(
                id: "cruise_daily",
                category: category("TRAVEL_CRUISE_LINES_TOUR_BUSES"),
                title: "Daily schedule",
                description: "Share what's happening right now onboard.",
                dropType: .community,
                contentType: .text,
                note: "Today's onboard highlight: Sunset deck yoga at 6pm. Check in with the host and say code SEAFOCUS for a reserved mat.",
                caption: "Join today's can't-miss onboard event.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "cruise_port_tip",
                category: category("TRAVEL_CRUISE_LINES_TOUR_BUSES"),
                title: "Port insider tip",
                description: "Offer guidance before guests disembark.",
                dropType: .tourStop,
                contentType: .text,
                note: "Next port secret: Take the blue tram to the local market and look for the coral arch—our partner shop has samples waiting.",
                caption: "Get the insider port game plan.",
                callToAction: nil,
            )
        ],
        "TRAVEL_RENTAL_CAR_SHUTTLES": [
            BusinessDropTemplate(
                id: "rental_pickup",
                category: category("TRAVEL_RENTAL_CAR_SHUTTLES"),
                title: "Pickup instructions",
                description: "Give clear steps when travelers arrive.",
                dropType: .tourStop,
                contentType: .text,
                note: "Welcome to pickup zone B. Text us your stall number when parked and we'll pull your car around in minutes.",
                caption: "Follow these easy pickup steps.",
                callToAction: nil,
            ),
            BusinessDropTemplate(
                id: "rental_return",
                category: category("TRAVEL_RENTAL_CAR_SHUTTLES"),
                title: "Return reminder",
                description: "Help guests wrap up their rental with ease.",
                dropType: .community,
                contentType: .text,
                note: "Returning tonight? Mention code FUELCHECK and we'll top off the tank for you at our discounted rate.",
                caption: "Ask about a seamless return perk.",
                callToAction: nil,
            )
        ],
    ]
}
