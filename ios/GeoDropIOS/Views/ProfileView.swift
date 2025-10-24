import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme
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
                                        .foregroundColor(geoDropTheme.colors.onSurface)
                                    Text(metadata.description)
                                        .font(.footnote)
                                        .foregroundColor(geoDropTheme.colors.onSurfaceVariant)

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
                                                        .foregroundColor(isSelected ? geoDropTheme.colors.primary : geoDropTheme.colors.onSurface)
                                                    Text(category.description)
                                                        .font(.caption)
                                                        .foregroundColor(isSelected ? geoDropTheme.colors.primary.opacity(0.8) : geoDropTheme.colors.onSurfaceVariant)
                                                }
                                                .padding(12)
                                                .frame(maxWidth: .infinity, alignment: .leading)
                                                .background(
                                                    RoundedRectangle(cornerRadius: 12)
                                                        .fill(isSelected ? geoDropTheme.colors.primary.opacity(0.15) : geoDropTheme.colors.surfaceVariant)
                                                )
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 12)
                                                        .stroke(isSelected ? geoDropTheme.colors.primary : geoDropTheme.colors.outlineVariant.opacity(0.6), lineWidth: 1.5)
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
                                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                        } else {
                            Button {
                                isShowingTemplateBrowser = true
                            } label: {
                                Label("Browse drop templates", systemImage: "sparkles")
                                    .foregroundColor(geoDropTheme.colors.primary)
                            }

                            Button {
                                isShowingDashboard = true
                            } label: {
                                Label("View business dashboard", systemImage: "chart.bar.doc.horizontal")
                                    .foregroundColor(geoDropTheme.colors.primary)
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
                                .foregroundColor(geoDropTheme.colors.onSurface)
                            Text("Sign in to personalize your profile, join groups, and participate in drops.")
                                .font(.subheadline)
                                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
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
            .tint(geoDropTheme.colors.primary)
            .scrollContentBackgroundHiddenIfAvailable()
            .background(geoDropTheme.colors.background)
        }
        .sheet(isPresented: $isShowingTemplateBrowser) {
            BusinessTemplateBrowserView(selectedCategories: selectedCategories)
        }
        .sheet(isPresented: $isShowingDashboard) {
            BusinessDashboardView()
            .modifier(BusinessDashboardSheetPresentation())
        }
    }

    private var categoryGridColumns: [GridItem] {
        [GridItem(.adaptive(minimum: 160), spacing: 12)]
    }
}

private extension View {
    @ViewBuilder
    func scrollContentBackgroundHiddenIfAvailable() -> some View {
        if #available(iOS 16.0, *) {
            self.scrollContentBackground(.hidden)
        } else {
            self
        }
    }
}

struct BusinessDashboardView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.geoDropTheme) private var geoDropTheme

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
        Group {
            if #available(iOS 16.0, *) {
                NavigationStack { dashboardContent }
            } else {
                NavigationView { dashboardContent }
                    .navigationViewStyle(StackNavigationViewStyle())
            }
        }
    }

    @ViewBuilder
    private var dashboardContent: some View {
        Group {
            if isLoading && drops.isEmpty {
                VStack(spacing: 16) {
                    ProgressView()
                        .tint(geoDropTheme.colors.primary)
                    Text("Loading your business analytics…")
                        .font(.callout)
                        .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let message = errorMessage {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.title2)
                        .foregroundColor(geoDropTheme.colors.tertiary)
                    Text(message)
                        .multilineTextAlignment(.center)
                        .foregroundColor(geoDropTheme.colors.onSurface)
                    Button("Retry") {
                        Task { await reload() }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(geoDropTheme.colors.primary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 24) {
                        header
                        metricsGrid
                        dropList
                    }
                    .padding(24)
                }
                .background(geoDropTheme.colors.background.ignoresSafeArea())
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

    @ViewBuilder
    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let name = profile?.businessName, !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(name)
                    .font(.title2.weight(.semibold))
                    .foregroundColor(geoDropTheme.colors.onSurface)
            }
            Text("A snapshot of how your drops are performing.")
                .font(.callout)
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
        }
    }

    private var metricsGrid: some View {
        let metrics = [
            (value: liveDropsCount, label: "Live drops"),
            (value: totalRedemptions, label: "Total redemptions"),
            (value: uniqueRedemptions, label: "Unique redeemers"),
            (value: activeOfferCount, label: "Active offers")
        ]

        let columns = Array(repeating: GridItem(.flexible(), spacing: 16), count: 2)

        return LazyVGrid(columns: columns, alignment: .leading, spacing: 16) {
            ForEach(metrics, id: \.label) { item in
                metricTile(value: item.value, label: item.label)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    @ViewBuilder
    private var dropList: some View {
        if sortedDrops.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text("No drops yet")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(geoDropTheme.colors.onSurface)
                Text("Create a drop from the composer to see analytics here.")
                    .font(.footnote)
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            VStack(alignment: .leading, spacing: 16) {
                Text("Recent drops")
                    .font(.headline)
                    .foregroundColor(geoDropTheme.colors.onSurface)
                LazyVStack(alignment: .leading, spacing: 16) {
                    ForEach(sortedDrops) { drop in
                        BusinessDropDashboardCard(drop: drop)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private func metricTile(value: Int, label: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\(value)")
                .font(.title.weight(.semibold))
                .foregroundColor(geoDropTheme.colors.onSurface)
            Text(label)
                .font(.footnote)
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(geoDropTheme.colors.surfaceVariant)
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
    @Environment(\.geoDropTheme) private var geoDropTheme

    private var statusText: String {
        drop.isExpired ? "Expired" : "Live"
    }

    private var statusColor: Color {
        drop.isExpired ? geoDropTheme.colors.onSurfaceVariant : geoDropTheme.colors.primary
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
                    .foregroundColor(geoDropTheme.colors.onSurface)
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
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            }

            HStack(spacing: 16) {
                Label(dropTypeLabel, systemImage: dropTypeIcon)
                if let group = drop.groupCode, !group.isEmpty {
                    Label("Group \(group)", systemImage: "person.3")
                }
            }
            .font(.caption)
            .foregroundColor(geoDropTheme.colors.onSurfaceVariant)

            if drop.dropType == .restaurantCoupon {
                let summary = redemptionSummary
                HStack(spacing: 16) {
                    Label("\(summary.total) redemptions", systemImage: "creditcard")
                    Label("\(summary.unique) unique", systemImage: "person.crop.circle.badge.checkmark")
                }
                .font(.footnote)
                .foregroundColor(geoDropTheme.colors.onSurfaceVariant)

                if let code = drop.redemptionCode, !code.isEmpty {
                    Text("Code: \(code)")
                        .font(.caption)
                        .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                }
                if let limit = drop.redemptionLimit {
                    Text("Limit: \(limit)")
                        .font(.caption)
                        .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                }
            } else {
                Label("\(collectCount) collects", systemImage: "map")
                    .font(.footnote)
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            }

            HStack {
                Text("Dropped \(createdDateString)")
                    .font(.caption)
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                Spacer()
                if drop.likeCount > 0 {
                    Label("\(drop.likeCount) likes", systemImage: "heart")
                        .font(.caption)
                        .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                }
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
                .stroke(geoDropTheme.colors.outlineVariant.opacity(0.6))
        )
    }
}

private struct BusinessDashboardSheetPresentation: ViewModifier {
    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        } else {
            content
        }
    }
}
