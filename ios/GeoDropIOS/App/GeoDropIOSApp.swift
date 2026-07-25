import SwiftUI
import Firebase
import FirebaseAppCheck
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
        #if DEBUG
        AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
        #else
        AppCheck.setAppCheckProviderFactory(GeoDropAppCheckProviderFactory())
        #endif
        FirebaseApp.configure()
        PilotFeatureFlags.shared.start()
        Messaging.messaging().delegate = self
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        guard PilotFeatureFlags.shared.notificationsEnabled else { return }
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard PilotFeatureFlags.shared.notificationsEnabled else { return }
        guard let token = fcmToken else { return }
        NotificationCenter.default.post(name: .messagingTokenUpdated, object: token)
    }
    
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }
}

final class GeoDropAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        if #available(iOS 14.0, *) {
            return AppAttestProvider(app: app)
        }
        return DeviceCheckProvider(app: app)
    }
}

extension Notification.Name {
    static let messagingTokenUpdated = Notification.Name("GeoDropMessagingTokenUpdated")
}
