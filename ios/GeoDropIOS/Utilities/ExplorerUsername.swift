import Foundation

enum ExplorerUsername {
    enum ValidationError: Error, LocalizedError {
        case tooShort
        case tooLong
        case invalidCharacters

        var errorDescription: String? {
            switch self {
            case .tooShort: return "Usernames must be at least 3 characters."
            case .tooLong: return "Usernames must be fewer than 32 characters."
            case .invalidCharacters: return "Only letters, numbers, underscores, and periods are allowed."
            }
        }
    }

    static func sanitize(_ raw: String) throws -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard trimmed.count >= 3 else { throw ValidationError.tooShort }
        guard trimmed.count <= 32 else { throw ValidationError.tooLong }
        let allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyz0123456789._")
        guard trimmed.rangeOfCharacter(from: allowed.inverted) == nil else {
            throw ValidationError.invalidCharacters
        }
        return trimmed
    }
}