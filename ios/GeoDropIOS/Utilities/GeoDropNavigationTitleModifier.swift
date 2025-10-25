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

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 14) {
                leading
                VStack(alignment: .leading, spacing: 2) {
                    Text("GeoDrop")
                        .font(.headline.weight(.bold))
                        .tracking(1.2)
                        .foregroundColor(geoDropTheme.colors.onSurface)
                }
                .accessibilityElement(children: .combine)
                Spacer(minLength: 12)
                trailing
            }
            .foregroundColor(geoDropTheme.colors.onSurface)
            .frame(minHeight: 10)
            .padding(.horizontal, 18)
            .padding(.bottom, 6)
            .padding(.top, 6)
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
