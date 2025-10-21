import SwiftUI

struct AuthView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var isSignUp: Bool = false

    var body: some View {
        VStack(spacing: 24) {
            Text("GeoDrop")
                .font(.largeTitle)
                .fontWeight(.bold)

            VStack(alignment: .leading, spacing: 12) {
                TextField("Email", text: $email)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(10)

                SecureField("Password", text: $password)
                    .textContentType(.password)
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(10)
            }
            .padding(.horizontal)

            Button(action: submit) {
                Text(isSignUp ? "Create account" : "Sign in")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.accentColor)
                    .cornerRadius(12)
            }
            .disabled(email.isEmpty || password.count < 6)
            .padding(.horizontal)

            Button(action: { isSignUp.toggle() }) {
                Text(isSignUp ? "Already have an account? Sign in" : "Need an account? Sign up")
                    .font(.subheadline)
            }
            .padding(.top)

            Spacer()
        }
        .padding(.top, 60)
    }

    private func submit() {
        if isSignUp {
            viewModel.createAccount(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password)
        } else {
            viewModel.signIn(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password)
        }
    }
}