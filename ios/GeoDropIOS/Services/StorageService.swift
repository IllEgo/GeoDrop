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

        let sanitizedExtension = fileExtension.trimmingCharacters(in: .whitespacesAndNewlines)
        let identifier = UUID().uuidString
        let path = "drops/\(userId)/\(identifier).\(sanitizedExtension)"
        let ref = storage.reference(withPath: path)

        let metadata = StorageMetadata()
        metadata.contentType = mimeType

        return try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<(url: URL, path: String), Error>) in
            ref.putData(data, metadata: metadata) { _, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }
                ref.downloadURL { url, error in
                    if let error = error {
                        continuation.resume(throwing: error)
                    } else if let url = url {
                        continuation.resume(returning: (url: url, path: path))
                    } else {
                        continuation.resume(
                            throwing: NSError(
                                domain: "GeoDropStorage",
                                code: -1,
                                userInfo: [NSLocalizedDescriptionKey: "Missing download URL"]
                            )
                        )
                    }
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
}
