import SwiftUI

struct FormSectionHeader: View {
    let title: String
    var subtitle: String?
    var systemImage: String?

    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                if let systemImage {
                    Image(systemName: systemImage)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(geoDropTheme.colors.primary)
                        .frame(width: 22, height: 22)
                }

                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(geoDropTheme.colors.onSurface)
            }

            if let subtitle = subtitle?.trimmingCharacters(in: .whitespacesAndNewlines), !subtitle.isEmpty {
                Text(subtitle)
                    .font(.footnote)
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 4)
    }
}
