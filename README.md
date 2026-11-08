# FlipClose

[![Latest](https://img.shields.io/github/v/release/bv-ack/flipclose?label=Latest&include_prereleases)](https://github.com/USERNAME/REPO/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/bv-ack/flipclose/total?label=Downloads)](https://github.com/USERNAME/REPO/releases)

| [Guide](docs/GUIDE.md) | [Changelog](CHANGELOG.md) |
|------------------------|---------------------------|

## Introduction

FlipClose closes apps when you turn the device face-down. It has grown into a small self-control kit: blocked apps and downtime, a PIN lock, a Safe Browser for YouTube and WhatsApp Web, and tools for a device owner.

## Features

- **Face-down trigger.** Hold the device face-down for about a second. A countdown starts (notification with Cancel, or a cover screen); turning it back over cancels.
- **Three ways to close apps.** Soft (stops background apps, no setup), Accessibility (clears recents), Device owner (suspends apps briefly). Scope: all apps, or only the app in front.
- **Never-close list** and a fixed protected list (Settings, System UI, dialer, installer, Play services, your launcher, keyboards, this app).
- **Blocked apps and downtime** (device owner). Pick any package, including background components. A downtime window can run past midnight.
- **PIN lock** for the settings screen, with a one-time recovery code and growing delays after wrong tries.
- **Safe Browser** (second icon). YouTube limited to allowed channels, WhatsApp Web with Status and Channels hidden, optional images off. Everything else is refused.
- **Device owner tools.** Cloudflare family DNS lock, date/time lock, protect this app from uninstall and clearing data, Chrome site rules.
- **A way out.** "Release device owner" undoes every rule and gives up ownership. This app never blocks a factory reset.

## Installation

Download the APK from [Releases](https://github.com/USERNAME/REPO/releases), allow installs from your browser when Android asks, and install. No PC is needed: the [guide](docs/GUIDE.md) shows how to build it on GitHub from a phone.

## Activation (device owner)

Only the device-owner features need this. Soft and Accessibility modes, the PIN and the Safe Browser work without it.

> [!IMPORTANT]
> Remove all accounts from the device first, and make sure no other app is device owner. Then, from wireless debugging (for example through Shizuku) or ADB:
> `dpm set-device-owner com.flipclose/.AdminReceiver`
> For a renamed variant, use the full class name: `dpm set-device-owner com.flipclose.two/com.flipclose.AdminReceiver`.

> [!TIP]
> Right after activating, press **Release device owner** once and confirm it says "Released", then activate again. Do this before you rely on any rule.

## Supported versions

Built for Android 8.0 and newer. Some tools need more: the DNS lock needs Android 10, app protection and the date/time lock work best on Android 11.

## Safety

- Device owner can be released from inside the app (Device owner tools, behind the PIN).
- If both the PIN and the recovery code are lost, the locked screen has an exit that works after a one-hour wait.
- Factory reset is never blocked.

## Known limits

- Chrome on Android reports the images-off policy as unknown, so image blocking lives in the Safe Browser.
- The Safe Browser is a WebView: no Google sign-in, no attachments, no notifications. YouTube and WhatsApp can change their pages; the channel check is built to block when unsure.
- Soft mode stops an app but its card can stay in recents.

## Build from source

The repository is built by GitHub Actions: upload the project zip to the repository root and the workflow unpacks it, builds it and publishes a release. See the [guide](docs/GUIDE.md).

## Credits

Written together with Claude (Anthropic).

## License

Not chosen yet. Add a `LICENSE` file before sharing the code more widely.
