import SwiftUI
import UIKit

struct AccountDataView: View {
    let onDeleted: (AccountDeletionReceipt) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var password = ""
    @State private var confirmation = ""
    @State private var isWorking = false
    @State private var errorMessage: String?
    @State private var exportResult: AccountExportResult?
    @State private var deletionReceipt: AccountDeletionReceipt?

    private let service = AccountLifecycleService.shared

    var body: some View {
        NavigationView {
            Form {
                Section("Verify your identity") {
                    if service.requiresPassword {
                        SecureField("Password", text: $password)
                            .textContentType(.password)
                    } else {
                        Text("Kithe will ask you to sign in with Google again before either action.")
                            .font(.footnote)
                    }
                }

                Section("Export") {
                    Text("Create a JSON copy of your profile, memberships, inventory, drops, and report history. The download link expires after 15 minutes.")
                        .font(.footnote)
                    Button("Create data export") {
                        Task { await createExport() }
                    }
                    .disabled(isWorking)
                    if let exportResult {
                        Link("Download export", destination: exportResult.downloadURL)
                        Text("Link expires: \(exportResult.expiresAt)")
                            .font(.caption)
                    }
                }

                Section("Delete account") {
                    Text("This permanently removes your account, memberships, inventory, owned drops, and media. Safety records may be retained only as described in the approved retention policy.")
                        .font(.footnote)
                    TextField("Type DELETE", text: $confirmation)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                    Button("Permanently delete account", role: .destructive) {
                        Task { await deleteAccount() }
                    }
                    .disabled(isWorking || confirmation != "DELETE")
                }

                if isWorking {
                    Section { ProgressView("Working…") }
                }
                if let errorMessage {
                    Section { Text(errorMessage).foregroundColor(.red) }
                }
                if let deletionReceipt {
                    Section("Deletion receipt") {
                        Text("Receipt: \(deletionReceipt.receiptID)")
                        Text("Completed: \(deletionReceipt.completedAt)")
                    }
                }
            }
            .navigationTitle("Your Kithe data")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }

    @MainActor
    private func createExport() async {
        await performReauthenticatedAction {
            let result = try await service.requestExport()
            exportResult = result
        }
    }

    @MainActor
    private func deleteAccount() async {
        guard confirmation == "DELETE" else { return }
        await performReauthenticatedAction {
            let receipt = try await service.deleteAccount(confirmation: confirmation)
            deletionReceipt = receipt
            onDeleted(receipt)
        }
    }

    @MainActor
    private func performReauthenticatedAction(
        _ action: @escaping () async throws -> Void
    ) async {
        guard !isWorking else { return }
        isWorking = true
        errorMessage = nil
        defer { isWorking = false }
        do {
            if service.requiresPassword {
                try await service.reauthenticate(password: password)
            } else {
                guard let presenter = UIApplication.shared.geoDropTopViewController else {
                    throw AccountLifecycleError.googleSignInUnavailable
                }
                try await service.reauthenticateWithGoogle(presenting: presenter)
            }
            try await action()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

private extension UIApplication {
    var geoDropTopViewController: UIViewController? {
        let scene = connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var controller = scene?.windows.first { $0.isKeyWindow }?.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }
}
