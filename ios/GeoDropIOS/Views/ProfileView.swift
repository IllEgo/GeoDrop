import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var username: String = ""
    @State private var businessName: String = ""
    @State private var selectedCategories: Set<BusinessCategory> = []
    @State private var isShowingTemplateBrowser = false
    @State private var isShowingDashboard = false
    
    var body: some View {
        GeoDropNavigationContainer(subtitle: "Profile") {
            Form {
                if case let .signedIn(session) = viewModel.authState {
                    Section(header: Text("Account")) {
                        Text(session.user.email ?? "")
                        Toggle("Allow NSFW drops", isOn: Binding(
                            get: { viewModel.allowNsfw },
                            set: { viewModel.setAllowNsfw($0) }
                        ))
                    }

                    Section(header: Text("Username")) {
                        TextField("Explorer username", text: $username)
                            .textInputAutocapitalization(.never)
                            .disableAutocorrection(true)
                        Button("Save username") {
                            viewModel.updateExplorerUsername(to: username)
                        }
                        .disabled(username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                    .onAppear {
                        username = session.profile.username ?? ""
                    }

                    Section(header: Text("Business profile")) {
                        TextField("Business name", text: $businessName)
                        
                        VStack(alignment: .leading, spacing: 16) {
                            ForEach(BusinessCategory.grouped, id: \.id) { metadata in
                                VStack(alignment: .leading, spacing: 12) {
                                    Text(metadata.title)
                                        .font(.subheadline.weight(.semibold))
                                    Text(metadata.description)
                                        .font(.footnote)
                                        .foregroundColor(.secondary)

                                    LazyVGrid(columns: categoryGridColumns, spacing: 12) {
                                        ForEach(metadata.categories, id: \.id) { category in
                                            let isSelected = selectedCategories.contains(category)
                                            Button {
                                                if isSelected {
                                                    selectedCategories.remove(category)
                                                } else {
                                                    selectedCategories.insert(category)
                                                }
                                            } label: {
                                                VStack(alignment: .leading, spacing: 6) {
                                                    Text(category.displayName)
                                                        .font(.subheadline.weight(.semibold))
                                                        .foregroundColor(isSelected ? .accentColor : .primary)
                                                    Text(category.description)
                                                        .font(.caption)
                                                        .foregroundColor(isSelected ? Color.accentColor.opacity(0.8) : .secondary)
                                                }
                                                .padding(12)
                                                .frame(maxWidth: .infinity, alignment: .leading)
                                                .background(
                                                    RoundedRectangle(cornerRadius: 12)
                                                        .fill(isSelected ? Color.accentColor.opacity(0.15) : Color(uiColor: .secondarySystemBackground))
                                                )
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 12)
                                                        .stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 1.5)
                                                )
                                            }
                                            .buttonStyle(.plain)
                                        }
                                    }
                                }
                            }
                        }
                        .padding(.vertical, 4)

                        Button("Save business profile") {
                            viewModel.updateBusinessProfile(name: businessName, categories: Array(selectedCategories))
                        }
                        .disabled(businessName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || selectedCategories.isEmpty)
                        
                        if selectedCategories.isEmpty {
                            Text("Pick at least one category to unlock tailored templates and analytics.")
                                .font(.footnote)
                                .foregroundColor(.secondary)
                        } else {
                            Button {
                                isShowingTemplateBrowser = true
                            } label: {
                                Label("Browse drop templates", systemImage: "sparkles")
                            }

                            Button {
                                isShowingDashboard = true
                            } label: {
                                Label("View business dashboard", systemImage: "chart.bar.doc.horizontal")
                            }
                        }
                    }
                    .onAppear {
                        businessName = session.profile.businessName ?? ""
                        selectedCategories = Set(session.profile.businessCategories)
                    }

                    Section {
                        Button("Sign out", role: .destructive) {
                            viewModel.signOut()
                        }
                    }
                } else if viewModel.userMode == .guest {
                    Section {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("You're exploring as a guest.")
                                .font(.headline)
                            Text("Sign in to personalize your profile, join groups, and participate in drops.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 4)
                    }

                    Section(header: Text("Upgrade your account")) {
                        Button {
                            viewModel.beginAuthentication(for: .explorer)
                        } label: {
                            Label("Sign in as Explorer", systemImage: "figure.walk")
                        }

                        Button {
                            viewModel.beginAuthentication(for: .business)
                        } label: {
                            Label("Sign in as Business", systemImage: "briefcase.fill")
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $isShowingTemplateBrowser) {
            BusinessTemplateBrowserView(selectedCategories: selectedCategories)
        }
        .sheet(isPresented: $isShowingDashboard) {
            BusinessDashboardView()
        }
    }

    private var categoryGridColumns: [GridItem] {
        [GridItem(.adaptive(minimum: 160), spacing: 12)]
    }
}

struct BusinessDashboardView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var drops: [Drop] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var didLoad = false

    private var sortedDrops: [Drop] {
        drops.sorted { $0.createdAt > $1.createdAt }
    }

    private var liveDropsCount: Int {
        drops.filter { !$0.isExpired }.count
    }

    private var activeOfferCount: Int {
        drops.filter { !$0.isExpired && $0.dropType == .restaurantCoupon }.count
    }

    private var totalRedemptions: Int {
        drops.reduce(into: 0) { $0 += $1.redemptionCount }
    }

    private var uniqueRedemptions: Int {
        drops.reduce(into: Set<String>()) { partial, drop in
            partial.formUnion(drop.redeemedBy.keys)
        }.count
    }

    private var profile: UserProfile? {
        if case let .signedIn(session) = viewModel.authState {
            return session.profile
        }
        return nil
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoading && drops.isEmpty {
                    VStack(spacing: 16) {
                        ProgressView()
                        Text("Loading your business analytics…")
                            .font(.callout)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let message = errorMessage {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.largeTitle)
                            .foregroundColor(.orange)
                        Text(message)
                            .multilineTextAlignment(.center)
                        Button("Retry") {
                            Task { await reload() }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 24) {
                            header
                            metricsGrid
                            dropList
                        }
                        .padding(24)
                    }
                    .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
                }
            }
            .navigationTitle("Business dashboard")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .task {
                await loadIfNeeded()
            }
        }
    }

    @ViewBuilder
    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let name = profile?.businessName, !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(name)
                    .font(.title2.weight(.semibold))
            }
            Text("A snapshot of how your drops are performing.")
                .font(.callout)
                .foregroundColor(.secondary)
        }
    }

    private var metricsGrid: some View {
        let columns = [GridItem(.flexible()), GridItem(.flexible())]
        return LazyVGrid(columns: columns, spacing: 16) {
            metricTile(value: liveDropsCount, label: "Live drops")
            metricTile(value: totalRedemptions, label: "Total redemptions")
            metricTile(value: uniqueRedemptions, label: "Unique redeemers")
            metricTile(value: activeOfferCount, label: "Active offers")
        }
    }

    @ViewBuilder
    private var dropList: some View {
        if sortedDrops.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text("No drops yet")
                    .font(.headline)
                Text("Create a drop from the composer to see analytics here.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            VStack(alignment: .leading, spacing: 16) {
                Text("Recent drops")
                    .font(.headline)
                LazyVStack(spacing: 16) {
                    ForEach(sortedDrops) { drop in
                        BusinessDropDashboardCard(drop: drop)
                    }
                }
            }
        }
    }

    private func metricTile(value: Int, label: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\(value)")
                .font(.title.weight(.semibold))
            Text(label)
                .font(.footnote)
                .foregroundColor(.secondary)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(uiColor: .secondarySystemBackground))
        )
    }

    private func loadIfNeeded() async {
        guard !didLoad else { return }
        didLoad = true
        await reload()
    }

    private func reload() async {
        isLoading = true
        errorMessage = nil
        do {
            let fetched = try await viewModel.fetchBusinessDrops()
            drops = fetched
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}

private struct BusinessDropDashboardCard: View {
    let drop: Drop

    private var statusText: String {
        drop.isExpired ? "Expired" : "Live"
    }

    private var statusColor: Color {
        drop.isExpired ? .secondary : .green
    }

    private var dropTypeLabel: String {
        switch drop.dropType {
        case .community: return "Community"
        case .restaurantCoupon: return "Offer"
        case .tourStop: return "Tour stop"
        }
    }

    private var dropTypeIcon: String {
        switch drop.dropType {
        case .community: return "person.2"
        case .restaurantCoupon: return "tag"
        case .tourStop: return "flag"
        }
    }

    private var redemptionSummary: (total: Int, unique: Int) {
        (drop.redemptionCount, drop.redeemedBy.keys.count)
    }

    private var collectCount: Int {
        drop.collectedBy.count
    }

    private var createdDateString: String {
        drop.createdAt.formatted(date: .abbreviated, time: .shortened)
    }

    private var headline: String {
        let trimmed = drop.text.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty { return trimmed }
        if let description = drop.description?.trimmingCharacters(in: .whitespacesAndNewlines), !description.isEmpty {
            return description
        }
        switch drop.dropType {
        case .community: return "Community drop"
        case .restaurantCoupon: return "Offer"
        case .tourStop: return "Tour stop"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(headline)
                    .font(.headline)
                Spacer()
                Text(statusText)
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(statusColor.opacity(0.15))
                    .foregroundColor(statusColor)
                    .clipShape(Capsule())
            }

            if let description = drop.description?.trimmingCharacters(in: .whitespacesAndNewlines), !description.isEmpty {
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            HStack(spacing: 16) {
                Label(dropTypeLabel, systemImage: dropTypeIcon)
                if let group = drop.groupCode, !group.isEmpty {
                    Label("Group \(group)", systemImage: "person.3")
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)

            if drop.dropType == .restaurantCoupon {
                let summary = redemptionSummary
                HStack(spacing: 16) {
                    Label("\(summary.total) redemptions", systemImage: "creditcard")
                    Label("\(summary.unique) unique", systemImage: "person.crop.circle.badge.checkmark")
                }
                .font(.footnote)

                if let code = drop.redemptionCode, !code.isEmpty {
                    Text("Code: \(code)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                if let limit = drop.redemptionLimit {
                    Text("Limit: \(limit)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            } else {
                Label("\(collectCount) collects", systemImage: "map")
                    .font(.footnote)
            }

            HStack {
                Text("Dropped \(createdDateString)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Spacer()
                if drop.likeCount > 0 {
                    Label("\(drop.likeCount) likes", systemImage: "heart")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(uiColor: .secondarySystemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.accentColor.opacity(0.15))
        )
    }
}
