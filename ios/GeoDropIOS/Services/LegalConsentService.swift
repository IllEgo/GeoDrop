import Foundation
import FirebaseFunctions

struct LegalPolicyManifest {
    let version: String
    let terms: URL
    let privacy: URL
    let communityGuidelines: URL
    let promotionTerms: URL
    let retention: URL
    let processors: URL
    let minors: URL
    let support: URL
}

final class LegalConsentService {
    static let shared = LegalConsentService()

    private let functions: Functions

    init(functions: Functions = Functions.functions(region: "us-central1")) {
        self.functions = functions
    }

    func fetchManifest() async throws -> LegalPolicyManifest {
        let data = try await call(name: "getLegalPolicyManifest", data: [:])
        return LegalPolicyManifest(
            version: try requiredString("version", in: data),
            terms: try requiredURL("terms", in: data),
            privacy: try requiredURL("privacy", in: data),
            communityGuidelines: try requiredURL("communityGuidelines", in: data),
            promotionTerms: try requiredURL("promotionTerms", in: data),
            retention: try requiredURL("retention", in: data),
            processors: try requiredURL("processors", in: data),
            minors: try requiredURL("minors", in: data),
            support: try requiredURL("support", in: data)
        )
    }

    func recordAcceptance(policyVersion: String) async throws {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        _ = try await call(
            name: "recordLegalAcceptance",
            data: [
                "policyVersion": policyVersion,
                "platform": "ios",
                "appVersion": version ?? "unknown",
                "locale": Locale.current.identifier,
            ]
        )
    }

    private func call(name: String, data: [String: Any]) async throws -> [String: Any] {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<[String: Any], Error>) in
            functions.httpsCallable(name).call(data) { result, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let data = result?.data as? [String: Any] {
                    continuation.resume(returning: data)
                } else {
                    continuation.resume(throwing: LegalConsentError.invalidManifest)
                }
            }
        }
    }

    private func requiredString(_ key: String, in data: [String: Any]) throws -> String {
        guard let value = data[key] as? String, !value.isEmpty else {
            throw LegalConsentError.invalidManifest
        }
        return value
    }

    private func requiredURL(_ key: String, in data: [String: Any]) throws -> URL {
        let value = try requiredString(key, in: data)
        guard let url = URL(string: value), url.scheme == "https" else {
            throw LegalConsentError.invalidManifest
        }
        return url
    }
}

enum LegalConsentError: LocalizedError {
    case invalidManifest

    var errorDescription: String? {
        "Kithe's approved legal policies are unavailable. Try again later."
    }
}
