import SwiftUI
import AVFoundation
import UIKit

struct CreateDropView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var text = ""
    @State private var description = ""
    @State private var isAnonymous = false
    @State private var dropType: DropType = .community
    @State private var contentType: DropContentType = .text
    @State private var mediaPreview: Image?
    @State private var mediaData: Data?
    @State private var mediaMimeType: String?
    @State private var mediaFileExtension: String?
    @State private var audioDuration: TimeInterval?
    @State private var videoDuration: TimeInterval?
    @State private var redemptionCode = ""
    @State private var redemptionLimit = ""
    @State private var decayDays = ""
    @State private var selectedVisibility: DropVisibilityOption = .allExplorers
    @State private var isShowingPhotoCapture = false
    @State private var isShowingVideoCapture = false
    @State private var isShowingAudioRecorder = false
    @State private var activeTemplate: BusinessDropTemplate?
    @State private var isShowingTemplateBrowser = false

    private let maxDecayDays = 365
    private let maxTemplateSuggestions = 6

    var body: some View {
        GeoDropNavigationContainer(
            subtitle: "New drop",
            trailing: { submitAction }
        ) {
            Form {
                messageSection

                if isBusinessUser {
                    templateSection
                }

                dropConfigurationSection

                if contentType != .text {
                    mediaSection
                }

                if dropType == .restaurantCoupon {
                    redemptionSection
                }

                timingSection
            }
            .onAppear {
                updateVisibilitySelection()
                ensureAnonymousState()
            }
            .onChange(of: isBusinessUser) { _ in ensureAnonymousState() }
            .onChange(of: viewModel.groups) { _ in updateVisibilitySelection() }
            .onChange(of: viewModel.selectedGroupCode) { _ in updateVisibilitySelection() }
            .onChange(of: dropType) { newValue in
                if newValue != .restaurantCoupon {
                    redemptionCode = ""
                    redemptionLimit = ""
                }
            }
            .onChange(of: contentType) { _ in resetMedia() }
            .sheet(isPresented: $isShowingPhotoCapture) {
                PhotoCaptureView {
                    data in
                    handleCapturedPhoto(data)
                    isShowingPhotoCapture = false
                } onCancel: {
                    isShowingPhotoCapture = false
                } onError: { message in
                    viewModel.errorMessage = message
                    isShowingPhotoCapture = false
                }
                .ignoresSafeArea()
            }
            .sheet(isPresented: $isShowingVideoCapture) {
                VideoCaptureView {
                    url, duration, mimeType in
                    handleCapturedVideo(url: url, duration: duration, mimeType: mimeType)
                    isShowingVideoCapture = false
                } onCancel: {
                    isShowingVideoCapture = false
                } onError: { message in
                    viewModel.errorMessage = message
                    isShowingVideoCapture = false
                }
                .ignoresSafeArea()
            }
            .sheet(isPresented: $isShowingAudioRecorder) {
                AudioRecorderSheet {
                    url, duration in
                    handleCapturedAudio(url: url, duration: duration)
                    isShowingAudioRecorder = false
                } onCancel: {
                    isShowingAudioRecorder = false
                } onError: { message in
                    viewModel.errorMessage = message
                    isShowingAudioRecorder = false
                }
            }
            .sheet(isPresented: $isShowingTemplateBrowser) {
                let categories = currentProfile?.businessCategories ?? []
                BusinessTemplateBrowserView(
                    selectedCategories: Set(categories),
                    onApply: { template in
                        apply(template: template)
                        isShowingTemplateBrowser = false
                    }
                )
            }
        }
    }
    private var currentProfile: UserProfile? {
        if case let .signedIn(session) = viewModel.authState {
            return session.profile
        }
        return nil
    }

    private var isBusinessUser: Bool {
        currentProfile?.role == .business
    }

    private var canPostAnonymously: Bool {
        !isBusinessUser
    }

    private var templateSuggestions: [BusinessDropTemplate] {
        guard let profile = currentProfile, profile.role == .business else { return [] }
        let suggestions = BusinessDropTemplates.suggestions(for: profile.businessCategories)
        return Array(suggestions.prefix(maxTemplateSuggestions))
    }
    
    private var hasBusinessCategories: Bool {
        guard let profile = currentProfile, profile.role == .business else { return false }
        return !profile.businessCategories.isEmpty
    }

    private var visibilityOptions: [DropVisibilityOption] {
        var options: [DropVisibilityOption] = [.allExplorers]
        let sorted = viewModel.groups.sorted { $0.code < $1.code }
        for membership in sorted {
            options.append(.group(membership.code))
        }
        return options
    }
    private var messageSection: some View {
        Section(header: Text("Message")) {
            TextField("Headline", text: $text)
            TextField("Description", text: $description)
                .lineLimit(3)
            Toggle("Post anonymously", isOn: $isAnonymous)
                .disabled(!canPostAnonymously)
            if !canPostAnonymously {
                Text("Business drops always display your profile.")
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }
        }
    }

    private var templateSection: some View {
        Section(header: Text("Drop ideas for your business")) {
            if !templateSuggestions.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 16) {
                        ForEach(templateSuggestions, id: \.id) { template in
                            BusinessTemplateCard(template: template) { template in
                                apply(template: template)
                            }
                            .frame(width: 280)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            
            if !hasBusinessCategories {
                Text("Add business categories in Profile to unlock tailored suggestions.")
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }

            Button {
                isShowingTemplateBrowser = true
            } label: {
                Label("Browse all templates", systemImage: "sparkles")
            }

            if let callToAction = activeTemplate?.callToAction, !callToAction.isEmpty {
                Text(callToAction)
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }
        }
    }

    private var dropConfigurationSection: some View {
        Section(header: Text("Drop configuration")) {
            Picker("Type", selection: $dropType) {
                Text("Community").tag(DropType.community)
                Text("Restaurant coupon").tag(DropType.restaurantCoupon)
                Text("Tour stop").tag(DropType.tourStop)
            }
            .pickerStyle(.segmented)

            Picker("Content", selection: $contentType) {
                Text("Text").tag(DropContentType.text)
                Text("Photo").tag(DropContentType.photo)
                Text("Audio").tag(DropContentType.audio)
                Text("Video").tag(DropContentType.video)
            }
            .pickerStyle(.segmented)
        }
    }
    private var mediaSection: some View {
        Section(header: Text("Media")) {
            switch contentType {
            case .text:
                Label("Text-only drop", systemImage: "text.alignleft")
                    .foregroundColor(.secondary)
            case .photo:
                Button {
                    presentPhotoCapture()
                } label: {
                    Label(mediaData == nil ? "Capture photo" : "Retake photo", systemImage: "camera")
                }
                if let preview = mediaPreview {
                    preview
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 220)
                        .cornerRadius(12)
                }
                if mediaData != nil {
                    Button("Remove photo", role: .destructive) { resetMedia() }
                }
            case .audio:
                Button {
                    presentAudioRecorder()
                } label: {
                    Label(mediaData == nil ? "Record audio" : "Re-record audio", systemImage: "mic")
                }
                if let duration = audioDuration {
                    Text("Recorded length: \(formatDuration(duration))")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                if mediaData != nil {
                    Button("Remove audio", role: .destructive) { resetMedia() }
                }
            case .video:
                Button {
                    presentVideoCapture()
                } label: {
                    Label(mediaData == nil ? "Record video" : "Re-record video", systemImage: "video")
                }
                if let preview = mediaPreview {
                    preview
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 220)
                        .cornerRadius(12)
                }
                if let duration = videoDuration {
                    Text("Clip length: \(formatDuration(duration))")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                if mediaData != nil {
                    Button("Remove video", role: .destructive) { resetMedia() }
                }
            }
        }
    }

    private var redemptionSection: some View {
        Section(header: Text("Offer security"), footer: Text("Set a code so each guest redeems only once.")) {
            TextField("Redemption code", text: $redemptionCode)
                .textInputAutocapitalization(.characters)
            TextField("Redemption limit", text: $redemptionLimit)
                .keyboardType(.numberPad)
            Text("Leave the limit blank for unlimited redemptions.")
                .font(.footnote)
                .foregroundColor(.secondary)
        }
    }

    private var timingSection: some View {
        Section(header: Text("Timing & visibility")) {
            TextField("Decay days", text: $decayDays)
                .keyboardType(.numberPad)
            Text("Leave blank to keep this drop forever (max \(maxDecayDays) days).")
                .font(.footnote)
                .foregroundColor(.secondary)

            Picker("Visible to", selection: $selectedVisibility) {
                ForEach(visibilityOptions, id: \.self) { option in
                    Text(option.displayName).tag(option)
                }
            }
            .pickerStyle(.menu)
        }
    }
    private var submitAction: some View {
        Button {
            Task { await submit() }
        } label: {
            Text("Drop")
                .font(.callout.weight(.semibold))
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(canSubmit ? Color.accentColor : Color.accentColor.opacity(0.35))
                .foregroundColor(.white)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .disabled(!canSubmit)
        .opacity(canSubmit ? 1 : 0.6)
    }
    
    private var canSubmit: Bool {
        switch contentType {
        case .text:
            if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return false }
        case .photo, .audio, .video:
            if mediaData == nil { return false }
        }

        if dropType == .restaurantCoupon && redemptionCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return false
        }

        return true
    }
    private func apply(template: BusinessDropTemplate) {
        text = template.caption
        description = template.note
        dropType = template.dropType
        contentType = template.contentType
        activeTemplate = template
        resetMedia()
    }

    private func presentPhotoCapture() {
        Task {
            let granted = await ensureVideoAccess()
            await MainActor.run {
                if granted {
                    isShowingPhotoCapture = true
                } else {
                    viewModel.errorMessage = "Camera access is required to capture photos."
                }
            }
        }
    }

    private func presentVideoCapture() {
        Task {
            let videoGranted = await ensureVideoAccess()
            let audioGranted = await ensureAudioAccess()
            await MainActor.run {
                if videoGranted && audioGranted {
                    isShowingVideoCapture = true
                } else {
                    viewModel.errorMessage = "Camera and microphone access are required to record video."
                }
            }
        }
    }

    private func presentAudioRecorder() {
        Task {
            let granted = await ensureAudioAccess()
            await MainActor.run {
                if granted {
                    isShowingAudioRecorder = true
                } else {
                    viewModel.errorMessage = "Microphone access is required to record audio."
                }
            }
        }
    }

    private func ensureAnonymousState() {
        if !canPostAnonymously {
            isAnonymous = false
        }
    }

    private func updateVisibilitySelection() {
        let options = visibilityOptions
        if options.contains(selectedVisibility) {
            return
        }
        if let preferred = viewModel.selectedGroupCode, options.contains(.group(preferred)) {
            selectedVisibility = .group(preferred)
        } else if let first = options.first {
            selectedVisibility = first
        } else {
            selectedVisibility = .allExplorers
        }
    }

    private func resetMedia() {
        mediaPreview = nil
        mediaData = nil
        mediaMimeType = nil
        mediaFileExtension = nil
        audioDuration = nil
        videoDuration = nil
    }
    private func ensureVideoAccess() async -> Bool {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            return true
        case .notDetermined:
            return await withCheckedContinuation { continuation in
                AVCaptureDevice.requestAccess(for: .video) { granted in
                    continuation.resume(returning: granted)
                }
            }
        default:
            return false
        }
    }

    private func ensureAudioAccess() async -> Bool {
        let session = AVAudioSession.sharedInstance()
        switch session.recordPermission {
        case .granted:
            return true
        case .undetermined:
            return await withCheckedContinuation { continuation in
                session.requestRecordPermission { granted in
                    continuation.resume(returning: granted)
                }
            }
        default:
            return false
        }
    }
    
    private func formatDuration(_ duration: TimeInterval) -> String {
        let totalSeconds = Int(duration.rounded())
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    private func defaultExtension(for mimeType: String) -> String {
        guard let component = mimeType.split(separator: "/").last else { return "dat" }
        return String(component)
    }

    private func generateThumbnail(for url: URL) -> Image? {
        let asset = AVAsset(url: url)
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        let time = CMTime(seconds: 0.5, preferredTimescale: 600)
        do {
            let cgImage = try generator.copyCGImage(at: time, actualTime: nil)
            return Image(uiImage: UIImage(cgImage: cgImage))
        } catch {
            return nil
        }
    }

    private func handleCapturedPhoto(_ data: Data) {
        resetMedia()
        mediaData = data
        mediaMimeType = "image/jpeg"
        mediaFileExtension = "jpg"
        if let uiImage = UIImage(data: data) {
            mediaPreview = Image(uiImage: uiImage)
        }
    }

    private func handleCapturedVideo(url: URL, duration: TimeInterval, mimeType: String) {
        resetMedia()
        do {
            let data = try Data(contentsOf: url)
            mediaData = data
            mediaMimeType = mimeType
            let ext = url.pathExtension.isEmpty ? "mov" : url.pathExtension
            mediaFileExtension = ext
            videoDuration = duration
            mediaPreview = generateThumbnail(for: url)
        } catch {
            viewModel.errorMessage = "Couldn't read the recorded video. Try again."
        }
        try? FileManager.default.removeItem(at: url)
    }

    private func handleCapturedAudio(url: URL, duration: TimeInterval) {
        resetMedia()
        do {
            let data = try Data(contentsOf: url)
            mediaData = data
            mediaMimeType = "audio/mp4"
            let ext = url.pathExtension.isEmpty ? "m4a" : url.pathExtension
            mediaFileExtension = ext
            audioDuration = duration
        } catch {
            viewModel.errorMessage = "Couldn't read the audio recording. Record again and try once more."
        }
        try? FileManager.default.removeItem(at: url)
    }
    private func submit() async {
        let trimmedHeadline = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedDescription = description.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedCode = redemptionCode.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedLimit = redemptionLimit.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedDecay = decayDays.trimmingCharacters(in: .whitespacesAndNewlines)

        if contentType == .text && trimmedHeadline.isEmpty {
            viewModel.errorMessage = "Enter a note before dropping."
            return
        }

        if dropType == .restaurantCoupon && trimmedCode.isEmpty {
            viewModel.errorMessage = "Enter a redemption code for your offer."
            return
        }

        var parsedLimit: Int?
        if !trimmedLimit.isEmpty {
            guard let value = Int(trimmedLimit), value > 0 else {
                viewModel.errorMessage = "Enter a valid redemption limit or leave it blank."
                return
            }
            parsedLimit = value
        }

        var parsedDecay: Int?
        if !trimmedDecay.isEmpty {
            guard let value = Int(trimmedDecay), value > 0, value <= maxDecayDays else {
                viewModel.errorMessage = "Choose a decay up to \(maxDecayDays) days."
                return
            }
            parsedDecay = value
        }

        if contentType != .text && mediaData == nil {
            let message: String
            switch contentType {
            case .photo: message = "Capture a photo before dropping."
            case .audio: message = "Record an audio message before dropping."
            case .video: message = "Record a video before dropping."
            case .text: message = ""
            }
            viewModel.errorMessage = message
            return
        }
        
        var mediaPayload: NewDropRequest.MediaPayload?
        if let data = mediaData, let mimeType = mediaMimeType {
            let ext = mediaFileExtension ?? defaultExtension(for: mimeType)
            mediaPayload = .init(data: data, mimeType: mimeType, fileExtension: ext)
        }
        
        let visibility: NewDropRequest.Visibility = {
            switch selectedVisibility {
            case .allExplorers:
                return .public
            case .group(let code):
                return .group(code)
            }
        }()
        
        let request = NewDropRequest(
            text: trimmedHeadline,
            description: trimmedDescription.isEmpty ? nil : trimmedDescription,
            isAnonymous: canPostAnonymously ? isAnonymous : false,
            dropType: dropType,
            contentType: contentType,
            media: mediaPayload,
            redemptionCode: dropType == .restaurantCoupon ? trimmedCode : nil,
            redemptionLimit: dropType == .restaurantCoupon ? parsedLimit : nil,
            decayDays: parsedDecay,
            visibility: visibility
        )
        
        await viewModel.createDrop(request: request)
        if viewModel.errorMessage == nil {
            resetForm()
        }
    }

    private func resetForm() {
        text = ""
        description = ""
        isAnonymous = false
        dropType = .community
        contentType = .text
        resetMedia()
        redemptionCode = ""
        redemptionLimit = ""
        decayDays = ""
        activeTemplate = nil
        updateVisibilitySelection()
        ensureAnonymousState()
    }
}

