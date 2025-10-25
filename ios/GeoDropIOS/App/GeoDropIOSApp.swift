import SwiftUI
import Firebase
import FirebaseMessaging
import GoogleMaps
import GoogleSignIn
import UIKit

@main
struct GeoDropIOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var appViewModel = AppViewModel()
    
    init() {
        let symbolConfiguration = UIImage.SymbolConfiguration(scale: .small)
        UIImageView.appearance().preferredSymbolConfiguration = symbolConfiguration
        UIButton.appearance().setPreferredSymbolConfiguration(symbolConfiguration, forImageIn: .normal)
        UIButton.appearance().setPreferredSymbolConfiguration(symbolConfiguration, forImageIn: .highlighted)
        UIButton.appearance().setPreferredSymbolConfiguration(symbolConfiguration, forImageIn: .selected)
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(appViewModel)
                .geoDropTheme()
                .onAppear {
                    appViewModel.bootstrap()
                }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        if !AppConfiguration.shared.mapsApiKey.isEmpty {
            GMSServices.provideAPIKey(AppConfiguration.shared.mapsApiKey)
        }
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        application.registerForRemoteNotifications()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        NotificationCenter.default.post(name: .messagingTokenUpdated, object: token)
    }
    
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }
}

extension Notification.Name {
    static let messagingTokenUpdated = Notification.Name("GeoDropMessagingTokenUpdated")
}
