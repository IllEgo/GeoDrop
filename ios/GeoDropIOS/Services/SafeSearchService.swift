import Foundation
import FirebaseFunctions

final class SafeSearchService {
    private let minimumLikelihood: Likelihood
    private let functions: Functions

    init(
        minimumLikelihood: Likelihood = .likely,
        functions: Functions = Functions.functions()
    ) {
        self.minimumLikelihood = minimumLikelihood
        self.functions = functions
    }

    func assess(
        text: String?,
        contentType: DropContentType,
        mediaMimeType: String?,
        mediaData: String?,
        mediaUrl: String?
    ) async -> DropSafetyAssessment {
        _ = text
        let eligibleForVision =
            contentType == .photo &&
            ((mediaMimeType?.hasPrefix("image/") ?? false) ||
             !(mediaData?.isEmpty ?? true) ||
             !(mediaUrl?.isEmpty ?? true))

        guard eligibleForVision else {
            return DropSafetyAssessment(isNsfw: false, reasons: [], visionStatus: .notEligible)
        }

        do {
            if let callableResult = try await requestViaCallable(mediaData: mediaData) {
                return finalizeAssessment(result: callableResult)
            }
            return DropSafetyAssessment(
                isNsfw: false,
                reasons: [],
                visionStatus: .error
            )
        } catch {
            print("GeoDrop: Vision callable failed \(error)")
            return DropSafetyAssessment(isNsfw: false, reasons: [], visionStatus: .error)
        }
    }

    private func requestViaCallable(mediaData: String?) async throws -> VisionAssessment? {
        guard let payload = extractBase64Payload(mediaData) else { return nil }

        // EXPLICIT GENERIC TYPE ANNOTATION (fixes the T inference error)
        let dataMap: [String: Any] = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<[String: Any], Error>) in
            let callable = functions.httpsCallable("safeSearch")
            callable.call(["base64": payload]) { callableResult, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }
                if let map = callableResult?.data as? [String: Any] {
                    continuation.resume(returning: map)
                } else {
                    continuation.resume(returning: [:])
                }
            }
        }

        return parseCallableResponse(dataMap)
    }

    // MARK: - Helpers

    private func parseCallableResponse(_ data: [String: Any]) -> VisionAssessment? {
        var likelihoods: [SafeSearchCategory: Likelihood] = [:]
        for category in SafeSearchCategory.allCases {
            let raw = data[category.responseKey] as? String
            likelihoods[category] = Likelihood(from: raw)
        }
        return buildAssessment(from: likelihoods)
    }

    private func buildAssessment(from likelihoods: [SafeSearchCategory: Likelihood]) -> VisionAssessment {
        let flagged = likelihoods.filter { _, value in
            value.rank >= minimumLikelihood.rank
        }
        let labels = flagged.map { $0.key.displayName }.sorted()
        return VisionAssessment(isNsfw: !labels.isEmpty, labels: labels)
    }

    private func finalizeAssessment(result: VisionAssessment?) -> DropSafetyAssessment {
        guard let result = result else {
            return DropSafetyAssessment(isNsfw: false, reasons: [], visionStatus: .ok)
        }
        return DropSafetyAssessment(isNsfw: result.isNsfw, reasons: result.labels, visionStatus: .ok)
    }

    private func extractBase64Payload(_ mediaData: String?) -> String? {
        guard let trimmed = mediaData?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else { return nil }
        let payload = trimmed.split(separator: ",", maxSplits: 1, omittingEmptySubsequences: false).last.map(String.init)
        let sanitized = payload?.trimmingCharacters(in: .whitespacesAndNewlines)
        return sanitized?.isEmpty == false ? sanitized : nil
    }
}

// MARK: - Types

private struct VisionAssessment {
    let isNsfw: Bool
    let labels: [String]
}

enum SafeSearchCategory: CaseIterable {
    case adult, spoof, medical, violence, racy

    var responseKey: String {
        switch self {
        case .adult: return "adult"
        case .spoof: return "spoof"
        case .medical: return "medical"
        case .violence: return "violence"
        case .racy: return "racy"
        }
    }

    var displayName: String {
        switch self {
        case .adult: return "Adult content"
        case .spoof: return "Spoof"
        case .medical: return "Medical"
        case .violence: return "Violence"
        case .racy: return "Racy"
        }
    }
}

enum Likelihood: String {
    case unknown = "UNKNOWN"
    case veryUnlikely = "VERY_UNLIKELY"
    case unlikely = "UNLIKELY"
    case possible = "POSSIBLE"
    case likely = "LIKELY"
    case veryLikely = "VERY_LIKELY"

    init(from raw: String?) {
        self = Likelihood(rawValue: raw ?? "") ?? .unknown
    }

    var rank: Int {
        switch self {
        case .unknown: return 0
        case .veryUnlikely: return 1
        case .unlikely: return 2
        case .possible: return 3
        case .likely: return 4
        case .veryLikely: return 5
        }
    }
}
