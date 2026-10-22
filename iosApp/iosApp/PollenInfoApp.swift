import ComposeApp
import SwiftUI

/// Hosts the shared Compose app full-screen. Compose pads itself for the safe areas and the
/// keyboard, so the host ignores them.
@main
struct PollenInfoApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeHost()
                .ignoresSafeArea()
        }
    }
}

private struct ComposeHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(apiBaseUrl: apiBaseUrl())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    /// The backend address of this build configuration, from Info.plist (`API_BASE_URL`).
    private func apiBaseUrl() -> String {
        guard let url = Bundle.main.object(forInfoDictionaryKey: "API_BASE_URL") as? String,
              !url.isEmpty
        else {
            fatalError("API_BASE_URL missing from Info.plist")
        }
        return url
    }
}
