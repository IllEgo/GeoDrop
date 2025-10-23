import SwiftUI
import CoreLocation
import AVKit

struct DropFeedView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var showingGroupJoin = false
    @State private var mapCameraState = GoogleMapCameraState(
        latitude: Self.defaultCoordinate.latitude,
        longitude: Self.defaultCoordinate.longitude,
        zoom: Self.defaultZoom
    )
    @State private var selectedDropID: Drop.ID?
    @State private var mapHeightFraction: CGFloat = 0.45
    @State private var dragStartFraction: CGFloat?
    @State private var shouldAnimateCamera = false
    
    private static let defaultCoordinate = CLLocationCoordinate2D(latitude: 37.7749, longitude: -122.4194)
    private static let defaultZoom: Float = GoogleMapCameraState.defaultZoom

    var body: some View {
        GeoDropNavigationContainer(
            subtitle: "Discover",
            trailing: { topBarActions }
        ) {
            VStack(spacing: 0) {
                if viewModel.groups.isEmpty {
                    VStack(spacing: 16) {
                        Text("Join a group to start discovering drops.")
                            .multilineTextAlignment(.center)
                        Button("Join group") { showingGroupJoin = true }
                            .buttonStyle(.borderedProminent)
                    }
                    .padding()
                } else {
                    VStack(spacing: 0) {
                        groupSelector
                            .padding(.horizontal)
                            .padding(.top, 8)

                        GeometryReader { geometry in
                            let totalHeight = geometry.size.height
                            let minFraction: CGFloat = 0.25
                            let maxFraction: CGFloat = 0.8
                            let dividerHeight: CGFloat = 16
                            let clampedFraction = min(max(mapHeightFraction, minFraction), maxFraction)
                            let mapHeight = totalHeight * clampedFraction
                            let listHeight = max(totalHeight - mapHeight - dividerHeight, 0)

                            let drag = DragGesture(minimumDistance: 0)
                                .onChanged { value in
                                    if dragStartFraction == nil {
                                        dragStartFraction = clampedFraction
                                    }
                                    let start = dragStartFraction ?? clampedFraction
                                    let translationFraction = value.translation.height / totalHeight
                                    let proposed = start + translationFraction
                                    mapHeightFraction = min(max(proposed, minFraction), maxFraction)
                                }
                                .onEnded { _ in
                                    dragStartFraction = nil
                                }

                            VStack(spacing: 0) {
                                GoogleMapView(
                                    drops: viewModel.drops,
                                    selectedDropID: $selectedDropID,
                                    cameraState: $mapCameraState,
                                    shouldAnimateCamera: $shouldAnimateCamera,
                                    onSelectDrop: { drop in focus(on: drop) }
                                )
                                .frame(height: mapHeight)
                                .clipped()

                                ZStack {
                                    Color(uiColor: .systemBackground)
                                    Capsule()
                                        .fill(Color.secondary.opacity(0.4))
                                        .frame(width: 48, height: 6)
                                }
                                .frame(height: dividerHeight)
                                .contentShape(Rectangle())
                                .gesture(drag)

                                ScrollView {
                                    LazyVStack(spacing: 12) {
                                        ForEach(viewModel.drops) { drop in
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
                                .frame(height: listHeight)
                                .background(Color(uiColor: .systemGroupedBackground))
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
        }
        .sheet(isPresented: $showingGroupJoin) {
            GroupManagementView()
                .environmentObject(viewModel)
        }
        .onAppear { updateSelection(for: viewModel.drops) }
        .onChange(of: viewModel.drops) { updateSelection(for: $0) }
    }
    
    private var topBarActions: some View {
        HStack(spacing: 12) {
            Button {
                showingGroupJoin = true
            } label: {
                topBarIcon(systemName: "person.3")
                    .accessibilityLabel("Manage groups")
            }

            Button {
                Task { await viewModel.refreshDrops() }
            } label: {
                topBarIcon(systemName: "arrow.clockwise")
                    .accessibilityLabel("Refresh drops")
            }
        }
        .buttonStyle(.plain)
    }

    private func topBarIcon(systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.title3.weight(.semibold))
            .frame(width: 36, height: 36)
            .foregroundColor(.accentColor)
            .background(Color.accentColor.opacity(0.12))
            .clipShape(Circle())
    }
    private var groupSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(viewModel.groups) { group in
                    let isSelected = group.code == viewModel.selectedGroupCode
                    Button(action: {
                        viewModel.selectedGroupCode = group.code
                        Task { await viewModel.refreshDrops() }
                    }) {
                        Text(group.code)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(isSelected ? Color.accentColor : Color(uiColor: .secondarySystemBackground))
                            .foregroundColor(isSelected ? .white : .primary)
                            .cornerRadius(12)
                    }
                }
            }
            .padding(.vertical, 8)
        }
    }
}

struct DropRowView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    let drop: Drop
    let isSelected: Bool
    let onSelect: () -> Void
    @State private var showingDetail = false
    @State private var isExpanded = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button {
                onSelect()
                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    isExpanded.toggle()
                }
            } label: {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .center, spacing: 8) {
                        Text(drop.displayTitle)
                            .font(.headline)
                            .multilineTextAlignment(.leading)
                        Spacer(minLength: 12)
                        if drop.requiresRedemption() {
                            Label("Redeem", systemImage: "tag")
                                .font(.caption)
                                .foregroundColor(.orange)
                                .labelStyle(.titleAndIcon)
                        }
                        Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(.secondary)
                    }

                    if let description = drop.description, !description.isEmpty {
                        Text(description)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .lineLimit(isExpanded ? nil : 2)
                            .multilineTextAlignment(.leading)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)

            if isExpanded {
                VStack(alignment: .leading, spacing: 12) {
                    if hasMediaPreview {
                        mediaPreview
                            .transition(.opacity.combined(with: .move(edge: .top)))
                    }

                    if let description = expandedDescriptionText, description != headerDescriptionText {
                        Text(description)
                            .font(.body)
                            .foregroundColor(.primary)
                    }

                    HStack(spacing: 16) {
                        Button(action: toggleLike) {
                            Label("Like", systemImage: drop.isLiked(by: currentUserId) == .liked ? "hand.thumbsup.fill" : "hand.thumbsup")
                        }
                        .buttonStyle(.borderless)

                        Button("Collect", action: markCollected)
                            .buttonStyle(.bordered)
                        
                
                        Spacer()

                        Button("Details") {
                            onSelect()
                            showingDetail = true
                        }
                        .font(.subheadline)
                    }
                }
                .transition(.opacity)            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(isSelected ? Color.accentColor.opacity(0.15) : Color(uiColor: .secondarySystemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 1)
        )
        .contentShape(RoundedRectangle(cornerRadius: 16))
        .onTapGesture { onSelect() }
        .animation(.easeInOut, value: isSelected)
        .sheet(isPresented: $showingDetail) {
            DropDetailView(drop: drop)
                .environmentObject(viewModel)
        }
    }

    private var currentUserId: String? {
        if case let .signedIn(session) = viewModel.authState {
            return session.user.uid
        }
        return nil
    }

    private func toggleLike() {
        let status = drop.isLiked(by: currentUserId) == .liked ? DropLikeStatus.none : .liked
        viewModel.like(drop: drop, status: status)
    }

    private func markCollected() {
        viewModel.markCollected(drop: drop)
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

    private var hasMediaPreview: Bool {
        guard drop.mediaURL != nil else { return false }
        switch drop.contentType {
        case .photo, .video:
            return true
        default:
            return false
        }
    }

    @ViewBuilder
    private var mediaPreview: some View {
        switch drop.contentType {
        case .photo:
            if let url = drop.mediaURL {
                AsyncImage(url: url, transaction: Transaction(animation: .easeInOut)) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                            .progressViewStyle(.circular)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .clipped()
                    case .failure:
                        Image(systemName: "photo")
                            .font(.largeTitle)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    @unknown default:
                        EmptyView()
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 220)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color(uiColor: .tertiarySystemFill))
                )
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
                            .stroke(Color.secondary.opacity(0.2))
                    )
            }
        default:
            EmptyView()
        }
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
