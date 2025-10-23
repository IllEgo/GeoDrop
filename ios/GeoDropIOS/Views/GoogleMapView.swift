import SwiftUI
import GoogleMaps
import CoreLocation
import UIKit

struct GoogleMapCameraState: Equatable {
    var latitude: CLLocationDegrees
    var longitude: CLLocationDegrees
    var zoom: Float

    static let defaultZoom: Float = 12

    init(latitude: CLLocationDegrees, longitude: CLLocationDegrees, zoom: Float) {
        self.latitude = latitude
        self.longitude = longitude
        self.zoom = zoom
    }

    init(cameraPosition: GMSCameraPosition) {
        self.init(
            latitude: cameraPosition.target.latitude,
            longitude: cameraPosition.target.longitude,
            zoom: cameraPosition.zoom
        )
    }

    func makeCameraPosition() -> GMSCameraPosition {
        GMSCameraPosition.camera(withLatitude: latitude, longitude: longitude, zoom: zoom)
    }
}

struct GoogleMapView: UIViewRepresentable {
    let drops: [Drop]
    @Binding var selectedDropID: Drop.ID?
    @Binding var cameraState: GoogleMapCameraState
    @Binding var shouldAnimateCamera: Bool
    var isInteractionEnabled: Bool = true
    var onSelectDrop: ((Drop) -> Void)? = nil

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> GMSMapView {
        let mapView = GMSMapView(frame: .zero)
        context.coordinator.attach(to: mapView)
        mapView.isMyLocationEnabled = false
        mapView.settings.setAllGesturesEnabled(isInteractionEnabled)
        mapView.camera = cameraState.makeCameraPosition()
        context.coordinator.lastCameraState = cameraState
        context.coordinator.updateMarkers(drops: drops, selectedDropID: selectedDropID)
        return mapView
    }

    func updateUIView(_ mapView: GMSMapView, context: Context) {
        context.coordinator.parent = self
        mapView.settings.setAllGesturesEnabled(isInteractionEnabled)
        context.coordinator.updateMarkers(drops: drops, selectedDropID: selectedDropID)
        context.coordinator.updateCameraIfNeeded(to: cameraState, animate: shouldAnimateCamera)
    }

    class Coordinator: NSObject, GMSMapViewDelegate {
        var parent: GoogleMapView
        weak var mapView: GMSMapView?
        var markers: [Drop.ID: GMSMarker] = [:]
        var lastCameraState: GoogleMapCameraState?

        init(parent: GoogleMapView) {
            self.parent = parent
            super.init()
        }

        func attach(to mapView: GMSMapView) {
            self.mapView = mapView
            mapView.delegate = self
        }

        func updateMarkers(drops: [Drop], selectedDropID: Drop.ID?) {
            guard let mapView else { return }

            let dropIDs = Set(drops.map { $0.id })
            let obsolete = markers.keys.filter { !dropIDs.contains($0) }
            for id in obsolete {
                markers[id]?.map = nil
                markers.removeValue(forKey: id)
            }

            for drop in drops {
                let marker = markers[drop.id] ?? GMSMarker()
                marker.position = CLLocationCoordinate2D(latitude: drop.latitude, longitude: drop.longitude)
                marker.title = drop.displayTitle
                marker.snippet = drop.description
                marker.userData = drop.id
                marker.icon = markerIcon(isSelected: drop.id == selectedDropID)
                marker.map = mapView
                markers[drop.id] = marker
            }
        }

        func updateCameraIfNeeded(to state: GoogleMapCameraState, animate: Bool) {
            guard let mapView else { return }
            guard lastCameraState != state else {
                resetAnimationFlagIfNeeded()
                return
            }

            lastCameraState = state
            if animate {
                mapView.animate(to: state.makeCameraPosition())
            } else {
                mapView.camera = state.makeCameraPosition()
            }
            resetAnimationFlagIfNeeded()
        }

        func mapView(_ mapView: GMSMapView, idleAt position: GMSCameraPosition) {
            let state = GoogleMapCameraState(cameraPosition: position)
            lastCameraState = state
            if parent.cameraState != state {
                parent.cameraState = state
            }
        }

        func mapView(_ mapView: GMSMapView, didTap marker: GMSMarker) -> Bool {
            guard parent.isInteractionEnabled else { return true }
            guard let dropID = marker.userData as? Drop.ID else { return false }
            parent.selectedDropID = dropID
            if let drop = parent.drops.first(where: { $0.id == dropID }) {
                parent.onSelectDrop?(drop)
            }
            updateMarkers(drops: parent.drops, selectedDropID: dropID)
            return true
        }

        private func markerIcon(isSelected: Bool) -> UIImage? {
            let accent = UIColor(named: "AccentColor") ?? .systemBlue
            let baseColor = isSelected ? accent : .systemRed
            return GMSMarker.markerImage(with: baseColor)
        }

        private func resetAnimationFlagIfNeeded() {
            guard parent.shouldAnimateCamera else { return }
            DispatchQueue.main.async { [weak self] in
                self?.parent.shouldAnimateCamera = false
            }
        }
    }
}
