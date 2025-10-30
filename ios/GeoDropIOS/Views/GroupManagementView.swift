import SwiftUI

struct GroupManagementView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.geoDropTheme) private var geoDropTheme
    @State private var groupCode: String = ""
    @State private var allowCreate: Bool = false
    private let showsCloseButton: Bool

    init(showsCloseButton: Bool = true) {
        self.showsCloseButton = showsCloseButton
    }
    
    var body: some View {
        GeoDropNavigationContainer(
            trailing: {
                if showsCloseButton {
                    Button("Close", action: dismiss.callAsFunction)
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(geoDropTheme.colors.primary)
                }
            }
        ) {
            if #available(iOS 16.0, *) {
                formContent
                    .scrollContentBackground(.hidden)
            } else {
                formContent
            }
        }
    }
}

extension GroupManagementView {
    @ViewBuilder
    private var formContent: some View {
        Form {
            Section {
                TextField("Group code", text: $groupCode)
                    .textInputAutocapitalization(.characters)
                    .disableAutocorrection(true)
                Toggle("Create group if missing", isOn: $allowCreate)
                Button {
                    viewModel.joinGroup(code: groupCode, allowCreate: allowCreate)
                    groupCode = ""
                } label: {
                    Label("Join group", systemImage: "person.crop.circle.badge.plus")
                        .font(geoDropTheme.typography.body.weight(.semibold))
                }
                .disabled(groupCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            } header: {
                FormSectionHeader(
                    title: "Join a group",
                    subtitle: "Enter a code from an organizer to collaborate with nearby explorers.",
                    systemImage: "person.3.sequence"
                )
            } footer: {
                Text("Enable \"Create group\" if you're the first to use a new code and want to host your own community.")
                    .font(.footnote)
                    .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
            }

            if !viewModel.groups.isEmpty {
                Section {
                    ForEach(viewModel.groups) { group in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(group.code)
                                    .font(geoDropTheme.typography.body.weight(.semibold))
                                if group.code == viewModel.selectedGroupCode {
                                    Text("Currently active")
                                        .font(.caption)
                                        .foregroundColor(geoDropTheme.colors.primary)
                                }
                            }
                            Spacer()
                            if group.code == viewModel.selectedGroupCode {
                                Image(systemName: "checkmark")
                                    .foregroundColor(geoDropTheme.colors.primary)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            viewModel.selectedGroupCode = group.code
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                viewModel.leaveGroup(code: group.code)
                            } label: {
                                Label("Leave", systemImage: "trash")
                            }
                        }
                    }
                } header: {
                    FormSectionHeader(
                        title: "Your groups",
                        subtitle: "Switch between joined groups to tailor drops to each community.",
                        systemImage: "person.2.badge.gearshape"
                    )
                } footer: {
                    Text("Tap a group to make it active. Leaving removes it from your device immediately.")
                        .font(.footnote)
                        .foregroundColor(geoDropTheme.colors.onSurfaceVariant)
                }
            }
        }
        .tint(geoDropTheme.colors.primary)
        .background(geoDropTheme.colors.background)
    }
}
