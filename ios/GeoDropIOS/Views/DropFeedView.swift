import SwiftUI
import CoreLocation

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
        NavigationView {
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
            .geoDropNavigationTitle(subtitle: "Discover")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showingGroupJoin = true
                    } label: {
                        Image(systemName: "person.3")
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task { await viewModel.refreshDrops() }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .sheet(isPresented: $showingGroupJoin) {
                GroupManagementView()
                    .environmentObject(viewModel)
            }
        }
        .onAppear { updateSelection(for: viewModel.drops) }
        .onChange(of: viewModel.drops) { updateSelection(for: $0) }
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

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(drop.displayTitle)
                    .font(.headline)
                Spacer()
                if drop.requiresRedemption() {
                    Label("Redeem", systemImage: "tag")
                        .font(.caption)
                        .foregroundColor(.orange)
                }
            }

            if let description = drop.description, !description.isEmpty {
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
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