private enum DropVisibilityOption: Hashable {
    case group(String)
    case allExplorers

    var id: String {
        switch self {
        case .group(let code):
            return code
        case .allExplorers:
            return "PUBLIC"
        }
    }

    var displayName: String {
        switch self {
        case .group(let code):
            return "Group \(code)"
        case .allExplorers:
            return "Everyone nearby"
        }
    }
}

struct BusinessTemplateCard: View {
    let template: BusinessDropTemplate
    var actionTitle: String = "Use this idea"
    var onApply: ((BusinessDropTemplate) -> Void)? = nil

    private var dropTypeIconName: String {
        switch template.dropType {
        case .community: return "person.2"
        case .restaurantCoupon: return "tag"
        case .tourStop: return "flag"
        }
    }

    private var contentIconName: String {
        switch template.contentType {
        case .text: return "text.alignleft"
        case .photo: return "photo"
        case .audio: return "waveform"
        case .video: return "video"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(template.title)
                    .font(.headline)
                Text("Inspired by \(template.category.displayName)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            HStack(spacing: 12) {
                Label(template.dropType == .restaurantCoupon ? "Offer" : template.dropType == .tourStop ? "Tour" : "Community", systemImage: dropTypeIconName)
                    .font(.caption)
                Label(template.contentType == .text ? "Text" : template.contentType == .photo ? "Photo" : template.contentType == .audio ? "Audio" : "Video", systemImage: contentIconName)
                    .font(.caption)
            }
            .foregroundColor(.secondary)

            Text(template.description)
                .font(.subheadline)

            Divider()

            VStack(alignment: .leading, spacing: 6) {
                if !template.caption.isEmpty {
                    Text(template.caption)
                        .font(.body.weight(.semibold))
                }
                Text(template.note)
                    .font(.body)
                if let callToAction = template.callToAction, !callToAction.isEmpty {
                    Text(callToAction)
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
            }
            
            if let onApply {
                Button(actionTitle) {
                    onApply(template)
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color(uiColor: .secondarySystemBackground)))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.accentColor.opacity(0.25))
        )
    }
}

