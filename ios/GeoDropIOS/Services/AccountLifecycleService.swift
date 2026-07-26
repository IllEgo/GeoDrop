import Foundation
import FirebaseAuth
import FirebaseCore
import FirebaseFunctions
import GoogleSignIn
import UIKit

struct AccountExportResult {
    let requestID: String
    let downloadURL: URL
    let expiresAt: String
    let policyVersion: String
}

struct AccountDeletionReceipt {
    let receiptID: String
    let status: String
    let completedAt: String
    let policyVersion: String
    let deletedDrops: Int
    let deletedMediaObjects: Int
}

final class AccountLifecycleService {
    static let shared = AccountLifecycleService()
    static let policyVersion = "pilot-2026-07-21-draft"

    private let functions: Functions

    init(functions: Functions = Functions.functions()) {
        self.functions = functions
    }

    var requiresPassword: Bool {
        Auth.auth().currentUser?.providerData.contains {
            $0.providerID == EmailAuthProviderID
        } == true
    }

    func reauthenticate(password: String) async throws {
        guard let user = Auth.auth().currentUser, let email = user.email else {
            throw AccountLifecycleError.notSignedIn
        }
        let normalized = password.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else {
            throw AccountLifecycleError.passwordRequired
        }
        let credential = EmailAuthProvider.credential(withEmail: email, password: normalized)
        _ = try await user.reauthenticate(with: credential)
    }

    func reauthenticateWithGoogle(presenting viewController: UIViewController) async throws {
        guard let user = Auth.auth().currentUser else {
            throw AccountLifecycleError.notSignedIn
        }
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            throw AccountLifecycleError.googleSignInUnavailable
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: viewController)
        guard let idToken = result.user.idToken?.tokenString else {
            throw AccountLifecycleError.googleSignInUnavailable
        }
        let credential = GoogleAuthProvider.credential(
            withIDToken: idToken,
            accessToken: result.user.accessToken.tokenString
        )
        _ = try await user.reauthenticate(with: credential)
    }

    func requestExport() async throws -> AccountExportResult {
        try await refreshIDToken()
        let data = try await call(
            name: "requestAccountExport",
            data: ["policyVersion": Self.policyVersion]
        )
        guard
            let requestID = data["requestId"] as? String,
            let rawURL = data["downloadUrl"] as? String,
            let downloadURL = URL(string: rawURL),
            let expiresAt = data["expiresAt"] as? String,
            let policyVersion = data["policyVersion"] as? String
        else {
            throw AccountLifecycleError.invalidResponse
        }
        return AccountExportResult(
            requestID: requestID,
            downloadURL: downloadURL,
            expiresAt: expiresAt,
            policyVersion: policyVersion
        )
    }

    func deleteAccount(confirmation: String) async throws -> AccountDeletionReceipt {
        try await refreshIDToken()
        let data = try await call(
            name: "deleteAccount",
            data: [
                "policyVersion": Self.policyVersion,
                "confirmation": confirmation,
            ]
        )
        guard
            let receiptID = data["receiptId"] as? String,
            let status = data["status"] as? String,
            let completedAt = data["completedAt"] as? String,
            let policyVersion = data["policyVersion"] as? String
        else {
            throw AccountLifecycleError.invalidResponse
        }
        let counts = data["counts"] as? [String: Any] ?? [:]
        let receipt = AccountDeletionReceipt(
            receiptID: receiptID,
            status: status,
            completedAt: completedAt,
            policyVersion: policyVersion,
            deletedDrops: (counts["drops"] as? NSNumber)?.intValue ?? 0,
            deletedMediaObjects: (counts["mediaObjects"] as? NSNumber)?.intValue ?? 0
        )
        UserDefaults.standard.set(receipt.receiptID, forKey: "geodrop.lastDeletionReceiptID")
        UserDefaults.standard.set(receipt.completedAt, forKey: "geodrop.lastDeletionCompletedAt")
        UserDefaults.standard.set(receipt.policyVersion, forKey: "geodrop.lastDeletionPolicyVersion")
        return receipt
    }

    private func refreshIDToken() async throws {
        guard let user = Auth.auth().currentUser else {
            throw AccountLifecycleError.notSignedIn
        }
        _ = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<String, Error>) in
            user.getIDTokenForcingRefresh(true) { token, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let token {
                    continuation.resume(returning: token)
                } else {
                    continuation.resume(throwing: AccountLifecycleError.invalidResponse)
                }
            }
        }
    }

    private func call(name: String, data: [String: Any]) async throws -> [String: Any] {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<[String: Any], Error>) in
            functions.httpsCallable(name).call(data) { result, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let data = result?.data as? [String: Any] {
                    continuation.resume(returning: data)
                } else {
                    continuation.resume(throwing: AccountLifecycleError.invalidResponse)
                }
            }
        }
    }
}

enum AccountLifecycleError: LocalizedError {
    case notSignedIn
    case invalidResponse
    case passwordRequired
    case googleSignInUnavailable

    var errorDescription: String? {
        switch self {
        case .notSignedIn:
            return "Sign in to manage account data."
        case .invalidResponse:
            return "GeoDrop returned an invalid account-data response."
        case .passwordRequired:
            return "Enter your password to continue."
        case .googleSignInUnavailable:
            return "Google reauthentication is unavailable. Try again."
        }
    }
}
