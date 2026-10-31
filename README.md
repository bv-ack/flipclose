# FlipClose

Flip the device face-down and hold ~1 s: a countdown starts (notification with Cancel; flipping back also cancels), then apps are closed.

Modes (pick in the app):
- Soft: asks Android to stop background apps. No setup. The app on screen stays.
- Accessibility: opens recents and presses the clear-all button. Enable the service in Accessibility settings. If the toggle is greyed out (sideloaded app, Android 13+): App info, three-dot menu, "Allow restricted settings". Set the button text for your device in the app.
- Device owner: suspends then unsuspends all closable apps. One-time setup, no accounts or extra users on the device, and no other device owner (e.g. TestDPC):
  `adb shell dpm set-device-owner com.flipclose/.AdminReceiver`
  (also possible via root or Shizuku). If an app ever stays suspended, run the same mode again or reboot.

First run is a dry run (detects the flip, closes nothing). Turn it off in the app when the detection feels right.

Optional release signing: add repo secrets KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS. Without them the release APK uses a debug key.

Optional: allow "Display over other apps" (button in the app). Then the countdown covers the whole screen with a big Cancel button, so taps cannot reach the app underneath. Without it, a notification with a Cancel button is used. Flipping back cancels in both cases.
