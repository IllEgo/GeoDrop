import SwiftUI

struct GeoDropCompactStyle: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 17.0, *) {
            content
                .dynamicTypeSize(.xSmall ... .medium)
                .environment(\.imageScale, .small)
        } else {
            content
                .dynamicTypeSize(.xSmall ... .medium)
                .environment(\.imageScale, .small)
        }
    }
}

extension View {
    func geoDropCompactStyle() -> some View {
        modifier(GeoDropCompactStyle())
    }
}
