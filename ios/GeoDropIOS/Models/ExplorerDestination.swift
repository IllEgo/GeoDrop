import Foundation

enum ExplorerDestination: String, CaseIterable, Identifiable {
    case nearby
    case myDrops
    case collected

    var id: String { rawValue }

    var title: String {
        switch self {
        case .nearby:
            return "Nearby"
        case .myDrops:
            return "My Drops"
        case .collected:
            return "Collected"
        }
    }

    var systemImageName: String {
        switch self {
        case .nearby:
            return "map"
        case .myDrops:
            return "tray.full"
        case .collected:
            return "bookmark"
        }
    }

    var requiresAuthentication: Bool {
        switch self {
        case .nearby:
            return false
        case .myDrops, .collected:
            return true
        }
    }

    func restrictionMessage(for mode: AppViewModel.UserMode?) -> String {
        switch self {
        case .nearby:
            return ""
        case .myDrops:
            if mode == .guest || mode == nil {
                return "Sign in to manage your drops."
            }
            return "Upgrade to a full account to manage your drops."
        case .collected:
            if mode == .guest || mode == nil {
                return "Sign in to view collected drops."
            }
            return "Upgrade to a full account to view collected drops."
        }
    }

    var emptyStateIcon: String {
        switch self {
        case .nearby:
            return "mappin.and.ellipse"
        case .myDrops:
            return "tray"
        case .collected:
            return "bookmark.slash"
        }
    }

    var emptyStateTitle: String {
        switch self {
        case .nearby:
            return "No drops nearby"
        case .myDrops:
            return "No drops yet"
        case .collected:
            return "No collected drops"
        }
    }

    var emptyStateMessage: String {
        switch self {
        case .nearby:
            return "Try refreshing or explore another group to discover more drops."
        case .myDrops:
            return "Drops you create will appear here for easy access."
        case .collected:
            return "Collect drops to save them for later."
        }
    }
}
