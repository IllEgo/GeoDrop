import SwiftUI

struct MainTabView: View {
    @EnvironmentObject private var viewModel: AppViewModel

    var body: some View {
        TabView {
            ProfileView()
                .tabItem {
                    Label("Profile", systemImage: "person.circle")
                }

            CreateDropView()
                .tabItem {
                    Label("Drop", systemImage: "plus.circle")
                }

            GroupManagementView(showsCloseButton: false)
                .tabItem {
                    Label("Groups", systemImage: "person.3")
                }
        }
    }
}
