import Foundation
import FirebaseFunctions

final class SafeSearchService {
    private let apiKey: String
    private let endpoint: URL
    private let minimumLikelihood: Likelihood
    private let functions: Functions

    init(
        apiKey: String,
        endpoint: String = "https://vision.googleapis.com/v1/images:annotate",
        minimumLikelihood: Likelihood = .likely,
        functions: Functions = Functions.functions()
    ) {
        self.apiKey = apiKey
        self.endpoint = URL(string: endpoint) ?? URL(string: "https://vision.googleapis.com/v1/images:annotate")!
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

        // Try direct Vision API call when an API key is configured
        if !apiKey.isEmpty {
            do {
                if let result = try await requestSafeSearch(mediaData: mediaData, mediaUrl: mediaUrl) {
                    return finalizeAssessment(result: result)
                }
            } catch {
                print("GeoDrop: Vision API request failed \(error)")
            }
        }

        // Fallback to callable
        do {
            if let callableResult = try await requestViaCallable(mediaData: mediaData) {
                return finalizeAssessment(result: callableResult)
            }
            return DropSafetyAssessment(
                isNsfw: false,
                reasons: [],
                visionStatus: apiKey.isEmpty ? .notConfigured : .error
            )
        } catch {
            print("GeoDrop: Vision callable failed \(error)")
            return DropSafetyAssessment(isNsfw: false, reasons: [], visionStatus: .error)
        }
    }

    // MARK: - Vision (HTTP)

    private func requestSafeSearch(mediaData: String?, mediaUrl: String?) async throws -> VisionAssessment? {
        guard let body = buildRequestBody(mediaData: mediaData, mediaUrl: mediaUrl) else { return nil }
        guard var components = URLComponents(url: endpoint, resolvingAgainstBaseURL: false) else { return nil }
        var queryItems = components.queryItems ?? []
        queryItems.append(URLQueryItem(name: "key", value: apiKey))
        components.queryItems = queryItems
        guard let url = components.url else { return nil }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 10
        request.setValue("application/json; charset=UTF-8", forHTTPHeaderField: "Content-Type")
        request.httpBody = body

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { return nil }
        guard (200...299).contains(http.statusCode) else {
            let message = String(data: data, encoding: .utf8) ?? ""
            throw NSError(domain: "GeoDropVision", code: http.statusCode, userInfo: [NSLocalizedDescriptionKey: message])
        }
        return try parseVisionResponse(data: data)
    }

    // MARK: - Vision (Callable)

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

    private func buildRequestBody(mediaData: String?, mediaUrl: String?) -> Data? {
        var imagePayload: [String: Any]?
        if let base64 = extractBase64Payload(mediaData) {
            imagePayload = ["content": base64]
        } else if let url = mediaUrl?.trimmingCharacters(in: .whitespacesAndNewlines), !url.isEmpty {
            imagePayload = ["source": ["imageUri": url]]
        }
        guard let payload = imagePayload else { return nil }

        let request: [String: Any] = [
            "requests": [[
                "image": payload,
                "features": [["type": "SAFE_SEARCH_DETECTION"]]
            ]]
        ]
        return try? JSONSerialization.data(withJSONObject: request, options: [])
    }

    private func parseVisionResponse(data: Data) throws -> VisionAssessment? {
        let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
        guard let root = json,
              let responses = root["responses"] as? [[String: Any]],
              let first = responses.first else {
            return nil
        }

        if let error = first["error"] as? [String: Any], let message = error["message"] as? String {
            throw NSError(domain: "GeoDropVision", code: -1, userInfo: [NSLocalizedDescriptionKey: message])
        }

        guard let annotation = first["safeSearchAnnotation"] as? [String: Any] else {
            return nil
        }

        var likelihoods: [SafeSearchCategory: Likelihood] = [:]
        for category in SafeSearchCategory.allCases {
            let raw = annotation[category.responseKey] as? String
            likelihoods[category] = Likelihood(from: raw)
        }
        return buildAssessment(from: likelihoods)
    }

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