struct BusinessTemplateBrowserView: View {
    @Environment(\.dismiss) private var dismiss

    let selectedCategories: Set<BusinessCategory>
    var onApply: ((BusinessDropTemplate) -> Void)?

    private var focusCategoryIds: Set<String> {
        Set(selectedCategories.map(\.id))
    }

    private var filteredGroups: [BusinessCategory.GroupMetadata] {
        BusinessCategory.grouped.compactMap { metadata in
            let categories = metadata.categories.filter { category in
                focusCategoryIds.isEmpty || focusCategoryIds.contains(category.id)
            }
            guard !categories.isEmpty else { return nil }
            return BusinessCategory.GroupMetadata(
                group: metadata.group,
                title: metadata.title,
                description: metadata.description,
                categories: categories
            )
        }
    }

    private var selectedCategorySummary: String? {
        guard !selectedCategories.isEmpty else { return nil }
        let formatter = ListFormatter()
        return formatter.string(from: selectedCategories.map(\.displayName).sorted())
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    header

                    if !BusinessDropTemplates.generalTemplates.isEmpty {
                        templateSection(
                            title: "Starter ideas",
                            subtitle: "Quick prompts any business can remix.",
                            templates: BusinessDropTemplates.generalTemplates
                        )
                    }

                    ForEach(filteredGroups, id: \.id) { metadata in
                        VStack(alignment: .leading, spacing: 16) {
                            Text(metadata.title)
                                .font(.title3.weight(.semibold))
                            Text(metadata.description)
                                .font(.footnote)
                                .foregroundColor(.secondary)

                            ForEach(metadata.categories, id: \.id) { category in
                                let templates = BusinessDropTemplates.templates(for: category)
                                VStack(alignment: .leading, spacing: 12) {
                                    Text(category.displayName)
                                        .font(.headline)
                                    Text(category.description)
                                        .font(.footnote)
                                        .foregroundColor(.secondary)

                                    if templates.isEmpty {
                                        Text("Templates coming soon for this category.")
                                            .font(.footnote)
                                            .foregroundColor(.secondary)
                                    } else {
                                        ForEach(templates, id: \.id) { template in
                                            BusinessTemplateCard(
                                                template: template,
                                                actionTitle: onApply == nil ? "" : "Use this idea",
                                                onApply: onApply.map { handler in
                                                    { template in
                                                        handler(template)
                                                        dismiss()
                                                    }
                                                }
                                            )
                                            .padding(.vertical, 4)
                                        }
                                    }
                                }
                                .padding(16)
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(Color(uiColor: .secondarySystemBackground))
                                )
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 24)
            }
            .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
            .navigationTitle("Drop template library")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }

