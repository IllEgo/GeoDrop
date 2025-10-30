import SwiftUI
import SafariServices

struct RootView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        content
            .geoDropCompactStyle()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(geoDropTheme.colors.background)
            .alert(item: Binding(
                get: { viewModel.errorMessage.map(IdentifiableError.init(message:)) },
                set: { _ in viewModel.errorMessage = nil }
            )) { wrapper in
                Alert(title: Text("Error"), message: Text(wrapper.message), dismissButton: .default(Text("OK")))
            }
            .sheet(isPresented: Binding(
                get: { viewModel.isShowingTutorialSlides },
                set: { newValue in
                    if !newValue {
                        viewModel.dismissTutorialSlides()
                    }
                }
            )) {
                TutorialSlidesView(onDismiss: viewModel.dismissTutorialSlides)
                    .environmentObject(viewModel)
            }
            .sheet(isPresented: Binding(
                get: { viewModel.isShowingFaq },
                set: { newValue in
                    if !newValue {
                        viewModel.dismissFaq()
                    }
                }
            )) {
                FaqSheet(onDismiss: viewModel.dismissFaq)
                    .environmentObject(viewModel)
            }
            .sheet(item: Binding(
                get: { viewModel.infoMenuURL.map(InfoLinkItem.init(url:)) },
                set: { newValue in
                    if let url = newValue?.url {
                        viewModel.infoMenuURL = url
                    } else {
                        viewModel.dismissInfoMenuLink()
                    }
                }
            )) { item in
                SafariView(url: item.url)
                    .ignoresSafeArea()
            }
    }

    @ViewBuilder
    private var content: some View {
        if !viewModel.hasAcceptedTerms {
            TermsAcceptanceView(onAccept: viewModel.acceptTerms)
        } else if !viewModel.hasCompletedOnboarding {
            OnboardingChecklistView(onContinue: viewModel.completeOnboarding)
        } else if let accountRole = viewModel.pendingAccountRole {
            AuthView(accountRole: accountRole, onDismiss: viewModel.cancelAuthenticationFlow)
        } else {
            switch viewModel.authState {
            case .loading:
                ProgressView("Loading...")
                    .progressViewStyle(.circular)
            case .signedOut:
                if viewModel.userMode == .guest {
                    MainTabView()
                } else {
                    UserModeSelectionView(
                        onSelectGuest: viewModel.selectGuestMode,
                        onSelectExplorer: { viewModel.beginAuthentication(for: .explorer) },
                        onSelectBusiness: { viewModel.beginAuthentication(for: .business) }
                    )
                }
            case .signedIn:
                MainTabView()
            }
        }
    }
}

private struct IdentifiableError: Identifiable {
    let id = UUID()
    let message: String
}

private struct InfoLinkItem: Identifiable {
    let url: URL
    var id: String { url.absoluteString }
}

private struct TutorialSlide: Identifiable {
    let id = UUID()
    let iconName: String
    let title: String
    let description: String
}

struct TutorialSlidesView: View {
    let onDismiss: () -> Void
    @Environment(\.geoDropTheme) private var geoDropTheme
    @State private var currentIndex = 0

