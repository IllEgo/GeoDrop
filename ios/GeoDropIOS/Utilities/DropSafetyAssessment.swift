import Foundation

struct DropSafetyAssessment {
    let isNsfw: Bool
    let reasons: [String]
    let visionStatus: VisionApiStatus
}

enum VisionApiStatus {
    case ok
    case notEligible
    case notConfigured
    case error
}