    @ViewBuilder
    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Browse template ideas")
                .font(.title2.weight(.semibold))
            if let summary = selectedCategorySummary {
                Text("Showing highlights for \(summary).")
                    .font(.callout)
                    .foregroundColor(.secondary)
            } else {
                Text("Explore playbooks for every category, or add business categories to tailor this list.")
                    .font(.callout)
                    .foregroundColor(.secondary)
            }
        }
    }

    @ViewBuilder
    private func templateSection(title: String, subtitle: String, templates: [BusinessDropTemplate]) -> some View {
        if templates.isEmpty {
            EmptyView()
        } else {
            VStack(alignment: .leading, spacing: 16) {
                Text(title)
                    .font(.title3.weight(.semibold))
                Text(subtitle)
                    .font(.footnote)
                    .foregroundColor(.secondary)

                ForEach(templates, id: \.id) { template in
                    BusinessTemplateCard(
                        template: template,
                        actionTitle: onApply == nil ? "" : "Use this idea",
                        onApply: onApply.map { handler in
                            { template in
                                handler(template)
                                dismiss()
                            }
                        }
                    )
                    .padding(.vertical, 4)
                }
            }
        }
    }
}
private struct PhotoCaptureView: UIViewControllerRepresentable {
    let onCapture: (Data) -> Void
    let onCancel: () -> Void
    let onError: (String) -> Void

