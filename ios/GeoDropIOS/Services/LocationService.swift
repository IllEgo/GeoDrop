import Foundation
import CoreLocation

/// Location access follows the direction doc's six-step model (tasks 3.2/3.3):
/// browsing runs on approximate, one-shot fixes, and precision is requested only at
/// the moment of an unlock attempt and given up again as soon as the proximity
/// question is answered. Nothing here streams.
final class LocationService: NSObject, ObservableObject {
    static let shared = LocationService()

    /// Approximate fix for browsing: distance labels, sorting, map centring.
    /// Never precise, and never used to decide whether a drop may be unlocked.
    @Published private(set) var currentLocation: CLLocation?
    @Published private(set) var authorizationStatus: CLAuthorizationStatus = .notDetermined

    private let manager = CLLocationManager()
    private var authorizationCompletion: ((CLAuthorizationStatus) -> Void)?
    private var preciseFixCompletions: [(CLLocation?) -> Void] = []
    private var isPreciseRequestInFlight = false

    private var isAuthorized: Bool {
        authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways
    }

    private override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
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
        refreshApproximateLocation()
    }

    /// One-shot approximate fix. Call when a list or map needs a position; there is no
    /// stream to start or stop.
    func refreshApproximateLocation() {
        guard isAuthorized, !isPreciseRequestInFlight else { return }
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.requestLocation()
    }

    /// Precise one-shot for an unlock attempt. Asks for temporary full accuracy when the
    /// user keeps Precise Location off, and drops back to approximate accuracy as soon
    /// as the fix arrives. Returns nil when the fix cannot be obtained — callers must
    /// fail closed.
    func requestPreciseFix(completion: @escaping (CLLocation?) -> Void) {
        guard isAuthorized else {
            completion(nil)
            return
        }
        preciseFixCompletions.append(completion)
        guard !isPreciseRequestInFlight else { return }
        isPreciseRequestInFlight = true

        let start: () -> Void = { [weak self] in
            guard let self else { return }
            self.manager.desiredAccuracy = kCLLocationAccuracyBest
            self.manager.requestLocation()
        }

        if manager.accuracyAuthorization == .reducedAccuracy {
            manager.requestTemporaryFullAccuracyAuthorization(
                withPurposeKey: "UnlockDrop"
            ) { [weak self] _ in
                // Proceed either way: if the user declined, the fix that arrives will be
                // too coarse and the accuracy check rejects it.
                guard self != nil else { return }
                start()
            }
        } else {
            start()
        }
    }

    private func finishPreciseRequest(with location: CLLocation?) {
        isPreciseRequestInFlight = false
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        let completions = preciseFixCompletions
        preciseFixCompletions.removeAll()
        completions.forEach { $0(location) }
    }
}

extension LocationService: CLLocationManagerDelegate {
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        if isAuthorized {
            refreshApproximateLocation()
        } else {
            currentLocation = nil
            if isPreciseRequestInFlight {
                finishPreciseRequest(with: nil)
            }
        }
        if let completion = authorizationCompletion {
            authorizationCompletion = nil
            completion(authorizationStatus)
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        if isPreciseRequestInFlight {
            finishPreciseRequest(with: location)
        } else {
            currentLocation = location
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("GeoDrop: Location update failed \(error)")
        if isPreciseRequestInFlight {
            finishPreciseRequest(with: nil)
        }
    }
}
