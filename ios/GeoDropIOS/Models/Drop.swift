import Foundation
import FirebaseFirestore

struct Drop: Identifiable, Equatable, Codable {
    var id: String
    var text: String
    var description: String?
    var latitude: Double
    var longitude: Double
    var createdBy: String
    var createdAt: Date
    var dropperUsername: String?
    var isAnonymous: Bool
    var isDeleted: Bool
    var deletedAt: Date?
    var decayDays: Int?
    var groupCode: String?
    var dropType: DropType
    var businessId: String?
    var businessName: String?
    var contentType: DropContentType
    var mediaURL: URL?
    var mediaMimeType: String?
    var mediaData: String?
    var mediaStoragePath: String?
    var isNsfw: Bool
    var nsfwLabels: [String]
    var likeCount: Int
    var likedBy: [String: Bool]
    var reportCount: Int
    var reportedBy: [String: TimeInterval]
    var redemptionCode: String?
    var redemptionLimit: Int?
    var redemptionCount: Int
    var redeemedBy: [String: TimeInterval]
    var collectedBy: [String: Bool]

    init(
        id: String = "",
        text: String = "",
        description: String? = nil,
        latitude: Double = 0,
        longitude: Double = 0,
        createdBy: String = "",
        createdAt: Date = Date(timeIntervalSince1970: 0),
        dropperUsername: String? = nil,
        isAnonymous: Bool = false,
        isDeleted: Bool = false,
        deletedAt: Date? = nil,
        decayDays: Int? = nil,
        groupCode: String? = nil,
        dropType: DropType = .community,
        businessId: String? = nil,
        businessName: String? = nil,
        contentType: DropContentType = .text,
        mediaURL: URL? = nil,
        mediaMimeType: String? = nil,
        mediaData: String? = nil,
        mediaStoragePath: String? = nil,
        isNsfw: Bool = false,
        nsfwLabels: [String] = [],
        likeCount: Int = 0,
        likedBy: [String: Bool] = [:],
        reportCount: Int = 0,
        reportedBy: [String: TimeInterval] = [:],
        redemptionCode: String? = nil,
        redemptionLimit: Int? = nil,
        redemptionCount: Int = 0,
        redeemedBy: [String: TimeInterval] = [:],
        collectedBy: [String: Bool] = [:]
    ) {
        self.id = id
        self.text = text
        self.description = description
        self.latitude = latitude
        self.longitude = longitude
        self.createdBy = createdBy
        self.createdAt = createdAt
        self.dropperUsername = dropperUsername
        self.isAnonymous = isAnonymous
        self.isDeleted = isDeleted
        self.deletedAt = deletedAt
        self.decayDays = decayDays
        self.groupCode = groupCode
        self.dropType = dropType
        self.businessId = businessId
        self.businessName = businessName
        self.contentType = contentType
        self.mediaURL = mediaURL
        self.mediaMimeType = mediaMimeType
        self.mediaData = mediaData
        self.mediaStoragePath = mediaStoragePath
        self.isNsfw = isNsfw
        self.nsfwLabels = nsfwLabels
        self.likeCount = likeCount
        self.likedBy = likedBy
        self.reportCount = reportCount
        self.reportedBy = reportedBy
        self.redemptionCode = redemptionCode
        self.redemptionLimit = redemptionLimit
        self.redemptionCount = redemptionCount
        self.redeemedBy = redeemedBy
        self.collectedBy = collectedBy
    }

    var coordinate: Coordinate {
        Coordinate(latitude: latitude, longitude: longitude)
    }

    var displayTitle: String {
        let baseTitle: String
        let desc = description?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        switch dropType {
        case .restaurantCoupon:
            baseTitle = text.nonEmpty ?? desc.nonEmpty ?? "Special offer"
        case .tourStop:
            baseTitle = text.nonEmpty ?? desc.nonEmpty ?? "Tour stop"
        case .community:
            switch contentType {
            case .text:
                baseTitle = text.nonEmpty ?? desc.nonEmpty ?? "(No message)"
            case .photo:
                baseTitle = text.nonEmpty ?? desc.nonEmpty ?? "Photo drop"
            case .audio:
                baseTitle = text.nonEmpty ?? desc.nonEmpty ?? "Audio drop"
            case .video:
                baseTitle = text.nonEmpty ?? desc.nonEmpty ?? "Video drop"
            }
        }

        guard !isAnonymous, let username = dropperUsername?.trimmingCharacters(in: .whitespaces), !username.isEmpty else {
            return baseTitle
        }
        let handle = username.hasPrefix("@") ? username : "@\(username)"
        return "\(handle) dropped \(baseTitle)"
    }

    var isExpired: Bool {
        guard let days = decayDays, days > 0 else { return false }
        let expiry = createdAt.addingTimeInterval(TimeInterval(days * 86_400))
        return expiry <= Date()
    }

    func requiresRedemption() -> Bool {
        dropType == .restaurantCoupon && !(redemptionCode?.isEmpty ?? true)
    }

    func isRedeemed(by userId: String?) -> Bool {
        guard let uid = userId, !uid.isEmpty else { return false }
        return redeemedBy[uid] != nil
    }

    func isLiked(by userId: String?) -> DropLikeStatus {
        guard let uid = userId, !uid.isEmpty else { return .none }
        return DropLikeStatus.from(raw: likedBy[uid])
    }
}

