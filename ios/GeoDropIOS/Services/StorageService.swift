import Foundation
import FirebaseStorage

final class StorageService {
    static let shared = StorageService()
    private let storage = Storage.storage()
    private init() {}

    func uploadMedia(
        data: Data,
        mimeType: String,
        fileExtension: String,
        userId: String
    ) async throws -> (url: URL, path: String) {
        guard PilotFeatureFlags.shared.creationEnabled,
              PilotFeatureFlags.shared.mediaEnabled else {
            throw NSError(
                domain: "GeoDrop.FeatureFlags",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Media uploads are disabled for this release."]
            )
        }
        let sanitizedExtension = fileExtension.trimmingCharacters(in: .whitespacesAndNewlines)
        let identifier = UUID().uuidString
        let path = "drops/\(userId)/\(identifier).\(sanitizedExtension)"
        let ref = storage.reference(withPath: path)

        let metadata = StorageMetadata()
        metadata.contentType = mimeType
        metadata.customMetadata = [
            "ownerId": userId,
            "accessLevel": "PRIVATE",
            "safetyStatus": "PENDING"
        ]

        return try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<(url: URL, path: String), Error>) in
            ref.putData(data, metadata: metadata) { _, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }
                if let url = Self.rulesCheckedMediaURL(bucket: ref.bucket, path: path) {
                    continuation.resume(returning: (url: url, path: path))
                } else {
                    continuation.resume(
                        throwing: NSError(
                            domain: "GeoDropStorage",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "Couldn't build media URL"]
                        )
                    )
                }
            }
        }
    }
    
    func fetchData(at path: String, maxSize: Int64 = 15 * 1024 * 1024) async throws -> Data {
        guard PilotFeatureFlags.shared.mediaEnabled else {
            throw NSError(
                domain: "GeoDrop.FeatureFlags",
                code: 2,
                userInfo: [NSLocalizedDescriptionKey: "Media access is disabled for this release."]
            )
        }
        let ref = storage.reference(withPath: path)
        return try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Data, Error>) in
            ref.getData(maxSize: maxSize) { data, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let data = data {
                    continuation.resume(returning: data)
                } else {
                    continuation.resume(
                        throwing: NSError(
                            domain: "GeoDropStorage",
                            code: -2,
                            userInfo: [NSLocalizedDescriptionKey: "Missing storage data"]
                        )
                    )
                }
            }
        }
    }

    func delete(path: String) async {
        guard !path.isEmpty else { return }
        let ref = storage.reference(withPath: path)
        do {
            _ = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                ref.delete { error in
                    if let error = error {
                        continuation.resume(throwing: error)
                    } else {
                        continuation.resume(returning: ())
                    }
                }
            }
        } catch {
            print("GeoDrop: Failed to delete storage path \(path): \(error)")
        }
    }

    private static func rulesCheckedMediaURL(bucket: String, path: String) -> URL? {
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-._~")
        guard let encodedBucket = bucket.addingPercentEncoding(withAllowedCharacters: allowed),
              let encodedPath = path.addingPercentEncoding(withAllowedCharacters: allowed) else {
            return nil
        }
        return URL(
            string: "https://firebasestorage.googleapis.com/v0/b/\(encodedBucket)/o/\(encodedPath)?alt=media"
        )
    }
}