    func makeUIViewController(context: Context) -> PhotoCaptureController {
        let controller = PhotoCaptureController()
        controller.onCapture = onCapture
        controller.onCancel = onCancel
        controller.onError = onError
        return controller
    }

    func updateUIViewController(_ uiViewController: PhotoCaptureController, context: Context) {}

    final class PhotoCaptureController: UIViewController, AVCapturePhotoCaptureDelegate {
        var onCapture: ((Data) -> Void)?
        var onCancel: (() -> Void)?
        var onError: ((String) -> Void)?

        private let session = AVCaptureSession()
        private let output = AVCapturePhotoOutput()
        private var previewLayer: AVCaptureVideoPreviewLayer?
        private let captureButton = UIButton(type: .system)
        private let closeButton = UIButton(type: .system)

        override func viewDidLoad() {
            super.viewDidLoad()
            view.backgroundColor = .black
            configureSession()
            configurePreview()
            configureControls()
        }

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            if !session.isRunning {
                DispatchQueue.global(qos: .userInitiated).async { self.session.startRunning() }
            }
        }

        override func viewWillDisappear(_ animated: Bool) {
            super.viewWillDisappear(animated)
            if session.isRunning {
                session.stopRunning()
            }
        }

        private func configureSession() {
            session.beginConfiguration()
            session.sessionPreset = .photo
            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
                onError?("Camera unavailable.")
                session.commitConfiguration()
                return
            }
            do {
                let input = try AVCaptureDeviceInput(device: device)
                if session.canAddInput(input) {
                    session.addInput(input)
                }
                if session.canAddOutput(output) {
                    session.addOutput(output)
                }
            } catch {
                onError?("Failed to configure camera: \(error.localizedDescription)")
            }
            session.commitConfiguration()
        }
        
        private func configurePreview() {
            let previewLayer = AVCaptureVideoPreviewLayer(session: session)
            previewLayer.videoGravity = .resizeAspectFill
            previewLayer.frame = view.bounds
            view.layer.addSublayer(previewLayer)
            self.previewLayer = previewLayer
        }

        private func configureControls() {
            captureButton.translatesAutoresizingMaskIntoConstraints = false
            captureButton.backgroundColor = UIColor.white.withAlphaComponent(0.9)
            captureButton.setTitle("", for: .normal)
            captureButton.layer.cornerRadius = 35
            captureButton.addTarget(self, action: #selector(capturePhoto), for: .touchUpInside)

            closeButton.translatesAutoresizingMaskIntoConstraints = false
            closeButton.setTitle("Close", for: .normal)
            closeButton.setTitleColor(.white, for: .normal)
            closeButton.addTarget(self, action: #selector(closeTapped), for: .touchUpInside)

            view.addSubview(captureButton)
            view.addSubview(closeButton)

            NSLayoutConstraint.activate([
                captureButton.widthAnchor.constraint(equalToConstant: 70),
                captureButton.heightAnchor.constraint(equalToConstant: 70),
                captureButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
                captureButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),

                closeButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
                closeButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16)
            ])
        }

        @objc private func capturePhoto() {
            let settings = AVCapturePhotoSettings()
            output.capturePhoto(with: settings, delegate: self)
        }

        @objc private func closeTapped() {
            onCancel?()
        }

        func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
            if let error {
                onError?("Failed to capture photo: \(error.localizedDescription)")
                return
            }
            guard let data = photo.fileDataRepresentation() else {
                onError?("Unable to process captured photo.")
                return
            }
            onCapture?(data)
        }
    }
}
private struct VideoCaptureView: UIViewControllerRepresentable {
    let onCapture: (URL, TimeInterval, String) -> Void
    let onCancel: () -> Void
    let onError: (String) -> Void

