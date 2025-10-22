import SwiftUI

struct GeoDropNavigationTitleModifier: ViewModifier {
    let subtitle: String?

    func body(content: Content) -> some View {
        content
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("GeoDrop")
                            .font(.title3.weight(.bold))
                        if let subtitle, !subtitle.isEmpty {
                            Text(subtitle)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityAddTraits(.isHeader)
                }
            }
    }
}

extension View {
    func geoDropNavigationTitle(subtitle: String? = nil) -> some View {
        modifier(GeoDropNavigationTitleModifier(subtitle: subtitle))
    }
}
