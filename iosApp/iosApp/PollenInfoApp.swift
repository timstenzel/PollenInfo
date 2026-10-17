import ComposeApp
import SwiftUI

@main
struct PollenInfoApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                // Compose draws edge to edge and pads each screen by the safe area and keyboard
                // insets itself, as on Android.
                .ignoresSafeArea()
                // The launch screen's colour, so nothing lighter shows before Compose's first frame.
                .background(Color("LaunchBackground"))
        }
    }
}

/// The shared Compose app, created once with this build configuration's backend.
private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(apiBaseUrl: Bundle.main.apiBaseUrl)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private extension Bundle {
    /// `API_BASE_URL` from `Info.plist`, set per configuration in `Config/*.xcconfig`.
    var apiBaseUrl: String {
        guard let url = object(forInfoDictionaryKey: "API_BASE_URL") as? String, !url.isEmpty else {
            fatalError("API_BASE_URL is missing from Info.plist")
        }
        return url
    }
}
