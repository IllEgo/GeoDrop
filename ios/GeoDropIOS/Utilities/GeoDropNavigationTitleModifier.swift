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
    
    private var titleFont: Font { geoDropTheme.typography.title }
    private var contentFont: Font { geoDropTheme.typography.body.weight(.semibold) }

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 10) {
                leading
                    .font(contentFont)
                    .imageScale(.small)
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text("GeoDrop")
                            .font(titleFont)
                            .tracking(0.8)
                            .foregroundColor(geoDropTheme.colors.onSurface)
                    }
                }
                .accessibilityElement(children: .combine)
                Spacer(minLength: 12)
                trailing
                    .font(contentFont)
                    .imageScale(.small)
                InfoMenuButton()
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

private struct InfoMenuButton: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        Menu {
            Button(action: { viewModel.showTutorialSlides() }) {
                Label("Tutorial slides", systemImage: "play.circle")
            }
            Button(action: { viewModel.showFaq() }) {
                Label("FAQ", systemImage: "questionmark.circle")
            }
            Button(action: { viewModel.showTermsOfService() }) {
                Label("Terms of Service", systemImage: "doc.text")
            }
            Button(action: { viewModel.showPrivacyPolicy() }) {
                Label("Privacy Policy", systemImage: "hand.raised")
            }
        } label: {
            Image(systemName: "info.circle")
                .foregroundColor(geoDropTheme.colors.onSurface.opacity(0.8))
                .imageScale(.medium)
        }
        .accessibilityLabel("Open GeoDrop info options")
    }
}
