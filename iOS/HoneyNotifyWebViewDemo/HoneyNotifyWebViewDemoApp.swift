import SwiftUI

@main
struct HoneyNotifyWebViewDemoApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            WebViewScreen(url: AppConfig.websiteURL)
                .ignoresSafeArea()
        }
    }
}
