import SwiftUI

struct DropDetailView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    let drop: Drop
    @State private var detailCameraState: GoogleMapCameraState
    @State private var detailShouldAnimate = false
    @State private var detailSelectedDropID: Drop.ID?
    @Environment(\.dismiss) private var dismiss
    @State private var reportReason: String = ""
    @State private var showingReport = false
    
    init(drop: Drop) {
        self.drop = drop
        _detailCameraState = State(
            initialValue: GoogleMapCameraState(
                latitude: drop.latitude,
                longitude: drop.longitude,
                zoom: GoogleMapCameraState.defaultZoom
            )
        )
        _detailSelectedDropID = State(initialValue: drop.id)
    }
    
    var body: some View {
        GeoDropNavigationContainer(
            subtitle: "Drop",
            trailing: {
                Button("Close", action: dismiss.callAsFunction)
                    .font(.callout.weight(.semibold))
                    .foregroundColor(.accentColor)
            }
        ) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(drop.displayTitle)
                        .font(.title)
                        .fontWeight(.bold)

                    if let description = drop.description, !description.isEmpty {
                        Text(description)
                    }
                    
                    GoogleMapView(
                        drops: [drop],
                        selectedDropID: $detailSelectedDropID,
                        cameraState: $detailCameraState,
                        shouldAnimateCamera: $detailShouldAnimate,
                        isInteractionEnabled: false
                    )
                    .frame(height: 200)
                    .cornerRadius(12)

                    if let url = drop.mediaURL {
                        Link("Open media", destination: url)
                    }

                    if drop.requiresRedemption() {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Redemption details")
                                .font(.headline)
                            if let code = drop.redemptionCode {
                                Text("Code: \(code)")
                            }
                            if let limit = drop.redemptionLimit {
                                Text("Redemptions left: \(max(limit - drop.redemptionCount, 0))")
                            }
                        }
                    }

                    Button("Report drop") { showingReport = true }
                        .buttonStyle(.bordered)
                        .padding(.top)
                }
                .padding()
            }
        }
        .sheet(isPresented: $showingReport) {
            GeoDropNavigationContainer(
                subtitle: "Report drop",
                trailing: {
                    Button("Cancel") { showingReport = false }
                        .font(.callout.weight(.semibold))
                        .foregroundColor(.accentColor)
                }
            ) {
                Form {
                    Section(header: Text("Tell us what's wrong")) {
                        TextField("Reason", text: $reportReason)
                    }
                    Button("Submit") {
                        viewModel.report(drop: drop, reasons: [reportReason])
                        reportReason = ""
                        dismiss()
                    }
                    .disabled(reportReason.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}
