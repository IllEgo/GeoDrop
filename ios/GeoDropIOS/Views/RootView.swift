import SwiftUI

struct RootView: View {
    @EnvironmentObject private var viewModel: AppViewModel

    var body: some View {
        Group {
            switch viewModel.authState {
            case .loading:
                ProgressView("Loading...")
                    .progressViewStyle(.circular)
            case .signedOut:
                AuthView()
            case .signedIn:
                MainTabView()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(uiColor: .systemBackground))
        .alert(item: Binding(
            get: { viewModel.errorMessage.map(IdentifiableError.init(message:)) },
            set: { _ in viewModel.errorMessage = nil }
        )) { wrapper in
            Alert(title: Text("Error"), message: Text(wrapper.message), dismissButton: .default(Text("OK")))
        }
    }
}

private struct IdentifiableError: Identifiable {
    let id = UUID()
    let message: String
}