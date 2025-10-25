import SwiftUI

struct GeoDropTopNavigationBar: View {
    private let leading: AnyView
    private let trailing: AnyView
    @Environment(\.geoDropTheme) private var geoDropTheme
    
    
    init(leading: AnyView = AnyView(EmptyView()),
         trailing: AnyView = AnyView(EmptyView())) {
        self.leading = leading
        self.trailing = trailing
    }
    
    private let titleFont = Font.system(size: 12, weight: .semibold, design: .default)
    private let contentFont = Font.system(size: 12, weight: .semibold, design: .default)

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 10) {
                leading
                    .font(contentFont)
                    .imageScale(.small)
                VStack(alignment: .leading, spacing: 2) {
                    Text("GeoDrop")
                        .font(titleFont)
                        .tracking(0.8)
                        .foregroundColor(geoDropTheme.colors.onSurface)
                }
                .accessibilityElement(children: .combine)
                Spacer(minLength: 12)
                trailing
                    .font(contentFont)
                    .imageScale(.small)
            }
            .foregroundColor(geoDropTheme.colors.onSurface)
            .font(contentFont)
            .imageScale(.small)
            .frame(minHeight: 0)
            .padding(.horizontal, 16)
            .padding(.vertical, 4)
        }
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                gradient: Gradient(colors: [
                    geoDropTheme.colors.surface.opacity(0.95),
                    geoDropTheme.colors.surface.opacity(0)
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .top)
        )
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(geoDropTheme.colors.outlineVariant.opacity(0.6))
                .frame(height: 0.5)
        }
    }
}

struct GeoDropNavigationContainer<Content: View>: View {
    private let leading: AnyView
    private let trailing: AnyView
    private let content: Content
    @Environment(\.geoDropTheme) private var geoDropTheme
    
    init(@ViewBuilder leading: () -> some View = { EmptyView() },
         @ViewBuilder trailing: () -> some View = { EmptyView() },
         @ViewBuilder content: () -> Content) {
        self.leading = AnyView(leading())
        self.trailing = AnyView(trailing())
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            GeoDropTopNavigationBar(
                leading: leading,
                trailing: trailing
            )
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(geoDropTheme.colors.background)
    }
}
