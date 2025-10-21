import Foundation
import UIKit
import UserNotifications
import FirebaseMessaging

final class MessagingService: NSObject, ObservableObject {
    static let shared = MessagingService()

    @Published private(set) var currentToken: String?

    private override init() {
        super.init()
        NotificationCenter.default.addObserver(self, selector: #selector(handleTokenUpdate(_:)), name: .messagingTokenUpdated, object: nil)
    }

    func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, error in
            if let error = error {
                print("GeoDrop: Notification authorization error \(error)")
            }
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
    }

    @objc private func handleTokenUpdate(_ notification: Notification) {
        guard let token = notification.object as? String else { return }
        DispatchQueue.main.async {
            self.currentToken = token
        }
    }
}