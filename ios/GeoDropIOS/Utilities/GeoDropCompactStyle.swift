import SwiftUI

struct GeoDropCompactStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .dynamicTypeSize(.xSmall ... .medium)
            .textScaleEffect(0.78, anchor: .topLeading)
            .environment(\.imageScale, .small)
    }
}

extension View {
    func geoDropCompactStyle() -> some View {
        modifier(GeoDropCompactStyle())
    }
}
