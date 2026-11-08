# Changelog

Format follows [Keep a Changelog](https://keepachangelog.com). Versions follow the build number (0.N).

## [Unreleased]

### Planned
- GitHub Pages site for the guide.
- Safe Browser as its own package, with general browsing that has images off.
- Minimal keyboard with a clipboard history that survives restarts.
- Home screen (launcher) so FlipClose can replace a stock home app.
- Redesigned UI with grouped, partly hidden options.
- Hide Meta AI in the WhatsApp Web filter.

## [0.1] - 2026-11-08

### Added
- Face-down trigger with a cancelable countdown; turning the device back cancels.
- Closing modes: Soft, Accessibility, Device owner; scope "all apps" or "only the app in front".
- Never-close list, fixed protected list, foreground-app detection through Usage access.
- Blocked apps and downtime (device owner), pickers that can list every package.
- PIN lock with recovery code, growing delays, and a one-hour exit.
- Safe Browser: YouTube allowed channels (checked through YouTube's public oEmbed address), WhatsApp Web with Status and Channels hidden, images-off switch.
- Device owner tools: Cloudflare family DNS, date/time lock, app protection, Chrome site rules, Restore apps.
- Release device owner: undoes every rule, then gives up ownership.
- Workflows: zip-to-release, and build-variant for a second copy under another package name.

### Changed
- Device-owner mode closes only the app in front by default.
- Accessibility mode always clears all apps.

### Removed
- Throw detection, upside-down flip, reboot and lock actions, dry run.
- Chrome images-off policy (Chrome on Android reports it as unknown).
