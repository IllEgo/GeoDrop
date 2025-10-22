import SwiftUI
import PhotosUI
import UIKit

struct CreateDropView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var text = ""
    @State private var description = ""
    @State private var isAnonymous = false
    @State private var dropType: DropType = .community
    @State private var contentType: DropContentType = .text

    // These are fine across iOS versions
    @State private var mediaPreview: Image?
    @State private var mediaData: Data?
    @State private var mediaMimeType: String?

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Message")) {
                    TextField("Headline", text: $text)
                    TextField("Description", text: $description)
                        .lineLimit(3)
                    Toggle("Post anonymously", isOn: $isAnonymous)
                }

                Section(header: Text("Drop type")) {
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

                if contentType == .photo {
                    if #available(iOS 16.0, *) {
                        PhotoPickerSection(
                            mediaPreview: $mediaPreview,
                            mediaData: $mediaData,
                            mediaMimeType: $mediaMimeType
                        )
                    } else {
                        Section(header: Text("Media")) {
                            Label("Photo uploads require iOS 16", systemImage: "exclamationmark.triangle")
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .geoDropNavigationTitle(subtitle: "New drop")
            .navigationBarItems(trailing:
                Button("Drop") { Task { await submit() } }
                    .disabled(!canSubmit)
            )
        }
    }

    private var canSubmit: Bool {
        if contentType == .photo && mediaData == nil { return false }
        return !(viewModel.selectedGroupCode?.isEmpty ?? true)
    }

    private func submit() async {
        guard let groupCode = viewModel.selectedGroupCode, !groupCode.isEmpty else { return }
        var mediaPayload: NewDropRequest.MediaPayload?
        if let data = mediaData, let mimeType = mediaMimeType {
            let ext = mimeType.components(separatedBy: "/").last ?? "jpg"
            mediaPayload = .init(data: data, mimeType: mimeType, fileExtension: ext)
        }
        let request = NewDropRequest(
            text: text,
            description: description.isEmpty ? nil : description,
            isAnonymous: isAnonymous,
            dropType: dropType,
            contentType: contentType,
            media: mediaPayload
        )
        await viewModel.createDrop(request: request)
        if viewModel.errorMessage == nil {
            text = ""; description = ""; isAnonymous = false
            mediaPreview = nil; mediaData = nil; mediaMimeType = nil
        }
    }
}

@available(iOS 16.0, *)
private struct PhotoPickerSection: View {
    @Binding var mediaPreview: Image?
    @Binding var mediaData: Data?
    @Binding var mediaMimeType: String?

    @State private var selectedPhoto: PhotosPickerItem?

    var body: some View {
        Section(header: Text("Media")) {
            PhotosPicker(selection: $selectedPhoto, matching: .images, photoLibrary: .shared()) {
                HStack {
                    Image(systemName: "photo")
                    Text(mediaData == nil ? "Select photo" : "Change photo")
                }
            }
            if let mediaPreview {
                mediaPreview
                    .resizable()
                    .scaledToFit()
                    .frame(maxHeight: 200)
            }
        }
        .onChange(of: selectedPhoto) { newValue in
            Task { await loadPhoto(item: newValue) }
        }
    }

    private func loadPhoto(item: PhotosPickerItem?) async {
        guard let item else { return }
        do {
            if let data = try await item.loadTransferable(type: Data.self) {
                mediaData = data
                mediaMimeType = item.supportedContentTypes.first?.preferredMIMEType ?? "image/jpeg"
                if let uiImage = UIImage(data: data) {
                    mediaPreview = Image(uiImage: uiImage)
                }
            }
        } catch {
            print("GeoDrop: Failed to load photo \(error)")
        }
    }
}