    func makeUIViewController(context: Context) -> VideoCaptureController {
        let controller = VideoCaptureController()
        controller.onCapture = onCapture
        controller.onCancel = onCancel
        controller.onError = onError
        return controller
    }

    func updateUIViewController(_ uiViewController: VideoCaptureController, context: Context) {}

    final class VideoCaptureController: UIViewController, AVCaptureFileOutputRecordingDelegate {
        var onCapture: ((URL, TimeInterval, String) -> Void)?
        var onCancel: (() -> Void)?
        var onError: ((String) -> Void)?

        private let session = AVCaptureSession()
        private let movieOutput = AVCaptureMovieFileOutput()
        private var previewLayer: AVCaptureVideoPreviewLayer?
        private let recordButton = UIButton(type: .system)
        private let closeButton = UIButton(type: .system)
        private var isRecording = false

        override func viewDidLoad() {
            super.viewDidLoad()
            view.backgroundColor = .black
            configureSession()
            configurePreview()
            configureControls()
        }

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            if !session.isRunning {
                DispatchQueue.global(qos: .userInitiated).async { self.session.startRunning() }
            }
        }

        override func viewWillDisappear(_ animated: Bool) {
            super.viewWillDisappear(animated)
            if session.isRunning {
                session.stopRunning()
            }
        }

