import SwiftUI

struct GroupManagementView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var groupCode: String = ""
    @State private var allowCreate: Bool = false
    private let showsCloseButton: Bool

    init(showsCloseButton: Bool = true) {
        self.showsCloseButton = showsCloseButton
    }
    
    var body: some View {
        GeoDropNavigationContainer(
            subtitle: "Groups",
            trailing: {
                if showsCloseButton {
                    Button("Close", action: dismiss.callAsFunction)
                        .font(.callout.weight(.semibold))
                        .foregroundColor(.accentColor)
                }
            }
        ) {
            Form {
                Section(header: Text("Join a group")) {
                    TextField("Group code", text: $groupCode)
                        .textInputAutocapitalization(.characters)
                        .disableAutocorrection(true)
                    Toggle("Create group if missing", isOn: $allowCreate)
                    Button("Join") {
                        viewModel.joinGroup(code: groupCode, allowCreate: allowCreate)
                        groupCode = ""
                    }
                    .disabled(groupCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }

                if !viewModel.groups.isEmpty {
                    Section(header: Text("Your groups")) {
                        ForEach(viewModel.groups) { group in
                            HStack {
                                Text(group.code)
                                Spacer()
                                if group.code == viewModel.selectedGroupCode {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.accentColor)
                                }
                            }
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    viewModel.leaveGroup(code: group.code)
                                } label: {
                                    Label("Leave", systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
