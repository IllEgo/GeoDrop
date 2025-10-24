import SwiftUI

struct MainTabView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.geoDropTheme) private var geoDropTheme

    var body: some View {
        ZStack(alignment: .bottom) {
            DropFeedView()
                .zIndex(0)

            if viewModel.isShowingAccountMenu {
                Color.black.opacity(0.001)
                    .ignoresSafeArea()
                    .onTapGesture { viewModel.isShowingAccountMenu = false }
                    .zIndex(1)
            }

            MainBottomBar(
                isAccountMenuPresented: accountMenuBinding,
                accountMenu: AccountMenuView(onDismiss: { viewModel.isShowingAccountMenu = false })
                    .environmentObject(viewModel),
                onAccountTapped: { viewModel.toggleAccountMenu() },
                onDropTapped: { viewModel.presentDropComposer() },
                onGroupsTapped: { viewModel.toggleGroupMenu() },
                isDropActive: viewModel.isShowingDropComposer,
                isGroupsMenuPresented: viewModel.isShowingGroupMenu,
                isDropEnabled: canPresentDropComposer,
                dropLabel: dropButtonLabel,
                isDropInFlight: viewModel.isPerformingAction
            )
            .environmentObject(viewModel)
            .zIndex(2)
        }
        .background(geoDropTheme.colors.background)
        .ignoresSafeArea(edges: .bottom)
        .modifier(AccountMenuSheetModifier(isPresented: accountMenuBinding) {
            AccountMenuView(onDismiss: { viewModel.isShowingAccountMenu = false })
                .environmentObject(viewModel)
        })
        .sheet(isPresented: Binding(
            get: { viewModel.isShowingDropComposer },
            set: { viewModel.isShowingDropComposer = $0 }
        )) {
            CreateDropView(onDismiss: viewModel.dismissDropComposer)
                .environmentObject(viewModel)
        }
        .sheet(isPresented: Binding(
            get: { viewModel.isShowingGroupMenu },
            set: { viewModel.isShowingGroupMenu = $0 }
        )) {
            GroupMenuSheet()
                .environmentObject(viewModel)
        }
        .sheet(isPresented: Binding(
            get: { viewModel.isShowingGroupManagement },
            set: { viewModel.isShowingGroupManagement = $0 }
        )) {
            GroupManagementView()
                .environmentObject(viewModel)
        }
        .sheet(isPresented: Binding(
            get: { viewModel.isShowingNotificationRadius },
            set: { viewModel.isShowingNotificationRadius = $0 }
        )) {
            NotificationRadiusSheet(initialRadius: viewModel.notificationRadiusMeters) { newValue in
                viewModel.setNotificationRadius(newValue)
            }
        }
    }

    private var accountMenuBinding: Binding<Bool> {
        Binding(
            get: { viewModel.isShowingAccountMenu },
            set: { viewModel.isShowingAccountMenu = $0 }
        )
    }

    private var canPresentDropComposer: Bool {
        guard viewModel.userMode?.canParticipate == true else { return false }
        if case .signedIn = viewModel.authState {
            return true
        }
        return false
    }

    private var dropButtonLabel: String {
        viewModel.isPerformingAction ? "Dropping…" : "Drop something"
    }
}

private struct AccountMenuSheetModifier<MenuContent: View>: ViewModifier {
    @Binding var isPresented: Bool
    let menuContent: () -> MenuContent

    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content
        } else {
            content.sheet(isPresented: $isPresented, content: menuContent)
        }
    }
}
