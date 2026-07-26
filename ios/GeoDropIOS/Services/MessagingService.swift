import Foundation
import UIKit
import UserNotifications
import FirebaseMessaging

final class MessagingService: NSObject, ObservableObject {
    static let shared = MessagingService()

    @Published private(set) var currentToken: String?
    @Published private(set) var authorizationStatus: UNAuthorizationStatus = .notDetermined

    private override init() {
        super.init()
        NotificationCenter.default.addObserver(self, selector: #selector(handleTokenUpdate(_:)), name: .messagingTokenUpdated, object: nil)
    }

    func refreshAuthorizationStatus(completion: ((UNAuthorizationStatus) -> Void)? = nil) {
        guard PilotFeatureFlags.shared.notificationsEnabled else {
            authorizationStatus = .denied
            completion?(.denied)
            return
        }
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                self.authorizationStatus = settings.authorizationStatus
                completion?(settings.authorizationStatus)
            }
        }
    }

    func requestAuthorization(completion: @escaping (Bool) -> Void) {
        guard PilotFeatureFlags.shared.notificationsEnabled else {
            completion(false)
            return
        }
        refreshAuthorizationStatus { status in
            switch status {
            case .authorized, .provisional, .ephemeral:
                UIApplication.shared.registerForRemoteNotifications()
                completion(true)
            case .notDetermined:
                UNUserNotificationCenter.current().requestAuthorization(
                    options: [.alert, .badge, .sound]
                ) { granted, error in
                    if let error {
                        print("GeoDrop: Notification authorization error \(error)")
                    }
                    DispatchQueue.main.async {
                        self.refreshAuthorizationStatus()
                        if granted {
                            UIApplication.shared.registerForRemoteNotifications()
                        }
                        completion(granted)
                    }
                }
            case .denied:
                completion(false)
            @unknown default:
                completion(false)
            }
        }
    }

    @objc private func handleTokenUpdate(_ notification: Notification) {
        guard PilotFeatureFlags.shared.notificationsEnabled else { return }
        guard let token = notification.object as? String else { return }
        DispatchQueue.main.async {
            self.currentToken = token
        }
    }
}
