import Foundation
import FirebaseAuth
import FirebaseCore
import FirebaseFunctions
import GoogleSignIn
import UIKit

final class AuthService {
    static let shared = AuthService()

    private var authHandle: AuthStateDidChangeListenerHandle?
    private(set) var currentUser: User? = Auth.auth().currentUser
    private lazy var functions = Functions.functions(region: "us-central1")

    /// Task 4.6 — what happened to a guest's activity when they signed in.
    ///
    /// A guest who signs in gets a *new* uid unless the anonymous account is
    /// linked in place, and everything they made is attached to the old one.
    /// Linking is preferred precisely because it makes this a non-event; the
    /// merge exists for the case linking cannot cover, where the credential
    /// already belongs to an account.
    enum GuestContentOutcome {
        /// There was no guest session — an ordinary sign-in.
        case notApplicable
        /// The anonymous account became the real one; the uid never changed.
        case linked
        /// A different account already existed, and the content was moved to it.
        case merged
        /// The move failed. The user is signed in; their guest activity is not.
        case mergeFailed
    }

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

    /// Signing in means the account already exists, so linking is deliberately
    /// not attempted: linking an email credential *creates* the account, and a
    /// mistyped address would silently make a new one instead of reporting that
    /// there is nothing to sign into.
    @discardableResult
    func signIn(email: String, password: String) async throws -> GuestContentOutcome {
        guard let guest = Auth.auth().currentUser, guest.isAnonymous else {
            _ = try await Auth.auth().signIn(withEmail: email, password: password)
            return .notApplicable
        }

        // Captured before the switch: afterwards there is no way to prove that
        // guest session was ever ours, and the server accepts nothing less.
        let guestToken = try? await guest.getIDToken()
        _ = try await Auth.auth().signIn(withEmail: email, password: password)
        return await mergeGuestContent(token: guestToken)
    }

    /// For a guest this is a link, which turns the anonymous account into a real
    /// one without changing the uid — the problem disappears instead of being
    /// repaired afterwards. A collision means the address is already registered,
    /// which is a real error for a registration attempt, so it propagates.
    @discardableResult
    func createAccount(email: String, password: String) async throws -> GuestContentOutcome {
        let credential = EmailAuthProvider.credential(withEmail: email, password: password)
        guard let guest = Auth.auth().currentUser, guest.isAnonymous else {
            _ = try await Auth.auth().createUser(withEmail: email, password: password)
            return .notApplicable
        }

        _ = try await guest.link(with: credential)
        return .linked
    }

    func signOut() throws {
        try Auth.auth().signOut()
    }

    @discardableResult
    func signInWithGoogle(presenting viewController: UIViewController) async throws -> GuestContentOutcome {
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

        guard let guest = Auth.auth().currentUser, guest.isAnonymous else {
            _ = try await Auth.auth().signIn(with: credential)
            return .notApplicable
        }

        let guestToken = try? await guest.getIDToken()
        do {
            _ = try await guest.link(with: credential)
            return .linked
        } catch let error as NSError where AuthService.credentialBelongsToAnotherAccount(error) {
            // The only case linking cannot resolve: this Google account already
            // exists, so Firebase has to issue a different uid and the guest's
            // content has to be handed over server-side.
            _ = try await Auth.auth().signIn(with: credential)
            return await mergeGuestContent(token: guestToken)
        }
    }

    /// Task 4.6 — a failed merge must not fail the sign-in.
    ///
    /// The user is already signed in by the time this runs, so throwing would
    /// show them an error about something that succeeded. The outcome is
    /// returned instead, so the caller can say the guest activity did not come
    /// across rather than pretending it did.
    private func mergeGuestContent(token: String?) async -> GuestContentOutcome {
        guard let token = token, !token.isEmpty else { return .mergeFailed }
        do {
            _ = try await functions.httpsCallable("mergeGuestAccount")
                .call(["guestIdToken": token])
            return .merged
        } catch {
            print("GeoDrop: guest content could not be merged \(error)")
            return .mergeFailed
        }
    }

    private static func credentialBelongsToAnotherAccount(_ error: NSError) -> Bool {
        guard error.domain == AuthErrorDomain,
              let code = AuthErrorCode(rawValue: error.code) else { return false }
        switch code {
        case .credentialAlreadyInUse, .emailAlreadyInUse, .accountExistsWithDifferentCredential:
            return true
        default:
            return false
        }
    }
}
