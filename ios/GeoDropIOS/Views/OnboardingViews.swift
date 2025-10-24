import SwiftUI

struct TermsAcceptanceView: View {
    let onAccept: () -> Void
    var onDecline: (() -> Void)?

    private let termsURL = URL(string: "https://www.geodrop.app/terms")
    private let privacyURL = URL(string: "https://www.geodrop.app/privacy")
    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    Image(systemName: "globe.americas.fill")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 90, height: 90)
                        .foregroundStyle(geoDropTheme.colors.primary)
                        .padding(.top, 40)

                    Text("Before you explore GeoDrop, please review and accept our Terms of Service and Privacy Policy.")
                        .font(.title2.weight(.semibold))
                        .foregroundColor(geoDropTheme.colors.onSurface)
                        .multilineTextAlignment(.center)

                    VStack(alignment: .leading, spacing: 16) {
                        Label("GeoDrop lets you discover and share local drops.", systemImage: "map")
                        Label("We collect limited data to keep your experience secure.", systemImage: "lock.shield")
                        Label("You can leave guest mode or delete your account at any time.", systemImage: "person.crop.circle.badge.xmark")
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(geoDropTheme.colors.surfaceVariant)
                    .cornerRadius(16)

                    VStack(spacing: 12) {
                        if let termsURL {
                            Link(destination: termsURL) {
                                Label("Terms of Service", systemImage: "doc.text")
                                    .foregroundColor(geoDropTheme.colors.primary)
                            }
                        }
                        if let privacyURL {
                            Link(destination: privacyURL) {
                                Label("Privacy Policy", systemImage: "hand.raised")
                                    .foregroundColor(geoDropTheme.colors.primary)
                            }
                        }
                    }
                    .font(.headline)

                    Button(action: onAccept) {
                        Text("Accept and Continue")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(geoDropTheme.colors.primary)
                            .foregroundColor(geoDropTheme.colors.onPrimary)
                            .cornerRadius(12)
                    }
                    .padding(.top)

                    if let onDecline {
                        Button(action: onDecline) {
                            Text("Maybe later")
                                .font(.subheadline)
                                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                        }
                        .padding(.bottom, 32)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 40)
            }
            .navigationTitle("Welcome")
        }
        .tint(geoDropTheme.colors.primary)
        .background(geoDropTheme.colors.background)
    }
}

struct OnboardingChecklistView: View {
    let onContinue: () -> Void
    @Environment(\.geoDropTheme) private var geoDropTheme

    private let checklist: [ChecklistItem] = [
        .init(icon: "mappin.and.ellipse", title: "Find drops nearby", description: "See what explorers and businesses are sharing around you."),
        .init(icon: "sparkles", title: "React and collect", description: "Save your favorite drops and let creators know you enjoyed them."),
        .init(icon: "person.3", title: "Join communities", description: "Follow groups to unlock local recommendations."),
        .init(icon: "shield.lefthalf.fill", title: "Stay safe", description: "Report content that breaks the rules so the community stays welcoming."),
    ]

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    Text("You're almost ready!")
                        .font(.largeTitle.weight(.bold))
                        .foregroundColor(geoDropTheme.colors.onSurface)
                        .multilineTextAlignment(.center)
                        .padding(.top, 32)

                    VStack(alignment: .leading, spacing: 20) {
                        ForEach(checklist) { item in
                            HStack(alignment: .top, spacing: 16) {
                                Image(systemName: item.icon)
                                    .font(.title2)
                                    .foregroundColor(geoDropTheme.colors.primary)
                                    .frame(width: 32, height: 32)
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(item.title)
                                        .font(.headline)
                                        .foregroundColor(geoDropTheme.colors.onSurface)
                                    Text(item.description)
                                        .font(.subheadline)
                                        .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    .padding()
                    .background(geoDropTheme.colors.surfaceVariant)
                    .cornerRadius(16)

                    Button(action: onContinue) {
                        Text("Start exploring")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(geoDropTheme.colors.primary)
                            .foregroundColor(geoDropTheme.colors.onPrimary)
                            .cornerRadius(12)
                    }
                    .padding(.bottom, 40)
                }
                .padding(.horizontal, 24)
            }
            .navigationTitle("Getting Started")
            .tint(geoDropTheme.colors.primary)
        }
        .background(geoDropTheme.colors.background)
    }

    private struct ChecklistItem: Identifiable {
        let id = UUID()
        let icon: String
        let title: String
        let description: String
    }
}

struct UserModeSelectionView: View {
    let onSelectGuest: () -> Void
    let onSelectExplorer: () -> Void
    let onSelectBusiness: () -> Void
    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    Text("How would you like to use GeoDrop?")
                        .font(.title.weight(.bold))
                        .foregroundColor(geoDropTheme.colors.onSurface)
                        .multilineTextAlignment(.center)
                        .padding(.top, 32)

                    ModeCard(
                        icon: "eyes",
                        title: "Explore as Guest",
                        description: "Preview drops around you before creating an account.",
                        actionTitle: "Continue as Guest",
                        action: onSelectGuest
                    )

                    ModeCard(
                        icon: "figure.walk",
                        title: "Sign in as Explorer",
                        description: "Join local communities, react to drops, and share your own.",
                        actionTitle: "Sign in",
                        action: onSelectExplorer
                    )

                    ModeCard(
                        icon: "briefcase.fill",
                        title: "Sign in as Business",
                        description: "Publish promotions and manage your business presence on GeoDrop.",
                        actionTitle: "Sign in",
                        action: onSelectBusiness
                    )
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 40)
            }
            .navigationTitle("Choose a mode")
            .tint(geoDropTheme.colors.primary)
        }
        .background(geoDropTheme.colors.background)
    }

    private struct ModeCard: View {
        let icon: String
        let title: String
        let description: String
        let actionTitle: String
        let action: () -> Void
        @Environment(\.geoDropTheme) private var geoDropTheme

        var body: some View {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 12) {
                    Image(systemName: icon)
                        .font(.title2)
                        .foregroundColor(geoDropTheme.colors.primary)
                    Text(title)
                        .font(.headline)
                        .foregroundColor(geoDropTheme.colors.onSurface)
                }

                Text(description)
                    .font(.subheadline)
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)

                Button(action: action) {
                    Text(actionTitle)
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(geoDropTheme.colors.primary)
                        .foregroundColor(geoDropTheme.colors.onPrimary)
                        .cornerRadius(12)
                }
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(geoDropTheme.colors.surfaceVariant)
            .cornerRadius(16)
            .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 5)
        }
    }
}
