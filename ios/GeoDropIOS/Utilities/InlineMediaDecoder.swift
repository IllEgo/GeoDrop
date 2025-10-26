import Foundation
#if canImport(UIKit)
import UIKit
#endif

enum InlineMediaDecoder {
    private static func payload(from base64String: String?) -> String? {
        guard let raw = base64String?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty else {
            return nil
        }
        if let range = raw.range(of: "base64,", options: .caseInsensitive) {
            return String(raw[range.upperBound...])
        }
        return raw
    }

    static func data(from base64String: String?) -> Data? {
        guard let payload = payload(from: base64String) else { return nil }
        return Data(base64Encoded: payload, options: .ignoreUnknownCharacters)
    }

    #if canImport(UIKit)
    static func image(from base64String: String?) -> UIImage? {
        guard let data = data(from: base64String) else { return nil }
        return UIImage(data: data)
    }
    #endif
}
