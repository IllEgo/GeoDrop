import Foundation

struct NotificationPreferences {
    static let minRadius: Double = 50
    static let maxRadius: Double = 1000
    static let defaultRadius: Double = 300

    private enum Keys {
        static let radius = "geodrop.notificationRadiusMeters"
    }

    private let defaults: UserDefaults

    init(userDefaults: UserDefaults = .standard) {
        self.defaults = userDefaults
    }

    func radiusMeters() -> Double {
        guard defaults.object(forKey: Keys.radius) != nil else {
            return Self.defaultRadius
        }
        let stored = defaults.double(forKey: Keys.radius)
        return clamp(stored)
    }

    func setRadiusMeters(_ meters: Double) {
        defaults.set(clamp(meters), forKey: Keys.radius)
    }

    private func clamp(_ value: Double) -> Double {
        return min(Self.maxRadius, max(Self.minRadius, value))
    }
}
