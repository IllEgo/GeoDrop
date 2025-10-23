import SwiftUI

struct MainTabView: View {
    @EnvironmentObject private var viewModel: AppViewModel

    var body: some View {
        TabView {
            DropFeedView()
                .tabItem {
                    Label("Discover", systemImage: "map")
                }

            CreateDropView()
                .tabItem {
                    Label("Drop", systemImage: "plus.circle")
                }

            GroupManagementView(showsCloseButton: false)
                .tabItem {
                    Label("Groups", systemImage: "person.3")
                }
            
            ProfileView()
                .tabItem {
                    Label("Profile", systemImage: "person.circle")
                }
        }
    }
}