        private func configureSession() {
            session.beginConfiguration()
            session.sessionPreset = .high
            do {
                if let videoDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) {
                    let videoInput = try AVCaptureDeviceInput(device: videoDevice)
                    if session.canAddInput(videoInput) { session.addInput(videoInput) }
                }
                if let audioDevice = AVCaptureDevice.default(for: .audio) {
                    let audioInput = try AVCaptureDeviceInput(device: audioDevice)
                    if session.canAddInput(audioInput) { session.addInput(audioInput) }
                }
            } catch {
                onError?("Failed to configure camera: \(error.localizedDescription)")
            }
            if session.canAddOutput(movieOutput) {
                session.addOutput(movieOutput)
            }
            session.commitConfiguration()
        }

        private func configurePreview() {
            let previewLayer = AVCaptureVideoPreviewLayer(session: session)
            previewLayer.videoGravity = .resizeAspectFill
            previewLayer.frame = view.bounds
            view.layer.addSublayer(previewLayer)
            self.previewLayer = previewLayer
        }

        private func configureControls() {
            recordButton.translatesAutoresizingMaskIntoConstraints = false
            recordButton.backgroundColor = UIColor.red.withAlphaComponent(0.9)
            recordButton.layer.cornerRadius = 35
            recordButton.addTarget(self, action: #selector(toggleRecording), for: .touchUpInside)

            closeButton.translatesAutoresizingMaskIntoConstraints = false
            closeButton.setTitle("Close", for: .normal)
            closeButton.setTitleColor(.white, for: .normal)
            closeButton.addTarget(self, action: #selector(closeTapped), for: .touchUpInside)

            view.addSubview(recordButton)
            view.addSubview(closeButton)

            NSLayoutConstraint.activate([
                recordButton.widthAnchor.constraint(equalToConstant: 70),
                recordButton.heightAnchor.constraint(equalToConstant: 70),
                recordButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
                recordButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),

                closeButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
                closeButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16)
            ])
        }

        @objc private func toggleRecording() {
            if isRecording {
                movieOutput.stopRecording()
            } else {
                let url = FileManager.default.temporaryDirectory
                    .appendingPathComponent(UUID().uuidString)
                    .appendingPathExtension("mov")
                movieOutput.startRecording(to: url, recordingDelegate: self)
            }
            isRecording.toggle()
            UIView.animate(withDuration: 0.2) {
                self.recordButton.backgroundColor = self.isRecording ? UIColor.gray.withAlphaComponent(0.8) : UIColor.red.withAlphaComponent(0.9)
            }
        }

        @objc private func closeTapped() {
            if isRecording {
                movieOutput.stopRecording()
                isRecording = false
            }
            onCancel?()
        }

        func fileOutput(_ output: AVCaptureFileOutput, didFinishRecordingTo outputFileURL: URL, from connections: [AVCaptureConnection], error: Error?) {
            if let error {
                onError?("Failed to record video: \(error.localizedDescription)")
                return
            }
            let duration = CMTimeGetSeconds(output.recordedDuration)
            onCapture?(outputFileURL, duration, "video/quicktime")
        }
    }
}
private struct AudioRecorderSheet: View {
    let onComplete: (URL, TimeInterval) -> Void
    let onCancel: () -> Void
    let onError: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var recorder: AVAudioRecorder?
    @State private var recordedURL: URL?
    @State private var isRecording = false
    @State private var elapsed: TimeInterval = 0
    @State private var timer: Timer?

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Text(formatDuration(elapsed))
                    .font(.system(size: 36, weight: .semibold, design: .rounded))
                    .padding(.top, 40)

