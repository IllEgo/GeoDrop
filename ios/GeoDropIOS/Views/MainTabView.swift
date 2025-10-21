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

            ProfileView()
                .tabItem {
                    Label("Profile", systemImage: "person.circle")
                }
        }
    }
}