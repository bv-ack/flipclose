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

Blocked apps and downtime (device owner): "Blocked apps" are kept suspended all the time; "Downtime apps" are suspended between the start and end times (a window can run past midnight). The pickers can list every installed package, including background components. Rules are applied when the service starts, every 30 s, and whenever you open the app, so the service must be running for downtime to start and end on time. Fixed protected apps (Settings, System UI, dialer, installer, Play services, launcher, keyboards, this app...) are never suspended.
"Protect this app" blocks uninstalling it and clearing its data or force-stopping it from Settings. "Lock date and time" stops the clock being changed by hand. Both can be undone from the same screen, behind the PIN. Keep the recovery code safe.

Leaving device owner: "Release device owner" (Device owner tools) first lifts every suspension, the DNS and date/time locks and the app protection, then removes this app as device owner, so it can be uninstalled normally. Try it right after you make the app device owner, before relying on any rule. If the PIN and the recovery code are both lost, the locked screen has "Lost the PIN and the code? Leave device owner": press it, wait 1 hour, press it again (the right PIN cancels it). A factory reset is never blocked by this app.

Safe Browser (second icon, "Safe Browser"): a small WebView browser for YouTube and WhatsApp Web only; every other site is refused, and app links are blocked.
- YouTube: only channels on your list (settings: "Allowed YouTube channels", add @handles or links; untick to remove). Home, search, feeds and other channels are refused. A video is checked with YouTube's public oEmbed address; if it can't be checked, it is blocked. Pages are checked every 0.6 s because YouTube changes pages without a real page load.
- WhatsApp Web: opened as a desktop browser; the Status and Channels buttons are hidden by matching their labels and icons. If WhatsApp renames them, they simply show again. No attachments, no notifications.
- "Block images in the browser" applies the next time you pick a site.
The settings live in FlipClose (behind the PIN), not in the browser.

Chrome rules (device owner): sends managed configuration to Chrome (blocked sites; images off if Chrome accepts it). Restart Chrome, open chrome://policy and tap Reload policies to see what it accepted. "Clear Chrome rules" removes them (also done by Release).
