import SwiftUI
import CoreLocation
import UserNotifications
import UIKit

struct MainBottomBar<AccountMenuContent: View>: View {
    @Binding var isAccountMenuPresented: Bool
    let accountMenu: AccountMenuContent
    let onAccountTapped: () -> Void
    let onDropTapped: () -> Void
    let onGroupsTapped: () -> Void
    let isDropActive: Bool
    let isGroupsMenuPresented: Bool
    let isDropEnabled: Bool
    let dropLabel: String
    let isDropInFlight: Bool

    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        VStack(spacing: 0) {
            Divider()
                .overlay(geoDropTheme.colors.outlineVariant.opacity(0.4))
            HStack(spacing: 12) {
                accountButton
                dropButton
                groupButton
            }
            .padding(.horizontal, 16)
            .padding(.top, 4)
            .padding(.bottom, 8)
        }
        .background(geoDropTheme.colors.surface.ignoresSafeArea(edges: .bottom))
    }

    private var accountButton: some View {
        let label = BarItemLabel(
            iconName: "person.crop.circle",
            title: "Profile",
            isSelected: isAccountMenuPresented,
            theme: geoDropTheme
        )
        let button = Button(action: onAccountTapped) {
            label
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)

        if #available(iOS 16.0, *) {
            return AnyView(
                button
                    .popover(isPresented: $isAccountMenuPresented, arrowEdge: .bottom) {
                        if #available(iOS 16.4, *) {
                            accountMenu
                                .presentationCompactAdaptation(.none)
                        } else {
                            accountMenu
                        }
                    }
            )
        } else {
            return AnyView(button)
        }
    }

    private var groupButton: some View {
        Button(action: onGroupsTapped) {
            BarItemLabel(
                iconName: "person.3.fill",
                title: "Groups",
                isSelected: isGroupsMenuPresented,
                theme: geoDropTheme
            )
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
    }

    private var dropButton: some View {
        Button(action: onDropTapped) {
            BarItemLabel(
                iconName: "mappin.and.ellipse",
                title: dropLabel,
                isSelected: isDropActive || isDropInFlight,
                theme: geoDropTheme
            )
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .disabled(!isDropEnabled || isDropInFlight)
        .opacity(isDropEnabled ? 1 : 0.7)
    }
}

private struct BarItemLabel: View {
    let iconName: String
    let title: String
    let isSelected: Bool
    let theme: GeoDropTheme

    var body: some View {
        VStack(spacing: 3) {
            Image(systemName: iconName)
                .font(.system(size: 15, weight: .semibold))
            Text(title)
                .font(.caption.weight(.semibold))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)
        .foregroundColor(isSelected ? theme.colors.primary : theme.colors.onSurface)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(isSelected ? theme.colors.primary.opacity(0.12) : Color.clear)
        )
    }
}

struct AccountMenuView: View {
    let onDismiss: () -> Void

    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if let session = currentSession {
                accountHeader(for: session)
                accountActions(for: session)
            } else {
                guestActions
            }
        }
        .padding(16)
        .frame(maxWidth: 280)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(geoDropTheme.colors.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(geoDropTheme.colors.outlineVariant.opacity(0.4))
        )
        .shadow(color: Color.black.opacity(0.12), radius: 12, x: 0, y: 6)
    }

    private var currentSession: AppViewModel.UserSession? {
        if case let .signedIn(session) = viewModel.authState {
            return session
        }
        return nil
    }

    @ViewBuilder
    private func accountHeader(for session: AppViewModel.UserSession) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            if let name = session.profile.displayName, !name.isEmpty {
                Text(name)
                    .font(.headline)
                    .foregroundColor(geoDropTheme.colors.onSurface)
            }
            if let email = session.user.email, !email.isEmpty {
                Text(email)
                    .font(.footnote)
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            }
        }
    }

    @ViewBuilder
    private func accountActions(for session: AppViewModel.UserSession) -> some View {
        let canParticipate = viewModel.userMode?.canParticipate == true
        VStack(alignment: .leading, spacing: 12) {
            if canParticipate {
                MenuActionButton(
                    title: "Notification radius (\(Int(viewModel.notificationRadiusMeters.rounded())) m)",
                    systemImage: "map",
                    theme: geoDropTheme
                ) {
                    onDismiss()
                    viewModel.openNotificationRadiusSettings()
                }

                MenuActionButton(
                    title: "Your data",
                    systemImage: "lock.doc",
                    theme: geoDropTheme
                ) {
                    onDismiss()
                    viewModel.openAccountData()
                }

            }

            MenuActionButton(
                title: "Sign out",
                systemImage: "rectangle.portrait.and.arrow.right",
                theme: geoDropTheme,
                tint: geoDropTheme.colors.tertiary
            ) {
                onDismiss()
                viewModel.signOut()
            }
        }
    }

    private var guestActions: some View {
        VStack(alignment: .leading, spacing: 12) {
            MenuActionButton(
                title: "Sign in for full participation",
                systemImage: "checkmark.circle",
                theme: geoDropTheme
            ) {
                onDismiss()
                viewModel.beginAuthentication(for: .explorer)
            }

            MenuActionButton(
                title: "Sign in or create a business account",
                systemImage: "briefcase",
                theme: geoDropTheme
            ) {
                onDismiss()
                viewModel.beginAuthentication(for: .business)
            }
        }
    }
}