    private let slides: [TutorialSlide] = [
        TutorialSlide(
            iconName: "map.fill",
            title: "Discover nearby drops",
            description: "See stories, rewards, and community posts pinned to real-world locations around you."
        ),
        TutorialSlide(
            iconName: "mappin.circle.fill",
            title: "Collect and redeem",
            description: "Walk up to a drop to unlock it, save it to your inventory, and redeem special offers in person."
        ),
        TutorialSlide(
            iconName: "storefront.fill",
            title: "Share your own moments",
            description: "Create drops with photos, audio, or coupons so nearby explorers can discover your business or story."
        ),
        TutorialSlide(
            iconName: "person.crop.circle.fill",
            title: "Build your profile",
            description: "Personalize your explorer profile to track progress and highlight the drops you're proud of."
        ),
        TutorialSlide(
            iconName: "person.3.fill",
            title: "Join community groups",
            description: "Follow local crews or start your own group to coordinate adventures and share exclusive drops."
        )
    ]

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                HStack {
                    Text("Welcome to GeoDrop")
                        .font(geoDropTheme.typography.title.weight(.bold))
                        .foregroundColor(geoDropTheme.colors.onSurface)
                    Spacer()
                    Button("Skip") {
                        onDismiss()
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(geoDropTheme.colors.primary)
                }

                TabView(selection: $currentIndex) {
                    ForEach(Array(slides.enumerated()), id: \.offset) { index, slide in
                        VStack(spacing: 16) {
                            ZStack {
                                Circle()
                                    .fill(geoDropTheme.colors.primaryContainer)
                                    .frame(width: 140, height: 140)
                                Image(systemName: slide.iconName)
                                    .font(.system(size: 64, weight: .semibold))
                                    .foregroundColor(geoDropTheme.colors.onPrimaryContainer)
                            }

                            Text(slide.title)
                                .font(.headline.weight(.semibold))
                                .multilineTextAlignment(.center)
                                .foregroundColor(geoDropTheme.colors.onSurface)

                            Text(slide.description)
                                .font(.system(size: 11))
                                .multilineTextAlignment(.center)
                                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                        }
                        .padding(.horizontal, 12)
                        .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(maxWidth: .infinity)

                HStack(spacing: 8) {
                    ForEach(0..<slides.count, id: \.self) { index in
                        Capsule()
                            .fill(index == currentIndex ? geoDropTheme.colors.primary : geoDropTheme.colors.surfaceVariant)
                            .frame(width: index == currentIndex ? 24 : 8, height: 8)
                    }
                }

                Button(action: advance) {
                    Text(currentIndex == slides.count - 1 ? "Continue" : "Next")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(geoDropTheme.colors.primary)
                        .foregroundColor(geoDropTheme.colors.onPrimary)
                        .cornerRadius(12)
                }
            }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(geoDropTheme.colors.background)
            .navigationBarHidden(true)
        }
        .tint(geoDropTheme.colors.primary)
    }

    private func advance() {
        if currentIndex >= slides.count - 1 {
            onDismiss()
        } else {
            withAnimation(.easeInOut(duration: 0.2)) {
                currentIndex += 1
            }
        }
    }
}

struct FaqSheet: View {
    let onDismiss: () -> Void
    @Environment(\.geoDropTheme) private var geoDropTheme

    private let entries: [FaqEntry] = [
        FaqEntry(
            question: "What are GeoDrop's core features?",
            answer: "Create drops at real-world spots, explore what's nearby, and connect with people and places through interactive stories, offers, and updates."
        ),
        FaqEntry(
            question: "What can a drop include?",
            answer: "Drops can hold text notes, photos, short videos, voice clips, and even business rewards or coupons. Combine the pieces that best fit the moment."
        ),
        FaqEntry(
            question: "How do explorers use drops?",
            answer: "Explorers follow the map or notifications to visit drops, open the content, react or collect it, and save favorites for later adventures."
        ),
        FaqEntry(
            question: "How do GeoDrop groups work?",
            answer: "Groups bring explorers together around shared interests, neighborhoods, or businesses. Join a group to see exclusive drops and stay in sync with the community."
        )
    ]

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("GeoDrop is built around sharing location-based drops. Here's a quick overview of how everything fits together.")
                        .font(geoDropTheme.typography.body)
                        .foregroundColor(geoDropTheme.colors.onSurfaceVariant)

                    ForEach(entries) { entry in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(entry.question)
                                .font(.headline)
                                .foregroundColor(geoDropTheme.colors.onSurface)
                            Text(entry.answer)
                                .font(geoDropTheme.typography.body)
                                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                        }
                    }
                }
                .padding(24)
            }
            .background(geoDropTheme.colors.background)
            .navigationTitle("Get to know GeoDrop")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        onDismiss()
                    }
                }
            }
        }
        .tint(geoDropTheme.colors.primary)
    }
}

private struct FaqEntry: Identifiable {
    let id = UUID()
    let question: String
    let answer: String
}

struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        SFSafariViewController(url: url)
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}
