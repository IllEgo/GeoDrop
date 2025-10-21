import Foundation
import FirebaseAuth

final class AuthService {
    static let shared = AuthService()

    private var authHandle: AuthStateDidChangeListenerHandle?
    private(set) var currentUser: User? = Auth.auth().currentUser

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
}
