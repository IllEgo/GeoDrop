import Foundation

struct NotificationPreferences {

    private enum Keys {
        static let alertsEnabled = "geodrop.nearbyAlertsEnabled"
    }

    private let defaults: UserDefaults

    init(userDefaults: UserDefaults = .standard) {
        self.defaults = userDefaults
    }

    func nearbyAlertsEnabled() -> Bool {
        defaults.bool(forKey: Keys.alertsEnabled)
    }

    func setNearbyAlertsEnabled(_ enabled: Bool) {
        defaults.set(enabled, forKey: Keys.alertsEnabled)
    }

}
