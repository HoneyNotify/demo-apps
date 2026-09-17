import HoneyNotify
import UIKit
import UserNotifications

extension Notification.Name {
    static let openHoneyNotifyURL = Notification.Name("OpenHoneyNotifyURL")
}

@MainActor
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    let honeyNotify = HoneyNotify(
        baseURL: AppConfig.honeyNotifyAPIURL,
        clientKey: AppConfig.honeyNotifyClientKey
    )

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self

        guard AppConfig.isHoneyNotifyConfigured else {
            print("HoneyNotify: set HONEYNOTIFY_CLIENT_KEY in Config.xcconfig")
            return true
        }

        Task {
            do {
                let granted = try await honeyNotify.requestPermissionAndRegister(
                    includeCriticalAlerts: AppConfig.requestsCriticalAlerts
                )
                print("HoneyNotify: notification permission granted = \(granted)")
            } catch {
                print("HoneyNotify: permission request failed: \(error)")
            }
        }

        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Task {
            do {
                let deviceID = try await honeyNotify.refresh(deviceToken: deviceToken)
                print("HoneyNotify: registered device \(deviceID)")
            } catch {
                print("HoneyNotify: registration failed: \(error)")
            }
        }
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("HoneyNotify: APNs registration failed: \(error)")
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        Task { @MainActor in
            try? await honeyNotify.trackReceived(userInfo: userInfo)
        }
        completionHandler([.banner, .sound, .badge])
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        completionHandler()
        Task { @MainActor in
            try? await honeyNotify.trackOpened(
                userInfo: userInfo,
                actionId: response.actionIdentifier == UNNotificationDefaultActionIdentifier
                    ? nil
                    : response.actionIdentifier
            )

            if let url = honeyNotify.notification(from: userInfo).clickURL {
                NotificationCenter.default.post(name: .openHoneyNotifyURL, object: url)
            }
        }
    }
}
