# HoneyNotify WebView demo apps

We here have provided two basic starter apps:

- [`iOS/`](iOS/) — SwiftUI, `WKWebView`, APNs, and the HoneyNotify Swift SDK.
- [`Android/`](Android/) — Kotlin, Android `WebView`, Firebase Cloud Messaging (FCM), and the HoneyNotify Android SDK.

Both projects load your website full screen and register the physical device with HoneyNotify. Notification opens are tracked and a HoneyNotify `click_url` is opened inside the app when it belongs to the configured website. Other HTTP(S) links are handed to the system browser.

> These projects contain a **public mobile client key**. Use only a restricted key beginning `ps_public_`. Never put a server/send key, APNs `.p8` private key, or Firebase service-account JSON in either app.

## Before you start

You need:

- A HoneyNotify app and its `ps_public_...` mobile client key.
- A website served over HTTPS. Its pages must be suitable for a mobile WebView.
- For iOS: a Mac with the current stable Xcode, an Apple Developer account, an APNs key configured in HoneyNotify, and a physical iPhone or iPad for push testing.
- For Android: the current stable Android Studio, its bundled Java 17 runtime, a Firebase project, and FCM credentials configured in HoneyNotify.

The simulators/emulators can be used to test the WebView. Use a physical Apple device for a representative APNs test. Android FCM works on a device or an emulator image that includes Google Play services.

## iOS setup

1. Open `iOS/HoneyNotifyWebViewDemo.xcodeproj` in Xcode.
2. Edit `iOS/Config.xcconfig`:
   - Set `APP_DISPLAY_NAME`.
   - Set `PRODUCT_BUNDLE_IDENTIFIER` to an identifier owned by your Apple Developer team.
   - Set `WEBVIEW_URL` to your HTTPS website URL. Keep `$()` between the two slashes as shown; this prevents `.xcconfig` from treating `//` as a comment.
   - Set `HONEYNOTIFY_API_URL` and `HONEYNOTIFY_CLIENT_KEY`.
3. Select the app target, open **Signing & Capabilities**, select your team, and confirm **Push Notifications** is present.
4. In the Apple Developer portal, create or select the matching App ID and enable Push Notifications.
5. Create an APNs signing key. Add its key ID, team ID, `.p8` private key, bundle ID, and environment to the iOS provider settings for this HoneyNotify app. Do not add the `.p8` file to this project.
6. Connect a physical device, choose it as the run destination, and press **Run**.
7. Accept the notification permission prompt. Check the Xcode console for a successful HoneyNotify device ID.

The Xcode project already declares `https://github.com/HoneyNotify/ios-SDK.git` as a Swift Package dependency tracking the latest `version`. Xcode keeps a managed checkout in its package cache. When opening the project for the first time, allow package resolution to finish. To force an immediate refresh later, use **File > Packages > Update to Latest Package Versions**.

The included entitlement uses the development APNs environment. Xcode changes the signed entitlement appropriately for distribution profiles. Ensure the HoneyNotify provider environment matches the build you are testing.

The demo deliberately leaves `AppConfig.requestsCriticalAlerts` set to `false` and does not include Apple's restricted Critical Alerts entitlement. Only after Apple approves the entitlement for your own App ID should you add it to the target and change that flag to `true`.

## Android setup

1. Open the `Android/` directory in Android Studio.
2. Edit `Android/demo.properties` and set the application name, HTTPS website URL, API URL, HoneyNotify public key, and a unique application ID.
3. In the Firebase console, add an Android app with exactly the same application ID.
4. Download Firebase's `google-services.json` and place it at `Android/app/google-services.json`. This file is intentionally ignored by Git.
5. In Firebase, enable Cloud Messaging. In HoneyNotify, configure the matching Firebase project ID and service-account JSON as the app's FCM provider credentials. Keep the service-account JSON on the HoneyNotify server; do not put it in this demo.
6. Let Android Studio complete Gradle sync, then run the `app` configuration on a device or Google Play-enabled emulator.
7. On Android 13 or later, accept the notification permission prompt. Check Logcat for a successful HoneyNotify device ID.

The Android project declares `https://github.com/HoneyNotify/android-SDK.git` as a Gradle Git source dependency tracking the latest `version`. Android Studio checks out the SDK into Gradle's managed VCS cache during sync/build. Dynamic dependency caching is disabled for this demo so Gradle checks the branch for updates; **Sync Project with Gradle Files** forces a new resolution when required.

The demo creates HoneyNotify's four notification channels during foreground startup. Android users retain final control of each channel's sound and importance, so the Critical option is an urgent high-importance channel rather than a guaranteed Do Not Disturb bypass.

Tracking `main` gives the demo the newest SDK code but also means an upstream change can affect an otherwise unchanged app. For a production release, consider changing each dependency to a tested release tag or commit before submitting to an app store.

## Sending a test notification

After a device has registered:

1. Find the device in the HoneyNotify dashboard and copy its HoneyNotify device ID.
2. Send a notification from the dashboard or your trusted server. Target only that test device for the first test.
3. Add an HTTPS `click_url` on your website to verify in-app navigation. A URL on another host should open in the system browser.
4. Test foreground, background, and terminated app states.
5. Confirm received/opened events in HoneyNotify. Delivery callbacks vary by operating-system state, so an OS-displayed background notification may not produce a client-side `received` event until the app handles it.

Notification sending is intentionally absent from these apps. Sends belong on a trusted server using a server API key and an `Idempotency-Key`; mobile apps should only contain the restricted public client key.

## Website and navigation behaviour

- JavaScript and DOM storage are enabled for normal web applications.
- File and content URL access are disabled on Android.
- Links for the configured website host stay in the WebView.
- Other `http` and `https` links open in the user's browser.
- Non-web schemes such as `mailto:` and `tel:` are handed to an installed system app when available.
- Android's back button moves through WebView history before exiting the app.
- Pull down on iOS to refresh. Android shows a retry page after a main-frame load error.

If the site uses login, keep authentication in the website. When you want notifications associated with the signed-in customer, have the website/native bridge obtain a short-lived identity token from your authenticated backend and call the SDK's `identify` method. Do not trust an arbitrary user ID supplied by page JavaScript.

## Troubleshooting

### The website does not load

- Confirm the URL is valid HTTPS and publicly reachable from the device.
- Check App Transport Security or cleartext-network errors in Xcode/Logcat. The demos deliberately do not allow insecure HTTP.
- Check the website's Content Security Policy and authentication/cookie behavior.

### The device does not appear in HoneyNotify

- Confirm the key starts with `ps_public_` and belongs to the same HoneyNotify app as the provider credentials.
- Confirm the HoneyNotify API URL is reachable from the device.
- On iOS, verify the bundle ID, signing team, push entitlement, APNs environment, and APNs provider credentials all match.
- On Android, verify `google-services.json` is in `app/`, its package name matches `application_id`, and the emulator/device has Google Play services.
- Remove and reinstall the app to repeat the initial permission flow. Operating systems may otherwise preserve a denial.

### Notifications arrive but taps do not open the expected page

- Send a full absolute HTTPS `click_url`.
- Confirm its host matches `webview_url` if it should remain inside the app.
- Inspect Xcode or Logcat for the received payload fields.

## Production checklist

Before publishing, replace the sample app icons and launch presentation, review accessibility and privacy copy, add your privacy manifest/policy declarations, test offline and authentication behavior, configure universal/app links if needed, and complete App Store/Play Store signing. Also validate notification permissions, token rotation, account switching, logout, and deep links on supported OS versions.
