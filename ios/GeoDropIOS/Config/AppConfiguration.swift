import Foundation
import FirebaseRemoteConfig

struct AppConfiguration {
    static let shared = AppConfiguration()

    let mapsApiKey: String
    let featureCreationEnabled: Bool
    let featureNotificationsEnabled: Bool
    let featureCouponsEnabled: Bool
    let featureMediaEnabled: Bool
    let featureHuntsEnabled: Bool
    
    private init(bundle: Bundle = .main) {
        if let key = bundle.object(forInfoDictionaryKey: "GOOGLE_MAPS_API_KEY") as? String {
            mapsApiKey = key
        } else {
            mapsApiKey = ""
        }
        featureCreationEnabled = Self.featureFlag("GEODROP_FEATURE_CREATION_ENABLED", bundle: bundle)
        featureNotificationsEnabled = Self.featureFlag("GEODROP_FEATURE_NOTIFICATIONS_ENABLED", bundle: bundle)
        featureCouponsEnabled = Self.featureFlag("GEODROP_FEATURE_COUPONS_ENABLED", bundle: bundle)
        featureMediaEnabled = Self.featureFlag("GEODROP_FEATURE_MEDIA_ENABLED", bundle: bundle)
        featureHuntsEnabled = Self.featureFlag("GEODROP_FEATURE_HUNTS_ENABLED", bundle: bundle)
    }

    private static func featureFlag(_ key: String, bundle: Bundle) -> Bool {
        if let value = bundle.object(forInfoDictionaryKey: key) as? Bool {
            return value
        }
        guard let value = bundle.object(forInfoDictionaryKey: key) as? String else {
            return false
        }
        return ["1", "YES", "TRUE"].contains(value.trimmingCharacters(in: .whitespacesAndNewlines).uppercased())
    }
}

final class PilotFeatureFlags {
    static let shared = PilotFeatureFlags()
    static let didUpdateNotification = Notification.Name("GeoDropPilotFeatureFlagsDidUpdate")

    private enum Key {
        static let creation = "pilot_creation_enabled"
        static let notifications = "pilot_notifications_enabled"
        static let coupons = "pilot_coupons_enabled"
        static let media = "pilot_media_enabled"
        static let hunts = "pilot_hunts_enabled"
        static let all = [creation, notifications, coupons, media, hunts]
    }

    private let build = AppConfiguration.shared
    private var remoteConfig: RemoteConfig?

    private init() {}

    var creationEnabled: Bool { build.featureCreationEnabled && remoteValue(Key.creation) }
    var notificationsEnabled: Bool { build.featureNotificationsEnabled && remoteValue(Key.notifications) }
    var couponsEnabled: Bool { build.featureCouponsEnabled && remoteValue(Key.coupons) }
    var mediaEnabled: Bool { build.featureMediaEnabled && remoteValue(Key.media) }
    var huntsEnabled: Bool { build.featureHuntsEnabled && remoteValue(Key.hunts) }

    func start() {
        guard remoteConfig == nil else { return }
        let config = RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 3_600
        config.configSettings = settings
        let defaults: [String: NSObject] = Dictionary(
            uniqueKeysWithValues: Key.all.map { ($0, NSNumber(value: false) as NSObject) }
        )
        config.setDefaults(defaults)
        remoteConfig = config

        config.fetchAndActivate { [weak self] _, _ in
            self?.notifyUpdate()
        }
        config.addOnConfigUpdateListener { [weak self] _, error in
            guard error == nil else { return }
            config.activate { _, _ in
                self?.notifyUpdate()
            }
        }
    }

    private func remoteValue(_ key: String) -> Bool {
        remoteConfig?.configValue(forKey: key).boolValue ?? false
    }

    private func notifyUpdate() {
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: Self.didUpdateNotification, object: nil)
        }
    }
}
