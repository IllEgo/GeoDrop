import Foundation
import CoreLocation

final class LocationService: NSObject, ObservableObject {
    static let shared = LocationService()

    @Published private(set) var currentLocation: CLLocation?
    @Published private(set) var authorizationStatus: CLAuthorizationStatus = .notDetermined

    private let manager = CLLocationManager()
    private var authorizationCompletion: ((CLAuthorizationStatus) -> Void)?

    private override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        authorizationStatus = manager.authorizationStatus
    }

    func requestWhenInUseAuthorization() {
        manager.requestWhenInUseAuthorization()
    }

    func requestAlwaysAuthorization(
        completion: ((CLAuthorizationStatus) -> Void)? = nil
    ) {
        guard authorizationStatus == .authorizedWhenInUse else {
            completion?(authorizationStatus)
            return
        }
        authorizationCompletion = completion
        manager.requestAlwaysAuthorization()
    }

    func refreshAuthorizationStatus() {
        authorizationStatus = manager.authorizationStatus
        if authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways {
            startUpdating()
        } else {
            stopUpdating()
        }
    }

    func startUpdating() {
        guard authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways else {
            return
        }
        manager.startUpdatingLocation()
    }

    func stopUpdating() {
        manager.stopUpdatingLocation()
    }
}

extension LocationService: CLLocationManagerDelegate {
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        if authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways {
            startUpdating()
        } else {
            stopUpdating()
        }
        if let completion = authorizationCompletion {
            authorizationCompletion = nil
            completion(authorizationStatus)
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        currentLocation = location
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("GeoDrop: Location update failed \(error)")
    }
}