private struct MenuActionButton: View {
    let title: String
    let systemImage: String
    let theme: GeoDropTheme
    var tint: Color?
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: systemImage)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(tint ?? theme.colors.primary)
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(theme.colors.onSurface)
                Spacer()
            }
            .padding(.vertical, 10)
            .padding(.horizontal, 12)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(theme.colors.surfaceVariant)
            )
        }
        .buttonStyle(.plain)
    }
}

struct GroupMenuSheet: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let content = VStack(spacing: 12) {
            Capsule()
                .fill(geoDropTheme.colors.outlineVariant.opacity(0.5))
                .frame(width: 48, height: 5)
                .padding(.top, 8)

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    MenuActionButton(
                        title: "Create or subscribe",
                        systemImage: "plus.circle",
                        theme: geoDropTheme
                    ) {
                        viewModel.openGroupManagement()
                    }

                    Divider()
                        .background(geoDropTheme.colors.outlineVariant.opacity(0.3))

                    groupSelectionSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
        }
        .background(geoDropTheme.colors.surface.ignoresSafeArea())

        if #available(iOS 16.0, *) {
            AnyView(content.presentationDetents([.medium, .large]))
        } else {
            AnyView(content)
        }
    }

    private var activeSelection: String? {
        guard let selected = viewModel.selectedGroupCode else { return nil }
        return viewModel.groups.contains(where: { $0.code == selected }) ? selected : nil
    }

    @ViewBuilder
    private var groupSelectionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            GroupMenuRow(
                title: "All groups",
                isSelected: activeSelection == nil,
                theme: geoDropTheme
            ) {
                viewModel.selectedGroupCode = nil
                dismiss()
            }

            Divider()
                .background(geoDropTheme.colors.outlineVariant.opacity(0.3))

            groupList(title: "Owned groups", groups: ownedGroups)

            Divider()
                .background(geoDropTheme.colors.outlineVariant.opacity(0.3))

            groupList(title: "Subscribed groups", groups: subscribedGroups)
        }
    }

    private func groupList(title: String, groups: [GroupMembership]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(geoDropTheme.colors.primary)

            if groups.isEmpty {
                Text(emptyMessage(for: title))
                    .font(.footnote)
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                    .padding(.vertical, 4)
            } else {
                ForEach(groups) { membership in
                    GroupMenuRow(
                        title: membership.code,
                        isSelected: activeSelection == membership.code,
                        theme: geoDropTheme
                    ) {
                        viewModel.selectedGroupCode = membership.code
                        dismiss()
                    }
                }
            }
        }
    }

    private func emptyMessage(for title: String) -> String {
        if title == "Owned groups" {
            return "You haven't created any groups yet."
        }
        return "You're not subscribed to any groups yet."
    }

    private var ownedGroups: [GroupMembership] {
        viewModel.groups
            .filter { $0.role == .owner }
            .sorted { $0.code < $1.code }
    }

    private var subscribedGroups: [GroupMembership] {
        viewModel.groups
            .filter { $0.role != .owner }
            .sorted { $0.code < $1.code }
    }
}

private struct GroupMenuRow: View {
    let title: String
    let isSelected: Bool
    let theme: GeoDropTheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(theme.colors.primary)
                } else {
                    Image(systemName: "circle")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(theme.colors.outlineVariant)
                }
                Text(title)
                    .font(.subheadline)
                    .foregroundColor(theme.colors.onSurface)
                Spacer()
            }
            .padding(.vertical, 10)
            .padding(.horizontal, 12)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isSelected ? theme.colors.primary.opacity(0.12) : theme.colors.surfaceVariant)
            )
        }
        .buttonStyle(.plain)
    }
}

