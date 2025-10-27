import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

struct StorageAsyncImage<Content: View>: View {
    private let storagePath: String?
    private let url: URL?
    private let maxSize: Int64
    private let transaction: Transaction
    private let content: (AsyncImagePhase) -> Content

    @State private var phase: AsyncImagePhase = .empty
    @State private var loadTask: Task<Void, Never>? = nil

    init(
        storagePath: String?,
        url: URL?,
        maxSize: Int64 = 15 * 1024 * 1024,
        transaction: Transaction = Transaction(),
        @ViewBuilder content: @escaping (AsyncImagePhase) -> Content
    ) {
        self.storagePath = storagePath
        self.url = url
        self.maxSize = maxSize
        self.transaction = transaction
        self.content = content
    }

    var body: some View {
        content(phase)
            .task(id: loadIdentifier) {
                await startLoading()
            }
            .onDisappear {
                loadTask?.cancel()
                loadTask = nil
            }
    }

    private var loadIdentifier: String? {
        storagePath ?? url?.absoluteString
    }

    private func startLoading() async {
        loadTask?.cancel()
        let task = Task {
            await updatePhase(.empty)
            do {
                if let image = try await loadFromStorage() {
                    await updatePhase(.success(image))
                } else if let image = try await loadFromURL() {
                    await updatePhase(.success(image))
                } else {
                    throw StorageAsyncImageError.missingSource
                }
            } catch is CancellationError {
                // Ignore cancellations
            } catch {
                await updatePhase(.failure(error))
            }
        }
        loadTask = task
        _ = await task.value
    }

    private func loadFromStorage() async throws -> Image? {
        guard let path = storagePath else { return nil }
        if let cached = StorageImageCache.shared.image(for: path) {
            return Image(uiImage: cached)
        }

        let data = try await StorageService.shared.fetchData(at: path, maxSize: maxSize)
        try Task.checkCancellation()
        guard let image = StorageImageCache.shared.storeImageData(data, for: path) else {
            throw StorageAsyncImageError.decodingFailed
        }
        return Image(uiImage: image)
    }

    private func loadFromURL() async throws -> Image? {
        guard let url = url else { return nil }
        let key = url.absoluteString
        if let cached = StorageImageCache.shared.image(for: key) {
            return Image(uiImage: cached)
        }

        let request = URLRequest(url: url, cachePolicy: .reloadRevalidatingCacheData)
        let (data, response) = try await URLSession.shared.data(for: request)
        try Task.checkCancellation()
        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw URLError(.badServerResponse)
        }
        guard let image = StorageImageCache.shared.storeImageData(data, for: key) else {
            throw StorageAsyncImageError.decodingFailed
        }
        return Image(uiImage: image)
    }

    @MainActor
    private func updatePhase(_ newPhase: AsyncImagePhase) {
        if let animation = transaction.animation {
            withAnimation(animation) {
                phase = newPhase
            }
        } else {
            phase = newPhase
        }
    }
}

private enum StorageAsyncImageError: Error {
    case missingSource
    case decodingFailed
}

private final class StorageImageCache {
    static let shared = StorageImageCache()

    private let cache = NSCache<NSString, UIImage>()

    func image(for key: String) -> UIImage? {
        cache.object(forKey: key as NSString)
    }

    func storeImageData(_ data: Data, for key: String) -> UIImage? {
        #if canImport(UIKit)
        guard let image = UIImage(data: data) else { return nil }
        cache.setObject(image, forKey: key as NSString)
        return image
        #else
        return nil
        #endif
    }
}