                Button {
                    toggleRecording()
                } label: {
                    Text(isRecording ? "Stop recording" : "Start recording")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(isRecording ? Color.red : Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }

                if recordedURL != nil && !isRecording {
                    Text("Recording ready. Tap Done to attach it to your drop.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }

                Spacer()
            }
            .padding()
            .navigationTitle("Record audio")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        cleanupRecording(save: false)
                        onCancel()
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        guard let url = recordedURL else {
                            onCancel()
                            dismiss()
                            return
                        }
                        cleanupRecording(save: true)
                        onComplete(url, elapsed)
                        dismiss()
                    }
                    .disabled(recordedURL == nil || isRecording)
                }
            }
        }
        .onDisappear {
            cleanupRecording(save: false)
        }
    }

    private func toggleRecording() {
        if isRecording {
            stopRecording(save: true)
        } else {
            startRecording()
        }
    }

    private func startRecording() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)

            let url = FileManager.default.temporaryDirectory
                .appendingPathComponent(UUID().uuidString)
                .appendingPathExtension("m4a")
            let settings: [String: Any] = [
                AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
                AVSampleRateKey: 44_100,
                AVNumberOfChannelsKey: 1,
                AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
            ]
            let recorder = try AVAudioRecorder(url: url, settings: settings)
            recorder.record()
            self.recorder = recorder
            recordedURL = url
            isRecording = true
            elapsed = 0
            startTimer()
        } catch {
            onError("Couldn't start recording audio: \(error.localizedDescription)")
            cleanupRecording(save: false)
            onCancel()
            dismiss()
        }
    }

    private func stopRecording(save: Bool) {
        recorder?.stop()
        recorder = nil
        isRecording = false
        stopTimer()
        if !save {
            if let url = recordedURL {
                try? FileManager.default.removeItem(at: url)
            }
            recordedURL = nil
        }
    }

    private func cleanupRecording(save: Bool) {
        if isRecording {
            stopRecording(save: save)
        } else if !save, let url = recordedURL {
            try? FileManager.default.removeItem(at: url)
            recordedURL = nil
        }
        stopTimer()
    }

    private func startTimer() {
        stopTimer()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            if let recorder = recorder {
                elapsed = recorder.currentTime
            }
        }
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }

    private func formatDuration(_ duration: TimeInterval) -> String {
        let totalSeconds = Int(duration.rounded())
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}
