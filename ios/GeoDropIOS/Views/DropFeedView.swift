import SwiftUI

struct DropFeedView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var showingGroupJoin = false

    var body: some View {
        NavigationView {
            VStack {
                if viewModel.groups.isEmpty {
                    VStack(spacing: 16) {
                        Text("Join a group to start discovering drops.")
                            .multilineTextAlignment(.center)
                        Button("Join group") { showingGroupJoin = true }
                            .buttonStyle(.borderedProminent)
                    }
                    .padding()
                } else {
                    List {
                        Section(header: groupSelector) {
                            ForEach(viewModel.drops) { drop in
                                DropRowView(drop: drop)
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle("Discover")
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

                Button("Details") { showingDetail = true }
                    .font(.subheadline)
            }
        }
        .padding(.vertical, 8)
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