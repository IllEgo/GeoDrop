import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var username: String = ""
    @State private var businessName: String = ""
    @State private var selectedCategories: Set<BusinessCategory> = []

    var body: some View {
        NavigationView {
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
                }
            }
            .navigationTitle("Profile")
        }
    }
}