struct NotificationRadiusSheet: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme
    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase
    @ObservedObject private var locationService = LocationService.shared
    @ObservedObject private var messagingService = MessagingService.shared

    private static let step: Double = 50

    @State private var sliderValue: Double
    @State private var setupPrompt: NearbyAlertSetupPrompt?
    @State private var pendingAlertEnablement = false
    let onSave: (Double) -> Void

    init(initialRadius: Double, onSave: @escaping (Double) -> Void) {
        _sliderValue = State(initialValue: Self.clamp(initialRadius))
        self.onSave = onSave
    }

    var body: some View {
        Group {
            if #available(iOS 16.0, *) {
                NavigationStack { content }
                    .navigationTitle("Nearby notification radius")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar { toolbarContent }
                    .presentationDetents([.medium])
            } else {
                NavigationView { content }
                    .navigationBarTitle("Nearby notification radius", displayMode: .inline)
                    .navigationBarItems(leading: cancelButton, trailing: saveButton)
            }
        }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 24) {
            Toggle(
                isOn: Binding(
                    get: { viewModel.nearbyAlertsEnabled },
                    set: { enabled in
                        if enabled {
                            beginAlertEnablement()
                        } else {
                            pendingAlertEnablement = false
                            viewModel.setNearbyAlertsEnabled(false)
                        }
                    }
                )
            ) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Nearby alerts")
                        .font(.headline)
                    Text(
                        viewModel.nearbyAlertsEnabled
                            ? "On — GeoDrop can alert you within your chosen radius."
                            : "Off — Nearby browsing still works without alerts."
                    )
                    .font(.footnote)
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                }
            }

            Text("Choose how close a drop should be before we alert you.")
                .font(geoDropTheme.typography.body)
                .foregroundColor(geoDropTheme.colors.onSurface)

            Text("\(Int(sliderValue)) meters")
                .font(geoDropTheme.typography.title.weight(.semibold))
                .foregroundColor(geoDropTheme.colors.onSurface)

            Slider(
                value: Binding(
                    get: { sliderValue },
                    set: { newValue in
                        let snapped = (newValue / Self.step).rounded() * Self.step
                        sliderValue = Self.clamp(snapped)
                    }
                ),
                in: NotificationPreferences.minRadius...NotificationPreferences.maxRadius,
                step: Self.step
            )
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(geoDropTheme.colors.background)
        .onAppear {
            refreshPermissionState(resumeSetup: false)
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                refreshPermissionState(resumeSetup: pendingAlertEnablement)
            }
        }
        .alert(item: $setupPrompt) { prompt in
            alert(for: prompt)
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) { cancelButton }
        ToolbarItem(placement: .confirmationAction) { saveButton }
    }

    private var cancelButton: some View {
        Button("Cancel") { dismiss() }
    }

    private var saveButton: some View {
        Button("Save") {
            onSave(sliderValue)
            dismiss()
        }
    }

    private static func clamp(_ value: Double) -> Double {
        let minValue = NotificationPreferences.minRadius
        let maxValue = NotificationPreferences.maxRadius
        return min(maxValue, max(minValue, value))
    }

    private func beginAlertEnablement() {
        pendingAlertEnablement = true
        refreshPermissionState(resumeSetup: true)
    }

    private func refreshPermissionState(resumeSetup: Bool) {
        locationService.refreshAuthorizationStatus()
        messagingService.refreshAuthorizationStatus { _ in
            if resumeSetup {
                continueAlertEnablement()
            } else if viewModel.nearbyAlertsEnabled && !allAlertPermissionsGranted {
                viewModel.setNearbyAlertsEnabled(false)
            }
        }
    }

    private var allAlertPermissionsGranted: Bool {
        locationService.authorizationStatus == .authorizedAlways &&
            notificationPermissionGranted
    }

    private var notificationPermissionGranted: Bool {
        switch messagingService.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        default:
            return false
        }
    }

    private func continueAlertEnablement() {
        switch locationService.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            break
        case .notDetermined:
            setupPrompt = .foregroundLocationRequired
            return
        case .denied, .restricted:
            setupPrompt = .foregroundLocationSettings
            return
        @unknown default:
            setupPrompt = .foregroundLocationRequired
            return
        }

        switch messagingService.authorizationStatus {
        case .notDetermined:
            setupPrompt = .notificationRationale
            return
        case .denied:
            setupPrompt = .notificationSettings
            return
        case .authorized, .provisional, .ephemeral:
            messagingService.requestAuthorization { granted in
                if granted {
                    continueBackgroundLocationEnablement()
                } else {
                    setupPrompt = .notificationSettings
                }
            }
        @unknown default:
            setupPrompt = .notificationSettings
        }
    }

    private func continueBackgroundLocationEnablement() {
        switch locationService.authorizationStatus {
        case .authorizedAlways:
            pendingAlertEnablement = false
            setupPrompt = nil
            viewModel.setNearbyAlertsEnabled(true)
        case .authorizedWhenInUse:
            setupPrompt = .backgroundLocationRationale
        case .notDetermined:
            setupPrompt = .foregroundLocationRequired
        case .denied, .restricted:
            setupPrompt = .foregroundLocationSettings
        @unknown default:
            setupPrompt = .backgroundLocationSettings
        }
    }

    private func requestNotificationAuthorization() {
        messagingService.requestAuthorization { granted in
            if granted {
                continueBackgroundLocationEnablement()
            } else {
                setupPrompt = .notificationSettings
            }
        }
    }

    private func requestBackgroundLocationAuthorization() {
        locationService.requestAlwaysAuthorization { status in
            if status == .authorizedAlways {
                pendingAlertEnablement = false
                setupPrompt = nil
                viewModel.setNearbyAlertsEnabled(true)
            } else {
                setupPrompt = .backgroundLocationSettings
            }
        }
    }

    private func openApplicationSettings() {
        guard let settingsURL = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(settingsURL)
    }

    private func cancelAlertEnablement() {
        pendingAlertEnablement = false
        setupPrompt = nil
        viewModel.setNearbyAlertsEnabled(false)
    }

    private func returnToNearby() {
        pendingAlertEnablement = false
        setupPrompt = nil
        viewModel.setNearbyAlertsEnabled(false)
        viewModel.setExplorerDestination(.nearby)
        dismiss()
    }

    private func alert(for prompt: NearbyAlertSetupPrompt) -> Alert {
        switch prompt {
        case .foregroundLocationRequired:
            return Alert(
                title: Text("Set up location from Nearby first"),
                message: Text(
                    "Nearby alerts need location access. Return to Nearby and choose “Use my location” so the request stays connected to discovery; browsing remains available if you decline."
                ),
                primaryButton: .default(Text("Go to Nearby"), action: returnToNearby),
                secondaryButton: .cancel(cancelAlertEnablement)
            )
        case .foregroundLocationSettings:
            return Alert(
                title: Text("Location is off"),
                message: Text(
                    "Turn on GeoDrop location access in Settings, then return to finish Nearby alert setup. Browsing still works without location."
                ),
                primaryButton: .default(Text("Open Settings"), action: openApplicationSettings),
                secondaryButton: .cancel(cancelAlertEnablement)
            )
        case .notificationRationale:
            return Alert(
                title: Text("Allow Nearby notifications?"),
                message: Text(
                    "GeoDrop will use alerts only after you enable this feature, to tell you when a drop is within your chosen radius. You can turn alerts off at any time."
                ),
                primaryButton: .default(Text("Continue"), action: requestNotificationAuthorization),
                secondaryButton: .cancel(cancelAlertEnablement)
            )
        case .notificationSettings:
            return Alert(
                title: Text("Notifications are off"),
                message: Text(
                    "Turn on GeoDrop notifications in Settings to finish Nearby alert setup. Nearby browsing still works without notifications."
                ),
                primaryButton: .default(Text("Open Settings"), action: openApplicationSettings),
                secondaryButton: .cancel(cancelAlertEnablement)
            )
        case .backgroundLocationRationale:
            return Alert(
                title: Text("Allow alerts while GeoDrop is closed?"),
                message: Text(
                    "Background location is used only for Nearby alerts you are enabling now. Choosing Always lets GeoDrop detect nearby drops without keeping the app open."
                ),
                primaryButton: .default(Text("Continue"), action: requestBackgroundLocationAuthorization),
                secondaryButton: .cancel(cancelAlertEnablement)
            )
        case .backgroundLocationSettings:
            return Alert(
                title: Text("Background location is off"),
                message: Text(
                    "In Settings, set Location to Always to receive Nearby alerts while GeoDrop is closed. You can keep using Nearby without it."
                ),
                primaryButton: .default(Text("Open Settings"), action: openApplicationSettings),
                secondaryButton: .cancel(cancelAlertEnablement)
            )
        }
    }
}

private enum NearbyAlertSetupPrompt: String, Identifiable {
    case foregroundLocationRequired
    case foregroundLocationSettings
    case notificationRationale
    case notificationSettings
    case backgroundLocationRationale
    case backgroundLocationSettings

    var id: String { rawValue }
}
