import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var username: String = ""
    @State private var businessName: String = ""
    @State private var selectedCategories: Set<BusinessCategory> = []

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
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(BusinessCategory.all, id: \.id) { category in
                                    let isSelected = selectedCategories.contains(category)
                                    Button(action: {
                                        if isSelected {
                                            selectedCategories.remove(category)
                                        } else {
                                            selectedCategories.insert(category)
                                        }
                                    }) {
                                        Text(category.displayName)
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
                        Button("Save business profile") {
                            viewModel.updateBusinessProfile(name: businessName, categories: Array(selectedCategories))
                        }
                        .disabled(businessName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || selectedCategories.isEmpty)
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
    }
}
