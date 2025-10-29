import SwiftUI

struct GeoDropTypography {
    let title: Font
    let body: Font

    static let `default` = GeoDropTypography(
        title: .system(size: 11, weight: .semibold, design: .default),
        body: .system(size: 9, weight: .regular, design: .default)
    )
}
