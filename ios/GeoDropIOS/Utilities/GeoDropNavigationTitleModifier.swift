import SwiftUI

struct GeoDropTopNavigationBar: View {
    private let subtitle: String?
    private let leading: AnyView
    private let trailing: AnyView

    init(subtitle: String? = nil,
         leading: AnyView = AnyView(EmptyView()),
         trailing: AnyView = AnyView(EmptyView())) {
        self.subtitle = subtitle
        self.leading = leading
        self.trailing = trailing
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 16) {
                leading
                VStack(alignment: .leading, spacing: 2) {
                    Text("GeoDrop")
                        .font(.title3.weight(.bold))
                        .tracking(1.2)
                    if let subtitle, !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .accessibilityElement(children: .combine)
                Spacer(minLength: 12)
                trailing
            }
            .foregroundColor(.primary)
            .frame(minHeight: 48)
            .padding(.horizontal, 20)
            .padding(.bottom, 12)
            .padding(.top, 12)
        }
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(uiColor: .systemBackground).opacity(0.95),
                    Color(uiColor: .systemBackground).opacity(0)
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .top)
        )
        .overlay(alignment: .bottom) {
            Divider()
                .opacity(0.15)
        }
    }
}

struct GeoDropNavigationContainer<Content: View>: View {
    private let subtitle: String?
    private let leading: AnyView
    private let trailing: AnyView
    private let content: Content

    init(subtitle: String? = nil,
         @ViewBuilder leading: () -> some View = { EmptyView() },
         @ViewBuilder trailing: () -> some View = { EmptyView() },
         @ViewBuilder content: () -> Content) {
        self.subtitle = subtitle
        self.leading = AnyView(leading())
        self.trailing = AnyView(trailing())
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            GeoDropTopNavigationBar(
                subtitle: subtitle,
                leading: leading,
                trailing: trailing
            )
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Color(uiColor: .systemBackground))
    }
}