extension Drop {
    init?(document: DocumentSnapshot) {
        guard let data = document.data() else { return nil }
        
        let inlineData: String?
        if let string = data["mediaData"] as? String, !string.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            inlineData = string
        } else if let blob = data["mediaData"] as? Data, !blob.isEmpty {
            inlineData = blob.base64EncodedString()
        } else {
            inlineData = nil
        }
        self.init(
            id: document.documentID,
            text: data["text"] as? String ?? "",
            description: data["description"] as? String,
            latitude: data["lat"] as? Double ?? 0,
            longitude: data["lng"] as? Double ?? 0,
            createdBy: data["createdBy"] as? String ?? "",
            createdAt: (data["createdAt"] as? Timestamp)?.dateValue() ?? Date(timeIntervalSince1970: 0),
            dropperUsername: data["dropperUsername"] as? String ?? data["createdByUsername"] as? String,
            isAnonymous: data["isAnonymous"] as? Bool ?? false,
            isDeleted: data["isDeleted"] as? Bool ?? false,
            deletedAt: (data["deletedAt"] as? Timestamp)?.dateValue(),
            decayDays: data["decayDays"] as? Int ?? (data["decayDays"] as? NSNumber)?.intValue,
            groupCode: (data["groupCode"] as? String).flatMap { value in
                let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !trimmed.isEmpty else { return nil }
                return trimmed.caseInsensitiveCompare("PUBLIC") == .orderedSame ? nil : trimmed.uppercased()
            },
            dropType: DropType(rawValue: (data["dropType"] as? String ?? "").uppercased()) ?? .community,
            businessId: data["businessId"] as? String,
            businessName: data["businessName"] as? String,
            contentType: DropContentType(rawValue: (data["contentType"] as? String ?? "").uppercased()) ?? .text,
            mediaURL: (data["mediaUrl"] as? String).flatMap(URL.init(string:)),
            mediaMimeType: data["mediaMimeType"] as? String,
            mediaData: inlineData,
            mediaStoragePath: data["mediaStoragePath"] as? String,
            isNsfw: data["isNsfw"] as? Bool ?? false,
            nsfwLabels: data["nsfwLabels"] as? [String] ?? [],
            likeCount: (data["likeCount"] as? NSNumber)?.intValue ?? 0,
            likedBy: data["likedBy"] as? [String: Bool] ?? [:],
            reportCount: (data["reportCount"] as? NSNumber)?.intValue ?? 0,
            reportedBy: data["reportedBy"] as? [String: TimeInterval] ?? [:],
            redemptionCode: data["redemptionCode"] as? String,
            redemptionLimit: (data["redemptionLimit"] as? NSNumber)?.intValue,
            redemptionCount: (data["redemptionCount"] as? NSNumber)?.intValue ?? 0,
            redeemedBy: data["redeemedBy"] as? [String: TimeInterval] ?? [:],
            collectedBy: data["collectedBy"] as? [String: Bool] ?? [:]
        )
    }

    func toFirestoreData() -> [String: Any] {
        var data: [String: Any] = [
            "text": text,
            "lat": latitude,
            "lng": longitude,
            "createdBy": createdBy,
            "createdAt": Timestamp(date: createdAt),
            "isAnonymous": isAnonymous,
            "isDeleted": isDeleted,
            "dropType": dropType.rawValue,
            "contentType": contentType.rawValue,
            "isNsfw": isNsfw,
            "nsfwLabels": nsfwLabels,
            "likeCount": likeCount,
            "likedBy": likedBy,
            "reportCount": reportCount,
            "reportedBy": reportedBy,
            "redemptionCount": redemptionCount,
            "redeemedBy": redeemedBy,
            "collectedBy": collectedBy
        ]

        if let description { data["description"] = description }
        if let dropperUsername { data["dropperUsername"] = dropperUsername }
        if let decayDays { data["decayDays"] = decayDays }
        if let groupCode { data["groupCode"] = groupCode }
        if let businessId { data["businessId"] = businessId }
        if let businessName { data["businessName"] = businessName }
        if let mediaURL { data["mediaUrl"] = mediaURL.absoluteString }
        if let mediaMimeType { data["mediaMimeType"] = mediaMimeType }
        if let mediaStoragePath { data["mediaStoragePath"] = mediaStoragePath }
        if let mediaData { data["mediaData"] = mediaData }
        if let deletedAt { data["deletedAt"] = Timestamp(date: deletedAt) }
        if let redemptionCode { data["redemptionCode"] = redemptionCode }
        if let redemptionLimit { data["redemptionLimit"] = redemptionLimit }
        return data
    }
}

struct Coordinate: Hashable {
    var latitude: Double
    var longitude: Double
}

enum DropType: String, CaseIterable, Codable {
    case community = "COMMUNITY"
    case restaurantCoupon = "RESTAURANT_COUPON"
    case tourStop = "TOUR_STOP"
}

enum DropContentType: String, CaseIterable, Codable {
    case text = "TEXT"
    case photo = "PHOTO"
    case audio = "AUDIO"
    case video = "VIDEO"
}

enum DropLikeStatus: String, Codable {
    case none
    case liked

    static func from(raw: Any?) -> DropLikeStatus {
        switch raw {
        case let value as Bool:
            return value ? .liked : .none
        case let value as Int:
            return value != 0 ? .liked : .none
        case let value as NSNumber:
            return value.intValue != 0 ? .liked : .none
        case let value as String:
            let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            return ["liked", "like", "true", "1"].contains(normalized) ? .liked : .none
        default:
            return .none
        }
    }
}

private extension String {
    var nonEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
