import SwiftUI
import MapKit
import AVKit
import AVFoundation
import UIKit

struct DropDetailView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme
    let drop: Drop
    @State private var detailCameraState: GoogleMapCameraState
    @State private var detailShouldAnimate = false
    @State private var detailSelectedDropID: Drop.ID?
    @Environment(\.dismiss) private var dismiss
    
    @State private var showingReport = false
    @State private var selectedReportReasons: Set<String> = []
    @State private var isSubmittingReport = false
    @State private var reportErrorMessage: String?
    @State private var actionStatusMessage: String?
    @State private var infoAlertMessage: String?
    @State private var redemptionCodeInput: String = ""
    @State private var redemptionStatusMessage: String?
    @State private var redemptionErrorMessage: String?
    @State private var isRedeeming = false
    @State private var isBlockingCreator = false
    @State private var isPresentingShareSheet = false
    @State private var shareItems: [Any] = []
    
    init(drop: Drop) {
        self.drop = drop
        _detailCameraState = State(
            initialValue: GoogleMapCameraState(
                latitude: drop.latitude,
                longitude: drop.longitude,
                zoom: GoogleMapCameraState.defaultZoom
            )
        )
        _detailSelectedDropID = State(initialValue: drop.id)
    }
    
    var body: some View {
        let resolvedDrop = viewModel.drops.first(where: { $0.id == drop.id }) ?? drop
        let likePermission = viewModel.likePermission(for: resolvedDrop)
        let hasCollected = viewModel.hasCollected(drop: resolvedDrop)
        let shouldHideContent = viewModel.shouldHideContent(for: resolvedDrop)
        let userId = viewModel.currentUserID
        let alreadyReported = userId.flatMap { resolvedDrop.reportedBy[$0] != nil } ?? false
        let alreadyRedeemed = resolvedDrop.isRedeemed(by: userId)
        let canParticipate = viewModel.userMode?.canParticipate ?? false
        let isOwner = viewModel.isOwner(of: resolvedDrop)
        let previewDistance = viewModel.distanceToDrop(resolvedDrop)
        let canPreviewContent = viewModel.canPreview(drop: resolvedDrop, distance: previewDistance)
        let previewRestrictionMessage = viewModel.previewRestrictionMessage(for: resolvedDrop, distance: previewDistance)
        let previewMessage = previewRestrictionMessage ?? "Move closer to preview this drop."

        return GeoDropNavigationContainer(
            trailing: {
                Button("Close", action: dismiss.callAsFunction)
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(geoDropTheme.colors.primary)
            }
        ) {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    headerSection(for: resolvedDrop)

                    if shouldHideContent {
                        nsfwNotice(labels: resolvedDrop.nsfwLabels)
                    } else if canPreviewContent {
                        descriptionSection(for: resolvedDrop)
                        mediaSection(for: resolvedDrop)
                    } else {
                        previewRestrictionNotice(previewMessage)
                    }

                    mapSection(for: resolvedDrop)

                    actionButtons(for: resolvedDrop, likePermission: likePermission, hasCollected: hasCollected)

                    shareSection(for: resolvedDrop)

                    if resolvedDrop.requiresRedemption() {
                        redemptionSection(
                            for: resolvedDrop,
                            alreadyRedeemed: alreadyRedeemed,
                            canParticipate: canParticipate
                        )
                    }

                    moderationSection(
                        for: resolvedDrop,
                        alreadyReported: alreadyReported,
                        isOwner: isOwner,
                        canParticipate: canParticipate
                    )

                    if let message = actionStatusMessage {
                        statusBanner(message)
                    }
                }
                .padding()
            }
        }
        .sheet(isPresented: $showingReport) {
            reportSheet(for: resolvedDrop)
        }
        .sheet(isPresented: $isPresentingShareSheet) {
            ShareSheet(activityItems: shareItems)
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

    private func headerSection(for drop: Drop) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(drop.displayTitle)
                .font(.title2)
                .fontWeight(.bold)
            if let businessName = drop.businessName, !businessName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(businessName)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            Text(drop.createdAt, style: .date)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }

    @ViewBuilder
    private func descriptionSection(for drop: Drop) -> some View {
        if let description = drop.description?.trimmingCharacters(in: .whitespacesAndNewlines), !description.isEmpty {
            Text(description)
                .font(.callout)
                .foregroundColor(.primary)
        }
    }

    @ViewBuilder
    private func mediaSection(for drop: Drop) -> some View {
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
                            .frame(maxWidth: .infinity, minHeight: 220)
                    case .success(let image):
                        photoContent(for: image)
                    case .failure:
                        if let inlinePhoto = inlinePhoto(for: drop) {
                            photoContent(for: inlinePhoto)
                        } else {
                            fallbackMediaPlaceholder(label: "Unable to load image")
                        }
                    @unknown default:
                        fallbackMediaPlaceholder(label: "Unable to load image")
                    }
                }
            } else if let inlinePhoto = inlinePhoto(for: drop) {
                photoContent(for: inlinePhoto)
            } else {
                fallbackMediaPlaceholder(label: "Unable to load image")
            }
        case .video:
            if let url = drop.mediaURL {
                InlineVideoPlayer(url: url)
                    .frame(height: 260)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(geoDropTheme.colors.outlineVariant.opacity(0.5))
                    )
            } else {
                fallbackMediaPlaceholder(label: "Video unavailable")
            }
        case .audio:
            DropAudioPlayerView(url: drop.mediaURL, inlineBase64: drop.mediaData)
        case .text:
            EmptyView()
        }
    }
    
    private func inlinePhoto(for drop: Drop) -> Image? {
        guard let image = InlineMediaDecoder.image(from: drop.mediaData) else { return nil }
        return Image(uiImage: image)
    }

    private func photoContent(for image: Image) -> some View {
        image
            .resizable()
            .scaledToFill()
            .frame(maxWidth: .infinity)
            .frame(height: 260)
            .clipped()
            .cornerRadius(16)
    }

    private func fallbackMediaPlaceholder(label: String) -> some View {
        RoundedRectangle(cornerRadius: 16)
            .fill(geoDropTheme.colors.surfaceVariant)
            .frame(maxWidth: .infinity)
            .frame(height: 220)
            .overlay {
                VStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.title3)
                        .foregroundColor(.secondary)
                    Text(label)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
    }

    private func mapSection(for drop: Drop) -> some View {
        GoogleMapView(
            drops: [drop],
            selectedDropID: $detailSelectedDropID,
            cameraState: $detailCameraState,
            shouldAnimateCamera: $detailShouldAnimate,
            isInteractionEnabled: false
        )
        .frame(height: 200)
        .cornerRadius(16)
    }

    private func actionButtons(for drop: Drop, likePermission: AppViewModel.LikePermission, hasCollected: Bool) -> some View {
        let reactionStatus = drop.isLiked(by: viewModel.currentUserID)
        return VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                Button {
                    handleLike(drop)
                } label: {
                    Label(
                        reactionStatus == .liked ? "Liked" : "Like",
                        systemImage: reactionStatus == .liked ? "hand.thumbsup.fill" : "hand.thumbsup"
                    )
                }
                .buttonStyle(.borderedProminent)

                Button {
                    handleCollect(drop)
                } label: {
                    Label(hasCollected ? "Collected" : "Collect", systemImage: hasCollected ? "checkmark.circle" : "tray.and.arrow.down")
                }
                .buttonStyle(.bordered)
                .disabled(hasCollected)
            }

            if !likePermission.allowed, let message = likePermission.message {
                Text(message)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }

    private func shareSection(for drop: Drop) -> some View {
        HStack(spacing: 12) {
            if let shareText = shareText(for: drop) {
                if #available(iOS 16.0, *) {
                    ShareLink(item: shareText) {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                    .buttonStyle(.bordered)
                } else {
                    Button {
                        shareItems = [shareText]
                        isPresentingShareSheet = true
                    } label: {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                    .buttonStyle(.bordered)
                }
            }

            Button {
                openInMaps(for: drop)
            } label: {
                Label("Open in Maps", systemImage: "map")
            }
            .buttonStyle(.bordered)
        }
    }

    private func redemptionSection(for drop: Drop, alreadyRedeemed: Bool, canParticipate: Bool) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Business redemption")
                .font(.subheadline.weight(.semibold))

            if let limit = drop.redemptionLimit {
                let remaining = max(limit - drop.redemptionCount, 0)
                Text("Redemptions left: \(remaining)")
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }

            if alreadyRedeemed {
                Text("You've already redeemed this offer.")
                    .font(.footnote)
                    .foregroundColor(.secondary)
            } else {
                TextField("Enter redemption code", text: $redemptionCodeInput)
                    .textFieldStyle(.roundedBorder)
                    .disabled(!canParticipate || viewModel.currentUserID == nil)

                Button {
                    redeem(drop)
                } label: {
                    if isRedeeming {
                        ProgressView()
                            .progressViewStyle(.circular)
                    } else {
                        Text("Redeem offer")
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(isRedeeming || !canParticipate || viewModel.currentUserID == nil)
            }

            if let redemptionStatusMessage {
                Text(redemptionStatusMessage)
                    .font(.footnote)
                    .foregroundColor(.green)
            }

            if let redemptionErrorMessage {
                Text(redemptionErrorMessage)
                    .font(.footnote)
                    .foregroundColor(.red)
            }

            if !canParticipate || viewModel.currentUserID == nil {
                Text("Sign in with a full account to redeem offers.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(geoDropTheme.colors.surfaceVariant)
        )
    }

    private func moderationSection(for drop: Drop, alreadyReported: Bool, isOwner: Bool, canParticipate: Bool) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Keep GeoDrop welcoming")
                .font(.subheadline.weight(.semibold))

            HStack(spacing: 12) {
                Button {
                    startReport(for: drop, alreadyReported: alreadyReported, isOwner: isOwner, canParticipate: canParticipate)
                } label: {
                    Label(alreadyReported ? "Reported" : "Report", systemImage: "exclamationmark.bubble")
                }
                .buttonStyle(.bordered)
                .disabled(isSubmittingReport)

                if !isOwner {
                    Button {
                        blockCreator(drop, canParticipate: canParticipate)
                    } label: {
                        if isBlockingCreator {
                            ProgressView()
                                .progressViewStyle(.circular)
                        } else {
                            Label("Block creator", systemImage: "hand.raised")
                        }
                    }
                    .buttonStyle(.bordered)
                    .disabled(isBlockingCreator)
                }
            }

            if alreadyReported {
                Text("Thanks for your report. We'll review it soon.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            if viewModel.currentUserID == nil {
                Text("Sign in to report or block this creator.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            } else if !canParticipate {
                Text("Upgrade to a full account to moderate drops.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }

    private func statusBanner(_ message: String) -> some View {
        Text(message)
            .font(.footnote)
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(geoDropTheme.colors.primary.opacity(0.12))
            )
    }

    private func nsfwNotice(labels: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Adult content hidden", systemImage: "eye.slash")
                .font(.subheadline.weight(.semibold))
            Text("Enable adult content in Profile settings to view this drop.")
                .font(.callout)
                .foregroundColor(.primary)
            if !labels.isEmpty {
                Text("Flagged because: \(labels.joined(separator: ", "))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(geoDropTheme.colors.surfaceVariant)
        )
    }
    
    private func previewRestrictionNotice(_ message: String) -> some View {
        Text(message)
            .font(.callout)
            .foregroundColor(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func shareText(for drop: Drop) -> String? {
        let trimmedDescription = drop.description?.trimmingCharacters(in: .whitespacesAndNewlines)
        let descriptionComponent = (trimmedDescription?.isEmpty == false) ? trimmedDescription : nil
        var components: [String] = [drop.displayTitle]
        if let descriptionComponent {
            components.append(descriptionComponent)
        }
        if let mapUrl = mapURL(for: drop) {
            components.append(mapUrl.absoluteString)
        } else {
            components.append("Lat: \(drop.latitude), Lng: \(drop.longitude)")
        }
        return components.joined(separator: "\n\n")
    }

    private func mapURL(for drop: Drop) -> URL? {
        var components = URLComponents(string: "https://maps.apple.com/")
        components?.queryItems = [
            URLQueryItem(name: "ll", value: "\(drop.latitude),\(drop.longitude)"),
            URLQueryItem(name: "q", value: drop.displayTitle.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed))
        ]
        return components?.url
    }

    private func openInMaps(for drop: Drop) {
        let coordinate = CLLocationCoordinate2D(latitude: drop.latitude, longitude: drop.longitude)
        let placemark = MKPlacemark(coordinate: coordinate)
        let mapItem = MKMapItem(placemark: placemark)
        mapItem.name = drop.displayTitle
        mapItem.openInMaps(launchOptions: [MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDriving])
    }

    private func handleLike(_ drop: Drop) {
        let permission = viewModel.likePermission(for: drop)
        guard permission.allowed else {
            if let message = permission.message {
                infoAlertMessage = message
            }
            return
        }
        let currentStatus = drop.isLiked(by: viewModel.currentUserID)
        let desiredStatus: DropLikeStatus = currentStatus == .liked ? .none : .liked
        viewModel.like(drop: drop, status: desiredStatus)
    }

    private func handleCollect(_ drop: Drop) {
        guard !viewModel.hasCollected(drop: drop) else { return }
        guard let userId = viewModel.currentUserID else {
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
        if let error = viewModel.markCollected(drop: drop) {
            infoAlertMessage = error.localizedDescription
        }
    }

    private func startReport(for drop: Drop, alreadyReported: Bool, isOwner: Bool, canParticipate: Bool) {
        guard let _ = viewModel.currentUserID else {
            infoAlertMessage = "Sign in to report drops."
            return
        }
        guard canParticipate else {
            infoAlertMessage = "Upgrade to a full account to report drops."
            return
        }
        guard !isOwner else {
            infoAlertMessage = "You created this drop."
            return
        }
        guard !alreadyReported else {
            infoAlertMessage = "Thanks for your report. We'll review it soon."
            return
        }
        selectedReportReasons = []
        reportErrorMessage = nil
        showingReport = true
    }

    private func submitReport(for drop: Drop) {
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
                actionStatusMessage = "Thanks for your report. We'll review it soon."
                showingReport = false
                selectedReportReasons = []
            case .failure(let error):
                let message = error.localizedDescription.trimmingCharacters(in: .whitespacesAndNewlines)
                reportErrorMessage = message.isEmpty ? "Couldn't submit report. Try again." : message
            }
        }
    }

    private func redeem(_ drop: Drop) {
        let trimmed = redemptionCodeInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            redemptionErrorMessage = "Enter the code shared by the business."
            redemptionStatusMessage = nil
            return
        }
        redemptionErrorMessage = nil
        redemptionStatusMessage = nil
        isRedeeming = true
        Task { @MainActor in
            let result = await viewModel.redeem(drop: drop, code: trimmed)
            isRedeeming = false
            switch result {
            case let .success(_, _, _):
                redemptionStatusMessage = "Offer redeemed! Show this confirmation to the business."
                redemptionErrorMessage = nil
                redemptionCodeInput = ""
            case .invalidCode:
                redemptionErrorMessage = "That code didn't match. Try again."
            case .alreadyRedeemed:
                redemptionErrorMessage = "This offer was already redeemed for your account."
            case .outOfRedemptions:
                redemptionErrorMessage = "This offer has reached its redemption limit."
            case .notEligible:
                redemptionErrorMessage = "This drop cannot be redeemed."
            case .error(let message):
                let fallback = message.trimmingCharacters(in: .whitespacesAndNewlines)
                redemptionErrorMessage = fallback.isEmpty ? "Couldn't complete redemption." : fallback
            }
        }
    }

    private func blockCreator(_ drop: Drop, canParticipate: Bool) {
        guard let _ = viewModel.currentUserID else {
            infoAlertMessage = "Sign in to block creators."
            return
        }
        guard canParticipate else {
            infoAlertMessage = "Upgrade to a full account to block creators."
            return
        }
        guard !viewModel.isOwner(of: drop) else {
            infoAlertMessage = "You can't block your own drop."
            return
        }
        isBlockingCreator = true
        Task { @MainActor in
            let result = await viewModel.blockCreator(of: drop)
            isBlockingCreator = false
            switch result {
            case .success:
                actionStatusMessage = "Creator blocked. You won't see their drops anymore."
            case .failure(let error):
                let message = error.localizedDescription.trimmingCharacters(in: .whitespacesAndNewlines)
                infoAlertMessage = message.isEmpty ? "Couldn't block this creator. Try again." : message
            }
        }
    }

    @ViewBuilder
    private func reportSheet(for drop: Drop) -> some View {
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
            onSubmit: {
                submitReport(for: drop)
            }
        )
        
        if #available(iOS 16.0, *) {
            sheet.presentationDetents([.medium, .large])
        } else {
            sheet
        }
    }
}

private struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

private struct InlineVideoPlayer: View {
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

private struct DropAudioPlayerView: View {
    let url: URL?
    let inlineBase64: String?
    @State private var player: AVPlayer?
    @State private var tempURL: URL?
    @State private var isPreparing = false
    @State private var isPlaying = false
    @State private var errorMessage: String?
    @State private var playbackObserver: NSObjectProtocol?
    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                Button(action: togglePlayback) {
                    Image(systemName: isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .font(.system(size: 36))
                        .foregroundColor(geoDropTheme.colors.primary)
                }
                .disabled(isPreparing || (player == nil && errorMessage != nil))

                VStack(alignment: .leading, spacing: 4) {
                    Text("Audio drop")
                        .font(.headline)
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    } else if isPlaying {
                        Text("Playing…")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    } else {
                        Text("Tap play to listen inline.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                Spacer()
            }

            if isPreparing {
                ProgressView()
                    .progressViewStyle(.circular)
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(geoDropTheme.colors.surfaceVariant)
        )
        .onDisappear {
            stopPlayback()
            cleanup()
        }
    }

    private func togglePlayback() {
        if player == nil {
            preparePlayerIfNeeded()
        }
        guard let player = player else { return }
        if isPlaying {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
            startObservingPlayer()
        }
    }

    private func preparePlayerIfNeeded() {
        guard !isPreparing else { return }
        guard player == nil else { return }
        isPreparing = true
        errorMessage = nil

        if let url {
            player = AVPlayer(url: url)
            isPreparing = false
        } else if let inlineBase64, let data = Data(base64Encoded: inlineBase64) {
            let temp = FileManager.default.temporaryDirectory
                .appendingPathComponent(UUID().uuidString)
                .appendingPathExtension("m4a")
            do {
                try data.write(to: temp)
                tempURL = temp
                player = AVPlayer(url: temp)
            } catch {
                errorMessage = "Audio unavailable."
            }
            isPreparing = false
        } else {
            errorMessage = "Audio unavailable."
            isPreparing = false
        }
    }

    private func startObservingPlayer() {
        guard let item = player?.currentItem, playbackObserver == nil else { return }
        playbackObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: item,
            queue: .main
        ) { _ in
            stopPlayback()
        }
    }

    private func stopPlayback() {
        player?.pause()
        player?.seek(to: .zero)
        isPlaying = false
    }

    private func cleanup() {
        if let playbackObserver {
            NotificationCenter.default.removeObserver(playbackObserver)
            self.playbackObserver = nil
        }
        if let tempURL {
            try? FileManager.default.removeItem(at: tempURL)
            self.tempURL = nil
        }
    }
}

struct ReportReason: Identifiable, Hashable {
    let code: String
    let label: String
    var id: String { code }
}

let defaultReportReasons: [ReportReason] = [
    ReportReason(code: "spam", label: "Spam or misleading"),
    ReportReason(code: "harassment", label: "Harassment or hate"),
    ReportReason(code: "nsfw", label: "Sexual or adult content"),
    ReportReason(code: "violence", label: "Violence or dangerous activity"),
    ReportReason(code: "other", label: "Something else")
]

struct ReportDropSheet: View {
    let reasons: [ReportReason]
    @Binding var selectedReasonCodes: Set<String>
    let isSubmitting: Bool
    let errorMessage: String?
    let onDismiss: () -> Void
    let onSubmit: () -> Void

    var body: some View {
        reportNavigationContainer
    }

    @ViewBuilder
    private var reportNavigationContainer: some View {
        if #available(iOS 16.0, *) {
            NavigationStack {
                reportNavigationContent
            }
        } else {
            NavigationView {
                reportNavigationContent
            }
        }
    }

    @ViewBuilder
    private var reportForm: some View {
        Form {
            Section {
                ForEach(reasons) { reason in
                    Toggle(isOn: binding(for: reason)) {
                        Text(reason.label)
                    }
                    .disabled(isSubmitting)
                }
            } header: {
                FormSectionHeader(
                    title: "Report details",
                    subtitle: "Help us understand what's wrong so we can investigate quickly.",
                    systemImage: "exclamationmark.bubble"
                )
            } footer: {
                Text("Select one or more reasons so our team can review this drop.")
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }

            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundColor(.red)
                }
            }
        }
    }

    private var reportNavigationContent: some View {
        reportForm
            .navigationTitle("Report drop")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { reportToolbar }
    }

    @ToolbarContentBuilder
    private var reportToolbar: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button("Cancel") { onDismiss() }
                .disabled(isSubmitting)
        }
        ToolbarItem(placement: .confirmationAction) {
            Button(action: onSubmit) {
                if isSubmitting {
                    ProgressView()
                        .progressViewStyle(.circular)
                } else {
                    Text("Submit")
                }
            }
            .disabled(isSubmitting)
        }
    }
    
    private func binding(for reason: ReportReason) -> Binding<Bool> {
        Binding(
            get: { selectedReasonCodes.contains(reason.code) },
            set: { isSelected in
                if isSelected {
                    selectedReasonCodes.insert(reason.code)
                } else {
                    selectedReasonCodes.remove(reason.code)
                }
            }
        )
    }
}
