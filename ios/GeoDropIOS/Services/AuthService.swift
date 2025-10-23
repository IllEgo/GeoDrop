import Foundation
import FirebaseAuth
import FirebaseCore
import GoogleSignIn
import UIKit

final class AuthService {
    static let shared = AuthService()

    private var authHandle: AuthStateDidChangeListenerHandle?
    private(set) var currentUser: User? = Auth.auth().currentUser
    
    enum AuthServiceError: LocalizedError {
        case missingGoogleClientID
        case missingGoogleCredentials

        var errorDescription: String? {
            switch self {
            case .missingGoogleClientID:
                return "Google sign-in isn't configured."
            case .missingGoogleCredentials:
                return "Google credentials are unavailable."
            }
        }
    }

    func observeAuthChanges(_ onChange: @escaping (User?) -> Void) {
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            self?.currentUser = user
            onChange(user)
        }
    }

    func stopObserving() {
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
            authHandle = nil
        }
    }

    func signIn(email: String, password: String) async throws {
        _ = try await Auth.auth().signIn(withEmail: email, password: password)
    }

    func createAccount(email: String, password: String) async throws {
        _ = try await Auth.auth().createUser(withEmail: email, password: password)
    }

    func signOut() throws {
        try Auth.auth().signOut()
    }

    func linkWithEmail(email: String, password: String) async throws {
        guard let user = Auth.auth().currentUser else { return }
        let credential = EmailAuthProvider.credential(withEmail: email, password: password)
        _ = try await user.link(with: credential)
    }
    
    func signInWithGoogle(presenting viewController: UIViewController) async throws {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            throw AuthServiceError.missingGoogleClientID
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signOut()

        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: viewController)

        guard let idToken = result.user.idToken?.tokenString else {
            throw AuthServiceError.missingGoogleCredentials
        }

        let accessToken = result.user.accessToken.tokenString
        let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: accessToken)
        _ = try await Auth.auth().signIn(with: credential)
    }
}
