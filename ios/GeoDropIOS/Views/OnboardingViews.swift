import SwiftUI

struct TermsAcceptanceView: View {
    let onAccept: () -> Void
    var onDecline: (() -> Void)?

    private let termsURL = URL(string: "https://www.geodrop.app/terms")
    private let privacyURL = URL(string: "https://www.geodrop.app/privacy")

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    Image(systemName: "globe.americas.fill")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 90, height: 90)
                        .foregroundStyle(.accent)
                        .padding(.top, 40)

                    Text("Before you explore GeoDrop, please review and accept our Terms of Service and Privacy Policy.")
                        .font(.title2.weight(.semibold))
                        .multilineTextAlignment(.center)

                    VStack(alignment: .leading, spacing: 16) {
                        Label("GeoDrop lets you discover and share local drops.", systemImage: "map")
                        Label("We collect limited data to keep your experience secure.", systemImage: "lock.shield")
                        Label("You can leave guest mode or delete your account at any time.", systemImage: "person.crop.circle.badge.xmark")
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(16)

                    VStack(spacing: 12) {
                        if let termsURL {
                            Link(destination: termsURL) {
                                Label("Terms of Service", systemImage: "doc.text")
                            }
                        }
                        if let privacyURL {
                            Link(destination: privacyURL) {
                                Label("Privacy Policy", systemImage: "hand.raised")
                            }
                        }
                    }
                    .font(.headline)

                    Button(action: onAccept) {
                        Text("Accept and Continue")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                    .padding(.top)

                    if let onDecline {
                        Button(action: onDecline) {
                            Text("Maybe later")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding(.bottom, 32)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 40)
            }
            .navigationTitle("Welcome")
        }
    }
}

struct OnboardingChecklistView: View {
    let onContinue: () -> Void

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
                        .multilineTextAlignment(.center)
                        .padding(.top, 32)

                    VStack(alignment: .leading, spacing: 20) {
                        ForEach(checklist) { item in
                            HStack(alignment: .top, spacing: 16) {
                                Image(systemName: item.icon)
                                    .font(.title2)
                                    .foregroundColor(.accentColor)
                                    .frame(width: 32, height: 32)
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(item.title)
                                        .font(.headline)
                                    Text(item.description)
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(16)

                    Button(action: onContinue) {
                        Text("Start exploring")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                    .padding(.bottom, 40)
                }
                .padding(.horizontal, 24)
            }
            .navigationTitle("Getting Started")
        }
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

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    Text("How would you like to use GeoDrop?")
                        .font(.title.weight(.bold))
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
        }
    }

    private struct ModeCard: View {
        let icon: String
        let title: String
        let description: String
        let actionTitle: String
        let action: () -> Void

        var body: some View {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 12) {
                    Image(systemName: icon)
                        .font(.title2)
                        .foregroundColor(.accentColor)
                    Text(title)
                        .font(.headline)
                }

                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Button(action: action) {
                    Text(actionTitle)
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(uiColor: .secondarySystemBackground))
            .cornerRadius(16)
            .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 5)
        }
    }
}
