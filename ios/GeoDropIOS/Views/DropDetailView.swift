import SwiftUI
import MapKit

struct DropDetailView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    let drop: Drop
    @Environment(\.dismiss) private var dismiss
    @State private var reportReason: String = ""
    @State private var showingReport = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(drop.displayTitle)
                        .font(.title)
                        .fontWeight(.bold)

                    if let description = drop.description, !description.isEmpty {
                        Text(description)
                    }

                    Map(coordinateRegion: .constant(MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: drop.latitude, longitude: drop.longitude), span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01))), interactionModes: [])
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
            .geoDropNavigationTitle(subtitle: "Drop")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: dismiss.callAsFunction)
                }
            }
            .sheet(isPresented: $showingReport) {
                NavigationView {
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
                    .geoDropNavigationTitle(subtitle: "Report drop")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showingReport = false }
                        }
                    }
                }
            }
        }
    }
}
