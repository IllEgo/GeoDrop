import Foundation

struct AppConfiguration {
    static let shared = AppConfiguration()

    let visionApiKey: String
    let mapsApiKey: String
    
    private init(bundle: Bundle = .main) {
        if let key = bundle.object(forInfoDictionaryKey: "GOOGLE_VISION_API_KEY") as? String {
            visionApiKey = key
        } else {
            visionApiKey = ""
        }
        
        if let key = bundle.object(forInfoDictionaryKey: "GOOGLE_MAPS_API_KEY") as? String {
            mapsApiKey = key
        } else {
            mapsApiKey = ""
        }
    }
}
