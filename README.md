# FlipClose

Turn the device face-down and keep it there for about a second: a countdown starts (notification with Cancel, or a cover screen if the overlay permission is allowed). Turning it back over cancels. Countdown 0 closes right away.

What to close: all apps, or only the app in front (needs Usage access).
How to close:
- Soft: asks Android to stop background apps. No setup. With "only the app in front" it first leaves the app (accessibility service, or the overlay permission), then stops it.
- Accessibility: opens recents and presses the clear-all button. Enable the service in Accessibility settings. If the toggle is greyed out (sideloaded app, Android 13+): App info, three-dot menu, "Allow restricted settings". Set the button text for your device in the app. Apps your device keeps alive on purpose may survive.
- Device owner: suspends then lifts the suspension, which stops the apps. One-time setup, no accounts or extra users on the device, and no other device owner (e.g. TestDPC):
  `adb shell dpm set-device-owner com.flipclose/.AdminReceiver`
  (also possible through Shizuku wireless debugging or root). "Restore apps" lifts any suspension left behind.

Never closed: Settings, System UI, phone/dialer, installer, Play services, WebView, Shizuku/Dhizuku, this app, your launcher, keyboards, anything in the never-close list. "Close all" also skips built-in system apps that were never updated.

Device owner tools: lock Private DNS to Cloudflare's family filter (family.cloudflare-dns.com) or return it to automatic.

Allow "Running in background" in the app so the service is not killed.

Optional release signing: add repo secrets KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS. Without them the release APK uses a debug key.

PIN lock: optional 6-digit PIN for the settings screen (stored only as a salted hash; wrong tries add growing delays). Setting it shows a one-time recovery code that removes the PIN: write it down or give it to someone you trust. The screen locks again 60 s after you leave the app. The PIN protects this screen only; it does not yet stop uninstalling or clearing the app's data (device owner can block those later).

Accessibility mode always clears all apps (it cannot pick one). Soft mode only stops the app: its card can stay in recents.
