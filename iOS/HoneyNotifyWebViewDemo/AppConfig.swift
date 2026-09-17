import Foundation

enum AppConfig {
    // Set this only after Apple approves the host app's Critical Alerts entitlement.
    static let requestsCriticalAlerts = false
    static let websiteURL = requiredURL(named: "WebViewURL")
    static let honeyNotifyAPIURL = requiredURL(named: "HoneyNotifyAPIURL")
    static let honeyNotifyClientKey = requiredString(named: "HoneyNotifyClientKey")

    static var isHoneyNotifyConfigured: Bool {
        honeyNotifyClientKey.hasPrefix("ps_public_")
            && honeyNotifyClientKey != "ps_public_replace_me"
    }

    private static func requiredURL(named key: String) -> URL {
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String,
              let url = URL(string: value),
              url.scheme?.lowercased() == "https",
              url.host != nil else {
            fatalError("Set a valid HTTPS value for \(key) in Config.xcconfig")
        }

        return url
    }

    private static func requiredString(named key: String) -> String {
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String,
              !value.isEmpty else {
            fatalError("Set \(key) in Config.xcconfig")
        }

        return value
    }
}
