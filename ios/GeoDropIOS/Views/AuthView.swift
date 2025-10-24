import SwiftUI
import UIKit

struct AuthView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme
    let accountRole: UserRole
    let onDismiss: () -> Void
    
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var isSignUp: Bool = false
    
    private var isProcessing: Bool {
        viewModel.isAuthenticating || viewModel.isGoogleSigningIn
    }

    private var buttonTitle: String { isSignUp ? "Create account" : "Sign in" }

    private var navigationTitle: String {
        switch accountRole {
        case .explorer:
            return isSignUp ? "Explorer Sign Up" : "Explorer Sign In"
        case .business:
            return isSignUp ? "Business Sign Up" : "Business Sign In"
        }
    }

    private var accountDescription: String {
        switch accountRole {
        case .explorer:
            return "Use your explorer account to participate in community drops."
        case .business:
            return "Manage your business presence, publish promotions, and reach explorers nearby."
        }
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    VStack(spacing: 12) {
                        Text("GeoDrop")
                            .font(.largeTitle.weight(.bold))
                            .foregroundColor(geoDropTheme.colors.onSurface)
                        Text(accountDescription)
                            .font(.subheadline)
                            .multilineTextAlignment(.center)
                            .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                    }
                    .padding(.top, 24)

                    Picker("Mode", selection: $isSignUp) {
                        Text("Sign in").tag(false)
                        Text("Sign up").tag(true)
                    }
                    .pickerStyle(.segmented)
                    .tint(geoDropTheme.colors.primary)
                    .onChange(of: isSignUp) { _ in viewModel.resetAuthFlowMessages() }

                    VStack(spacing: 16) {
                        TextField("Email", text: $email)
                            .textInputAutocapitalization(.never)
                            .disableAutocorrection(true)
                            .textContentType(.emailAddress)
                            .keyboardType(.emailAddress)
                            .padding()
                            .background(geoDropTheme.colors.surfaceVariant)
                            .cornerRadius(12)

                        SecureField("Password", text: $password)
                            .textContentType(.password)
                            .padding()
                            .background(geoDropTheme.colors.surfaceVariant)
                            .cornerRadius(12)
                    }

                    Button(action: submit) {
                        Text(buttonTitle)
                            .font(.headline)
                            .foregroundColor(geoDropTheme.colors.onPrimary)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(canSubmit ? geoDropTheme.colors.primary : geoDropTheme.colors.primary.opacity(0.4))
                            .cornerRadius(12)
                    }
                    .disabled(!canSubmit || isProcessing)

                    Divider()
                        .padding(.vertical, 8)

                    Button(action: startGoogleSignIn) {
                        HStack(spacing: 12) {
                            Image(systemName: "g.circle")
                                .font(.title3)
                            Text("Continue with Google")
                                .font(.headline)
                        }
                        .foregroundColor(geoDropTheme.colors.onSurface)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(geoDropTheme.colors.surfaceVariant)
                        .cornerRadius(12)
                    }
                    .disabled(isProcessing)

                    if isProcessing {
                        ProgressView()
                            .progressViewStyle(.circular)
                        .tint(geoDropTheme.colors.primary)
                    }

                    if let status = viewModel.authFlowStatus {
                        Label(status, systemImage: "checkmark.circle.fill")
                        .foregroundColor(geoDropTheme.colors.primary)
                    }

                    if let error = viewModel.authFlowError {
                        Label(error, systemImage: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                    }

                    Spacer(minLength: 20)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 40)
            }
            .navigationTitle(navigationTitle)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Back", action: onDismiss)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        }
        .onAppear { viewModel.resetAuthFlowMessages() }
        .background(geoDropTheme.colors.background)
    }

    private var canSubmit: Bool {
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmedEmail.isEmpty && password.count >= 6
    }

    private func submit() {
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        if isSignUp {
            viewModel.createAccount(email: trimmedEmail, password: password)
        } else {
            viewModel.signIn(email: trimmedEmail, password: password)
        }
    }

    private func startGoogleSignIn() {
        guard let controller = currentRootController() else {
            viewModel.authFlowError = "Couldn't present Google sign-in."
            return
        }
        viewModel.signInWithGoogle(presenting: controller)
    }

    private func currentRootController() -> UIViewController? {
        guard
            let scene = UIApplication.shared
                .connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first(where: { $0.activationState == .foregroundActive }),
            let root = scene
                .windows
                .first(where: { $0.isKeyWindow })?
                .rootViewController
        else { return nil }

        var top = root
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }
}
