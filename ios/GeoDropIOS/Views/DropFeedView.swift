import SwiftUI
import CoreLocation
import AVKit
import UIKit

struct DropFeedView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme
    @State private var mapCameraState = GoogleMapCameraState(
        latitude: Self.defaultCoordinate.latitude,
        longitude: Self.defaultCoordinate.longitude,
        zoom: Self.defaultZoom
    )
    @State private var selectedDropID: Drop.ID?
    @State private var selectedSortOption: DropSortOption = .newest
    @State private var mapHeightFraction: CGFloat = 0.45
    @State private var dragStartFraction: CGFloat?
    @State private var shouldAnimateCamera = false
    @State private var isRestrictionAlertPresented = false
    
    private static let defaultCoordinate = CLLocationCoordinate2D(latitude: 37.7749, longitude: -122.4194)
    private static let defaultZoom: Float = GoogleMapCameraState.defaultZoom

    var body: some View {
        GeoDropNavigationContainer {
            VStack(spacing: 0) {
                if shouldShowGroupPrompt {
                    VStack(spacing: 16) {
                        Text("Join a group to start discovering drops.")
                            .multilineTextAlignment(.center)
                        Button("Join group") { viewModel.openGroupManagement() }
                            .buttonStyle(.borderedProminent)
                    }
                    .padding()
                } else {
                    VStack(spacing: 0) {
                        GeometryReader { geometry in
                            resizableLayout(for: geometry)
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
        }
        .onAppear { updateSelection(for: displayedDrops) }
        .onChange(of: viewModel.drops) { _ in updateSelection(for: displayedDrops) }
        .onChange(of: viewModel.inventory) { _ in updateSelection(for: displayedDrops) }
        .onChange(of: viewModel.selectedExplorerDestination) { destination in
            updateSelection(for: viewModel.explorerDrops(for: destination))
        }
        .onChange(of: selectedSortOption) { _ in updateSelection(for: displayedDrops) }
        .onChange(of: viewModel.explorerRestrictionMessage) { message in
            isRestrictionAlertPresented = message != nil
        }
        .alert("Limited Access", isPresented: Binding(
            get: { isRestrictionAlertPresented },
            set: { newValue in
                isRestrictionAlertPresented = newValue
                if !newValue {
                    viewModel.explorerRestrictionMessage = nil
                }
            }
        )) {
            Button("OK", role: .cancel) { }
        } message: {
            Text(viewModel.explorerRestrictionMessage ?? "Sign in to continue.")
        }
    }
    
    @ViewBuilder
    private func resizableLayout(for geometry: GeometryProxy) -> some View {
        let totalHeight = geometry.size.height
        let minFraction: CGFloat = 0.25
        let maxFraction: CGFloat = 0.8
        let dividerHeight: CGFloat = 16
        let clampedFraction = min(max(mapHeightFraction, minFraction), maxFraction)
        let mapHeight = totalHeight * clampedFraction
        let listHeight = max(totalHeight - mapHeight - dividerHeight, 0)
        
        let resizingAnimation = Animation.interactiveSpring(response: 0.25, dampingFraction: 0.85, blendDuration: 0.2)

        let drag = DragGesture(minimumDistance: 0)
            .onChanged { value in
                if dragStartFraction == nil {
                    dragStartFraction = clampedFraction
                }
                let start = dragStartFraction ?? clampedFraction
                let translationFraction = value.translation.height / totalHeight
                let proposed = start + translationFraction
                let updatedFraction = min(max(proposed, minFraction), maxFraction)
                withAnimation(resizingAnimation) {
                    mapHeightFraction = updatedFraction
                }
            }
            .onEnded { value in
                let finalStart = dragStartFraction ?? clampedFraction
                dragStartFraction = nil
                let translationFraction = value.predictedEndTranslation.height / totalHeight
                let proposed = finalStart + translationFraction
                let finalFraction = min(max(proposed, minFraction), maxFraction)
                withAnimation(resizingAnimation) {
                    mapHeightFraction = finalFraction
                }
            }

        VStack(spacing: 0) {
            mapSection(height: mapHeight)

            dividerSection(height: dividerHeight)
                .gesture(drag)

            listSection(height: listHeight)
        }
    }

    @ViewBuilder
    private func mapSection(height: CGFloat) -> some View {
        ZStack(alignment: .top) {
            GoogleMapView(
                drops: displayedDrops,
                selectedDropID: $selectedDropID,
                cameraState: $mapCameraState,
                shouldAnimateCamera: $shouldAnimateCamera,
                onSelectDrop: { drop in focus(on: drop) }
            )
            VStack {
                destinationTabs
                    .padding(.horizontal, 12)
                    .padding(.top, 12)
                Spacer()
            }
        }
        .frame(height: height)
        .clipped()
    }

    @ViewBuilder
    private func dividerSection(height: CGFloat) -> some View {
        ZStack {
            geoDropTheme.colors.surface
            Capsule()
                .fill(geoDropTheme.colors.onSurfaceVariant.opacity(0.4))
                .frame(width: 48, height: 6)
        }
        .frame(height: height)
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private func listSection(height: CGFloat) -> some View {
        Group {
            if viewModel.isAuthLoading && viewModel.drops.isEmpty {
                loadingStateView
            } else if let message = viewModel.errorMessage {
                errorStateView(message: message)
            } else if displayedDrops.isEmpty {
                emptyStateView
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        sortHeader
                        ForEach(displayedDrops) { drop in
                            DropRowView(
                                drop: drop,
                                isSelected: drop.id == selectedDropID,
                                onSelect: { focus(on: drop) }
                            )
                            .padding(.horizontal)
                        }
                    }
                    .padding(.vertical, 12)
                }
            }
        }
        .frame(height: height)
        .background(geoDropTheme.colors.surface)
    }
    
    private var destinationTabs: some View {
        HStack(spacing: 6) {
            ForEach(ExplorerDestination.allCases) { destination in
                destinationButton(for: destination)
            }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 4)
        .background(.ultraThinMaterial, in: Capsule())
        .shadow(color: Color.black.opacity(0.08), radius: 12, x: 0, y: 6)
    }

    private func destinationButton(for destination: ExplorerDestination) -> some View {
        let isSelected = viewModel.selectedExplorerDestination == destination
//        let count = viewModel.explorerCount(for: destination)
        let isRestricted = destination.requiresAuthentication && !(viewModel.userMode?.canParticipate ?? false)
        return Button {
            guard viewModel.selectedExplorerDestination != destination else { return }
            withAnimation(.easeInOut(duration: 0.2)) {
                attemptSelection(of: destination)
            }
        } label: {
            HStack(spacing: 4) {
                HStack(spacing: 4) {
                    Image(systemName: destination.systemImageName)
                        .font(.system(size: 10, weight: .semibold))
                    Text(destination.title)
                        .font(.system(size: 10, weight: .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.85)
                    if isRestricted {
                        Image(systemName: "lock.fill")
                            .font(.caption.weight(.bold))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

//                if count > 0 {
//                    Text("\(count)")
//                        .font(.system(size: 10, weight: .semibold))
//                        .padding(.horizontal, 2)
//                        .padding(.vertical, 2)
//                        .background(
//                            Capsule()
//                                .fill(isSelected ? geoDropTheme.colors.onPrimary.opacity(0.2) : geoDropTheme.colors.surfaceVariant.opacity(0.6))
//                        )
//                        .foregroundColor(isSelected ? geoDropTheme.colors.onPrimary : geoDropTheme.colors.onSurface)
//                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
            .padding(.horizontal, 10)
            .background(
                Capsule()
                    .fill(isSelected ? geoDropTheme.colors.primary : Color.clear)
            )
            .foregroundColor(
                isSelected ? geoDropTheme.colors.onPrimary : (isRestricted ? geoDropTheme.colors.onSurface.opacity(0.6) : geoDropTheme.colors.onSurface)
            )
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .accessibilityLabel(destination.title)
        .accessibilityHint(destination.requiresAuthentication ? "Requires sign in" : "")
    }

    private func attemptSelection(of destination: ExplorerDestination) {
        let previous = viewModel.selectedExplorerDestination
        viewModel.setExplorerDestination(destination)
        if previous == viewModel.selectedExplorerDestination, previous != destination {
            isRestrictionAlertPresented = viewModel.explorerRestrictionMessage != nil
        }
    }
}

struct DropRowView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme
    let drop: Drop
    let isSelected: Bool
    let onSelect: () -> Void
    @State private var isExpanded = false
    @State private var infoAlertMessage: String?
    @State private var showingReport = false
    @State private var isSubmittingReport = false
    @State private var reportErrorMessage: String?
    @State private var selectedReportReasons: Set<String> = []
    @State private var showingDeleteConfirmation = false
    
    var body: some View {
        let likePermission = viewModel.likePermission(for: drop)
        let reactionStatus = drop.isLiked(by: currentUserId)
        let hasCollected = viewModel.hasCollected(drop: drop)
        let shouldHideContent = viewModel.shouldHideContent(for: drop)
        let previewDistance = viewModel.distanceToDrop(drop)
        let canPreviewContent = viewModel.canPreview(drop: drop, distance: previewDistance)
        let previewRestrictionMessage = viewModel.previewRestrictionMessage(for: drop, distance: previewDistance)
        let isOutsidePreviewRadius = !canPreviewContent && previewDistance != nil
        let isCollectedDestination = viewModel.selectedExplorerDestination == .collected
        let isMyDropsDestination = viewModel.selectedExplorerDestination == .myDrops
        let canDeleteDrop = drop.createdBy == currentUserId
        
        let titleFont = geoDropTheme.typography.title
        let descriptionFont = geoDropTheme.typography.body
        let actionFont = geoDropTheme.typography.body.weight(.semibold)

        return VStack(alignment: .leading, spacing: 12) {
            Button {
                onSelect()
                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    isExpanded.toggle()
                }
            } label: {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .center, spacing: 8) {
                        Text(drop.displayTitle)
                            .font(titleFont)
                            .foregroundColor(geoDropTheme.colors.onSurface)
                            .multilineTextAlignment(.leading)
                        Spacer(minLength: 12)
                        if drop.requiresRedemption() {
                            Label("Redeem", systemImage: "tag")
                                .font(actionFont)
                                .foregroundColor(geoDropTheme.colors.tertiary)
                                .labelStyle(.titleAndIcon)
                        }
                        Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                            .font(actionFont)
                            .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                    }

                    if canPreviewContent, let description = drop.description, !description.isEmpty {
                        Text(description)
                            .font(descriptionFont)
                            .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                            .lineLimit(isExpanded ? nil : 2)
                            .multilineTextAlignment(.leading)
                    } else if !canPreviewContent, let previewRestrictionMessage {
                        Text(previewRestrictionMessage)
                            .font(descriptionFont)
                            .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                            .lineLimit(isExpanded ? nil : 2)
                            .multilineTextAlignment(.leading)
                    }

                    if isNearbyDestination {
                        reactionSummary
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)

            if isExpanded {
                VStack(alignment: .leading, spacing: 12) {
                    if shouldHideContent {
                        nsfwNotice
                    } else if canPreviewContent {
                        if hasMediaPreview {
                            mediaPreview
                                .transition(.opacity.combined(with: .move(edge: .top)))
                        }

                        if let description = expandedDescriptionText, description != headerDescriptionText {
                            Text(description)
                                .font(descriptionFont)
                                .foregroundColor(geoDropTheme.colors.onSurface)
                        }
                    } else if let previewRestrictionMessage {
                        Text(previewRestrictionMessage)
                            .font(descriptionFont)
                            .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        if isCollectedDestination {
                            Button(action: toggleDislike) {
                                Label(
                                    reactionStatus == .disliked ? "Disliked" : "Dislike",
                                    systemImage: reactionStatus == .disliked ? "hand.thumbsdown.fill" : "hand.thumbsdown"
                                )
                                    .font(actionFont)
                            }
                            .buttonStyle(.borderless)
                            .help(likePermission.message ?? "")
                        }

                        if !isMyDropsDestination, !isNearbyDestination {
                            Button(action: startReport) {
                                Label("Report", systemImage: "exclamationmark.bubble")
                                    .font(actionFont)
                            }
                            .buttonStyle(.borderless)
                        }
                        
                        if isOutsidePreviewRadius && !isNearbyDestination {
                            Button(action: ignoreDrop) {
                                Label("Ignore for now", systemImage: "eye.slash")
                                    .font(actionFont)
                            }
                            .buttonStyle(.borderless)
                        }

                        HStack(alignment: .center, spacing: 12) {
                            Spacer(minLength: 0)

                            if canDeleteDrop {
                                Button(role: .destructive) {
                                    showingDeleteConfirmation = true
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                        .font(actionFont)
                                }
                                .buttonStyle(.borderless)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        
                        if !isMyDropsDestination {
                            Button(action: markCollected) {
                                Label(hasCollected ? "Collected" : "Collect", systemImage: hasCollected ? "checkmark.circle.fill" : "tray.and.arrow.down")
                                    .font(actionFont)
                            }
                            .frame(maxWidth: .infinity)
                            .buttonStyle(.bordered)
                            .disabled(hasCollected || !canPreviewContent)
                        }
                    }
                    
                    if !isMyDropsDestination, !likePermission.allowed, let message = likePermission.message {
                        Text(message)
                            .font(descriptionFont)
                            .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                    }
                }
                .transition(.opacity)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(isSelected ? geoDropTheme.colors.primary.opacity(0.15) : geoDropTheme.colors.surfaceVariant)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(isSelected ? geoDropTheme.colors.primary : geoDropTheme.colors.outlineVariant.opacity(0.6), lineWidth: 1)
        )
        .contentShape(RoundedRectangle(cornerRadius: 16))
        .onTapGesture { onSelect() }
        .animation(.easeInOut, value: isSelected)
        .sheet(isPresented: $showingReport) {
            reportSheet()
                .environmentObject(viewModel)
        }
        .confirmationDialog(
            "Delete this drop?",
            isPresented: $showingDeleteConfirmation,
            titleVisibility: .visible
        ) {
            Button("Delete Drop", role: .destructive) {
                deleteDrop()
            }
            Button("Cancel", role: .cancel) { }
        } message: {
            Text("This action cannot be undone.")
        }
        .alert("Notice", isPresented: Binding(
            get: { infoAlertMessage != nil },
            set: { if !$0 { infoAlertMessage = nil } }
        )) {
            Button("OK", role: .cancel) { infoAlertMessage = nil }
        } message: {
            Text(infoAlertMessage ?? "")
        }
    }

    private var currentUserId: String? {
        if case let .signedIn(session) = viewModel.authState {
            return session.user.uid
        }
        return nil
    }
    
    private var isNearbyDestination: Bool {
        viewModel.selectedExplorerDestination == .nearby
    }
    
    private var isMyDropsDestination: Bool {
        viewModel.selectedExplorerDestination == .myDrops
    }

    private var reactionSummary: some View {
        HStack(spacing: 12) {
            reactionCountLabel(systemImage: "hand.thumbsup.fill", count: drop.likeCount)
            reactionCountLabel(systemImage: "hand.thumbsdown.fill", count: drop.dislikeCount)
            Spacer(minLength: 0)
            if !isMyDropsDestination {
                Button(action: startReport) {
                    Label("Report", systemImage: "exclamationmark.bubble")
                        .labelStyle(.titleAndIcon)
                }
                .buttonStyle(.borderless)
            }
        }
        .font(.system(size: 12, weight: .semibold))
        .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
    }

    private func reactionCountLabel(systemImage: String, count: Int) -> some View {
        Label("\(count)", systemImage: systemImage)
            .labelStyle(.titleAndIcon)
    }

    private func toggleDislike() {
        let permission = viewModel.likePermission(for: drop)
        guard permission.allowed else {
            if let message = permission.message {
                infoAlertMessage = message
            }
            return
        }
        let currentStatus = drop.isLiked(by: currentUserId)
        let status: DropLikeStatus = currentStatus == .disliked ? .none : .disliked
        viewModel.like(drop: drop, status: status)
    }

    private func markCollected() {
        guard let userId = currentUserId else {
            infoAlertMessage = "Sign in to collect drops."
            return
        }
        guard viewModel.userMode?.canParticipate == true else {
            infoAlertMessage = "Upgrade to a full account to collect drops."
            return
        }
        guard drop.createdBy != userId else {
            infoAlertMessage = "You created this drop."
            return
        }
        guard !viewModel.hasCollected(drop: drop) else { return }
        if let error = viewModel.markCollected(drop: drop) {
            infoAlertMessage = error.localizedDescription
        }
    }
    
    private func startReport() {
        guard let userId = currentUserId else {
            infoAlertMessage = "Sign in to report drops."
            return
        }
        guard viewModel.userMode?.canParticipate == true else {
            infoAlertMessage = "Upgrade to a full account to report drops."
            return
        }
        guard drop.createdBy != userId else {
            infoAlertMessage = "You created this drop."
            return
        }
        guard drop.reportedBy[userId] == nil else {
            infoAlertMessage = "Thanks for your report. We'll review it soon."
            return
        }
        selectedReportReasons = []
        reportErrorMessage = nil
        showingReport = true
    }

    private func submitReport() {
        if selectedReportReasons.isEmpty {
            reportErrorMessage = "Select at least one reason."
            return
        }
        reportErrorMessage = nil
        isSubmittingReport = true
        Task { @MainActor in
            let result = await viewModel.report(drop: drop, reasonCodes: selectedReportReasons)
            isSubmittingReport = false
            switch result {
            case .success:
                showingReport = false
                selectedReportReasons = []
                infoAlertMessage = "Thanks for your report. We'll review it soon."
            case .failure(let error):
                let message = error.localizedDescription.trimmingCharacters(in: .whitespacesAndNewlines)
                reportErrorMessage = message.isEmpty ? "Couldn't submit report. Try again." : message
            }
        }
    }
    
    private func deleteDrop() {
        showingDeleteConfirmation = false
        viewModel.delete(drop: drop)
    }
    
    private func ignoreDrop() {
        viewModel.setIgnored(drop: drop, isIgnored: true)
        infoAlertMessage = "We'll hide this drop for now."
    }

    @ViewBuilder
    private func reportSheet() -> some View {
        let sheet = ReportDropSheet(
            reasons: defaultReportReasons,
            selectedReasonCodes: $selectedReportReasons,
            isSubmitting: isSubmittingReport,
            errorMessage: reportErrorMessage,
            onDismiss: {
                if !isSubmittingReport {
                    showingReport = false
                }
            },
            onSubmit: submitReport
        )

        if #available(iOS 16.0, *) {
            sheet.presentationDetents([.medium, .large])
        } else {
            sheet
        }
    }
    
    private var expandedDescriptionText: String? {
        let trimmed = drop.text.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty {
            return trimmed
        }
        if let description = drop.description?.trimmingCharacters(in: .whitespacesAndNewlines), !description.isEmpty {
            return description
        }
        return nil
    }

    private var headerDescriptionText: String? {
        drop.description?.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    
    private var nsfwNotice: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("Adult content hidden", systemImage: "eye.slash")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            Text("Enable adult content in Profile settings to view this drop.")
                .font(.caption)
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(geoDropTheme.colors.surfaceVariant)
        )
    }

    private var hasMediaPreview: Bool {
        switch drop.contentType {
        case .photo:
            return drop.mediaURL != nil || inlinePhoto != nil
        case .video:
            return drop.mediaURL != nil
        default:
            return false
        }
    }

    @ViewBuilder
    private var mediaPreview: some View {
        switch drop.contentType {
        case .photo:
            if let url = drop.mediaURL {
                StorageAsyncImage(
                    storagePath: drop.mediaStoragePath,
                    url: url,
                    transaction: Transaction(animation: .easeInOut)
                ) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                            .progressViewStyle(.circular)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    case .success(let image):
                        photoContent(for: image)
                    case .failure:
                        if let inlinePhoto {
                            photoContent(for: inlinePhoto)
                        } else {
                            photoPlaceholder
                        }
                    @unknown default:
                        photoPlaceholder
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 220)
                .background(photoBackground)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            } else if let inlinePhoto {
                photoContent(for: inlinePhoto)
                    .frame(maxWidth: .infinity)
                    .frame(height: 220)
                    .background(photoBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            } else {
                photoPlaceholder
                    .frame(maxWidth: .infinity)
                    .frame(height: 220)
                    .background(photoBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        case .video:
            if let url = drop.mediaURL {
                DropVideoPlayerView(url: url)
                    .frame(height: 240)
                    .frame(maxWidth: .infinity)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(geoDropTheme.colors.outlineVariant.opacity(0.5))
                    )
            }
        default:
            EmptyView()
        }
    }
    
    private var inlinePhoto: Image? {
        guard let image = InlineMediaDecoder.image(from: drop.mediaData) else { return nil }
        return Image(uiImage: image)
    }

    @ViewBuilder
    private func photoContent(for image: Image) -> some View {
        image
            .resizable()
            .scaledToFill()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipped()
    }

    private var photoPlaceholder: some View {
        Image(systemName: "photo")
            .font(geoDropTheme.typography.title)
            .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var photoBackground: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(geoDropTheme.colors.surfaceVariant)
    }
}

private struct DropVideoPlayerView: View {
    let url: URL
    @State private var player: AVPlayer?
    
    var body: some View {
        VideoPlayer(player: player)
            .onAppear {
                if player == nil {
                    player = AVPlayer(url: url)
                }
            }
            .onDisappear {
                player?.pause()
                player?.seek(to: .zero)
            }
    }
}

extension DropFeedView {
    private var sortHeader: some View {
        HStack(spacing: 12) {
            Label("Sort", systemImage: "arrow.up.arrow.down")
                .font(.footnote.weight(.semibold))
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            
            Menu {
                Picker("Sort by", selection: $selectedSortOption) {
                    ForEach(DropSortOption.allCases) { option in
                        Text(option.title).tag(option)
                    }
                }
            } label: {
                HStack(spacing: 6) {
                    Text(selectedSortOption.title)
                        .font(.footnote.weight(.semibold))
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.footnote.weight(.semibold))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(
                    Capsule()
                        .fill(geoDropTheme.colors.surfaceVariant)
                )
                .foregroundColor(geoDropTheme.colors.onSurface)
            }
            .accessibilityLabel("Sort drops")
            
            Spacer()

            dropCounter
        }
        .padding(.horizontal)
    }
    
    private var dropCounter: some View {
        Text(dropCountText)
            .font(.footnote.weight(.semibold))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(geoDropTheme.colors.surfaceVariant)
            )
            .foregroundColor(geoDropTheme.colors.onSurface)
            .accessibilityLabel(dropCountAccessibilityLabel)
    }

    private var shouldShowGroupPrompt: Bool {
        viewModel.selectedExplorerDestination == .nearby &&
        viewModel.userMode?.canParticipate == true &&
        viewModel.groups.isEmpty
    }
    
    private var displayedDrops: [Drop] {
        sortDrops(viewModel.explorerDrops(for: viewModel.selectedExplorerDestination))
    }
    
    private var dropCount: Int { displayedDrops.count }

    private var dropCountText: String {
        dropCount == 1 ? "1" : "\(dropCount)"
    }

    private var dropCountAccessibilityLabel: String {
        dropCount == 1 ? "1 drop available" : "\(dropCount) drops available"
    }

    private var emptyStateView: some View {
        let destination = viewModel.selectedExplorerDestination
        return VStack(spacing: 12) {
            Image(systemName: destination.emptyStateIcon)
                .font(geoDropTheme.typography.title.weight(.semibold))
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            Text(destination.emptyStateTitle)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(geoDropTheme.colors.onSurface)
            Text(destination.emptyStateMessage)
                .font(.footnote)
                .multilineTextAlignment(.center)
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                .padding(.horizontal)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var loadingStateView: some View {
        VStack(spacing: 12) {
            ProgressView()
                .progressViewStyle(.circular)
            Text("Loading drops")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(geoDropTheme.colors.onSurface)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func errorStateView(message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(geoDropTheme.typography.title)
                .foregroundColor(geoDropTheme.colors.tertiary)
            Text("Couldn't load drops")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(geoDropTheme.colors.onSurface)
            Text(message)
                .font(.footnote)
                .multilineTextAlignment(.center)
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                .padding(.horizontal)
            Button("Try again") {
                Task { await viewModel.refreshDrops() }
            }
            .buttonStyle(.borderedProminent)
            .tint(geoDropTheme.colors.primary)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func updateSelection(for drops: [Drop]) {
        guard let first = drops.first else {
            selectedDropID = nil
            mapCameraState = GoogleMapCameraState(
                latitude: Self.defaultCoordinate.latitude,
                longitude: Self.defaultCoordinate.longitude,
                zoom: Self.defaultZoom
            )
            shouldAnimateCamera = false
            return
        }

        if let currentID = selectedDropID,
           let currentDrop = drops.first(where: { $0.id == currentID }) {
            focus(on: currentDrop, animated: false)
        } else {
            focus(on: first, animated: false, preserveZoom: false)
        }
    }

    private func focus(on drop: Drop, animated: Bool = true, preserveZoom: Bool = true) {
        selectedDropID = drop.id
        let zoom = preserveZoom ? sanitizedZoom(mapCameraState.zoom) : Self.defaultZoom
        mapCameraState = GoogleMapCameraState(
            latitude: drop.latitude,
            longitude: drop.longitude,
            zoom: zoom
        )
        shouldAnimateCamera = animated
    }

    private func sanitizedZoom(_ zoom: Float) -> Float {
        guard zoom.isFinite, zoom > 0 else {
            return Self.defaultZoom
        }
        return zoom
    }
}

extension DropFeedView {
    enum DropSortOption: String, CaseIterable, Identifiable {
        case newest
        case nearest
        case popular
        case endingSoon

        var id: String { rawValue }

        var title: String {
            switch self {
            case .newest: return "Newest"
            case .nearest: return "Nearest"
            case .popular: return "Popular"
            case .endingSoon: return "Ending Soon"
            }
        }
    }
}

private extension DropFeedView {
    func sortDrops(_ drops: [Drop]) -> [Drop] {
        switch selectedSortOption {
        case .newest:
            return drops.sorted { lhs, rhs in
                if lhs.createdAt == rhs.createdAt {
                    return lhs.id < rhs.id
                }
                return lhs.createdAt > rhs.createdAt
            }
        case .nearest:
            return drops.sorted { lhs, rhs in
                let lhsDistance = viewModel.distanceToDrop(lhs)
                let rhsDistance = viewModel.distanceToDrop(rhs)
                switch (lhsDistance, rhsDistance) {
                case let (l?, r?):
                    if l == r { return lhs.createdAt > rhs.createdAt }
                    return l < r
                case (.some, .none):
                    return true
                case (.none, .some):
                    return false
                case (nil, nil):
                    return lhs.createdAt > rhs.createdAt
                }
            }
        case .popular:
            return drops.sorted { lhs, rhs in
                if lhs.likeCount == rhs.likeCount {
                    return lhs.createdAt > rhs.createdAt
                }
                return lhs.likeCount > rhs.likeCount
            }
        case .endingSoon:
            return drops.sorted { lhs, rhs in
                let lhsTime = timeUntilExpiration(for: lhs)
                let rhsTime = timeUntilExpiration(for: rhs)
                switch (lhsTime, rhsTime) {
                case let (l?, r?):
                    if l == r { return lhs.createdAt > rhs.createdAt }
                    return l < r
                case (.some, .none):
                    return true
                case (.none, .some):
                    return false
                case (nil, nil):
                    return lhs.createdAt > rhs.createdAt
                }
            }
        }
    }

    func timeUntilExpiration(for drop: Drop) -> TimeInterval? {
        guard let days = drop.decayDays, days > 0 else {
            return nil
        }
        let expiration = drop.createdAt.addingTimeInterval(TimeInterval(days) * 86_400)
        return expiration.timeIntervalSinceNow
    }
}

private struct ReadOnlyModeCard: View {
    let title: String
    let message: String
    let actionTitle: String?
    let action: (() -> Void)?

    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .foregroundColor(geoDropTheme.colors.onSurface)
            Text(message)
                .font(.subheadline)
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(.borderedProminent)
                    .tint(geoDropTheme.colors.primary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(geoDropTheme.colors.surfaceVariant)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(geoDropTheme.colors.outlineVariant.opacity(0.6), lineWidth: 1)
        )
    }
}
