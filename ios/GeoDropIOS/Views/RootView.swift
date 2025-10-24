import SwiftUI

struct RootView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(geoDropTheme.colors.background)
            .alert(item: Binding(
                get: { viewModel.errorMessage.map(IdentifiableError.init(message:)) },
                set: { _ in viewModel.errorMessage = nil }
            )) { wrapper in
                Alert(title: Text("Error"), message: Text(wrapper.message), dismissButton: .default(Text("OK")))
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
