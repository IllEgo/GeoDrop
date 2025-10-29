import SwiftUI

private extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}

struct GeoDropColors {
    let primary: Color
    let onPrimary: Color
    let primaryContainer: Color
    let onPrimaryContainer: Color
    let inversePrimary: Color
    let secondary: Color
    let onSecondary: Color
    let secondaryContainer: Color
    let onSecondaryContainer: Color
    let tertiary: Color
    let onTertiary: Color
    let tertiaryContainer: Color
    let onTertiaryContainer: Color
    let background: Color
    let onBackground: Color
    let surface: Color
    let onSurface: Color
    let surfaceVariant: Color
    let onSurfaceVariant: Color
    let surfaceTint: Color
    let inverseSurface: Color
    let inverseOnSurface: Color
    let outline: Color
    let outlineVariant: Color
    let scrim: Color

    static let light = GeoDropColors(
        primary: Color(hex: 0x0F7C4D),
        onPrimary: Color(hex: 0xFFFFFF),
        primaryContainer: Color(hex: 0xCFF5E3),
        onPrimaryContainer: Color(hex: 0x00391F),
        inversePrimary: Color(hex: 0x56D39A),
        secondary: Color(hex: 0x147D64),
        onSecondary: Color(hex: 0xFFFFFF),
        secondaryContainer: Color(hex: 0xBDEFE4),
        onSecondaryContainer: Color(hex: 0x04362A),
        tertiary: Color(hex: 0x72590A),
        onTertiary: Color(hex: 0xFFFFFF),
        tertiaryContainer: Color(hex: 0xFFF1C9),
        onTertiaryContainer: Color(hex: 0x2D2300),
        background: Color(hex: 0xF4FBF7),
        onBackground: Color(hex: 0x10291A),
        surface: Color(hex: 0xFFFFFF),
        onSurface: Color(hex: 0x10291A),
        surfaceVariant: Color(hex: 0xDBEAE1),
        onSurfaceVariant: Color(hex: 0x2F4B3A),
        surfaceTint: Color(hex: 0x0F7C4D),
        inverseSurface: Color(hex: 0x1D3526),
        inverseOnSurface: Color(hex: 0xF4FBF7),
        outline: Color(hex: 0x6F8E7C),
        outlineVariant: Color(hex: 0xC4DCCD),
        scrim: Color(hex: 0x000000)
    )

    static let dark = GeoDropColors(
        primary: Color(hex: 0x56D39A),
        onPrimary: Color(hex: 0x00391F),
        primaryContainer: Color(hex: 0x104E32),
        onPrimaryContainer: Color(hex: 0xCFF5E3),
        inversePrimary: Color(hex: 0x0F7C4D),
        secondary: Color(hex: 0x66DDBA),
        onSecondary: Color(hex: 0x00382B),
        secondaryContainer: Color(hex: 0x0F4F3F),
        onSecondaryContainer: Color(hex: 0xBDEFE4),
        tertiary: Color(hex: 0xE9CD72),
        onTertiary: Color(hex: 0x2D2300),
        tertiaryContainer: Color(hex: 0x3A2E00),
        onTertiaryContainer: Color(hex: 0xFFF1C9),
        background: Color(hex: 0x081C11),
        onBackground: Color(hex: 0xDCEFE3),
        surface: Color(hex: 0x0E2A1B),
        onSurface: Color(hex: 0xDCEFE3),
        surfaceVariant: Color(hex: 0x1D3A2C),
        onSurfaceVariant: Color(hex: 0xC4DCCD),
        surfaceTint: Color(hex: 0x56D39A),
        inverseSurface: Color(hex: 0xDCEFE3),
        inverseOnSurface: Color(hex: 0x0E2A1B),
        outline: Color(hex: 0x81A891),
        outlineVariant: Color(hex: 0x294435),
        scrim: Color(hex: 0x000000)
    )
}

enum GeoDropThemeStyle {
    case light
    case dark
}

struct GeoDropTheme {
    let style: GeoDropThemeStyle
    let colors: GeoDropColors
    let typography: GeoDropTypography

    static let light = GeoDropTheme(style: .light, colors: .light, typography: .default)
    static let dark = GeoDropTheme(style: .dark, colors: .dark, typography: .default)

    init(style: GeoDropThemeStyle, colors: GeoDropColors, typography: GeoDropTypography = .default) {
        self.style = style
        self.colors = colors
        self.typography = typography
    }

    static func preferred(for colorScheme: ColorScheme) -> GeoDropTheme {
        colorScheme == .dark ? .dark : .light
    }
}

private struct GeoDropThemeKey: EnvironmentKey {
    static let defaultValue: GeoDropTheme = .light
}

extension EnvironmentValues {
    var geoDropTheme: GeoDropTheme {
        get { self[GeoDropThemeKey.self] }
        set { self[GeoDropThemeKey.self] = newValue }
    }
}

private struct GeoDropThemeModifier: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme
    let overrideStyle: GeoDropThemeStyle?

    func body(content: Content) -> some View {
        let theme: GeoDropTheme
        if let overrideStyle {
            switch overrideStyle {
            case .light:
                theme = .light
            case .dark:
                theme = .dark
            }
        } else {
            theme = GeoDropTheme.preferred(for: colorScheme)
        }

        return content.environment(\.geoDropTheme, theme)
    }
}

extension View {
    func geoDropTheme(_ style: GeoDropThemeStyle? = nil) -> some View {
        modifier(GeoDropThemeModifier(overrideStyle: style))
    }
}
