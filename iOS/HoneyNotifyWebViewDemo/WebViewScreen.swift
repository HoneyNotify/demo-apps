import SwiftUI
import WebKit

struct WebViewScreen: UIViewRepresentable {
    let url: URL

    func makeCoordinator() -> Coordinator {
        Coordinator(websiteURL: url)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.contentInsetAdjustmentBehavior = .never

        let refreshControl = UIRefreshControl()
        refreshControl.addTarget(
            context.coordinator,
            action: #selector(Coordinator.refresh(_:)),
            for: .valueChanged
        )
        webView.scrollView.refreshControl = refreshControl
        context.coordinator.attach(to: webView)
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        coordinator.detach()
    }

    @MainActor
    final class Coordinator: NSObject, WKNavigationDelegate {
        private let websiteURL: URL
        private weak var webView: WKWebView?
        private var notificationObserver: NSObjectProtocol?

        init(websiteURL: URL) {
            self.websiteURL = websiteURL
        }

        func attach(to webView: WKWebView) {
            self.webView = webView
            notificationObserver = NotificationCenter.default.addObserver(
                forName: .openHoneyNotifyURL,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                guard let url = notification.object as? URL else {
                    return
                }
                Task { @MainActor in
                    self?.open(url)
                }
            }
        }

        func detach() {
            if let notificationObserver {
                NotificationCenter.default.removeObserver(notificationObserver)
            }
        }

        @objc func refresh(_ sender: UIRefreshControl) {
            webView?.reload()
            sender.endRefreshing()
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            guard let destination = navigationAction.request.url else {
                decisionHandler(.cancel)
                return
            }

            if navigationAction.targetFrame == nil,
               isWebsiteURL(destination) {
                webView.load(navigationAction.request)
                decisionHandler(.cancel)
                return
            }

            guard isWebsiteURL(destination) else {
                UIApplication.shared.open(destination)
                decisionHandler(.cancel)
                return
            }

            decisionHandler(.allow)
        }

        func webView(
            _ webView: WKWebView,
            didFailProvisionalNavigation navigation: WKNavigation?,
            withError error: Error
        ) {
            showErrorPage(in: webView)
        }

        private func open(_ url: URL) {
            if isWebsiteURL(url) {
                webView?.load(URLRequest(url: url))
            } else {
                UIApplication.shared.open(url)
            }
        }

        private func isWebsiteURL(_ url: URL) -> Bool {
            guard let scheme = url.scheme?.lowercased() else {
                return false
            }

            if scheme == "about" || scheme == "javascript" {
                return true
            }

            return scheme == "https"
                && url.host?.lowercased() == websiteURL.host?.lowercased()
        }

        private func showErrorPage(in webView: WKWebView) {
            let html = """
            <!doctype html><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>body{font:17px -apple-system;padding:48px 24px;text-align:center;color:#222}a{color:#9a6700}</style>
            <h1>Unable to load</h1><p>Check your connection and try again.</p>
            <p><a href="\(websiteURL.absoluteString)">Retry</a></p>
            """
            webView.loadHTMLString(html, baseURL: websiteURL)
        }
    }
}
