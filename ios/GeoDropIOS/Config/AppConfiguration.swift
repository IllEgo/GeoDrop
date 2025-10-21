import Foundation

struct AppConfiguration {
    static let shared = AppConfiguration()

    let visionApiKey: String

    private init(bundle: Bundle = .main) {
        if let key = bundle.object(forInfoDictionaryKey: "GOOGLE_VISION_API_KEY") as? String {
            visionApiKey = key
        } else {
            visionApiKey = ""
        }
    }
}