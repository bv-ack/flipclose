# FlipClose guide

Everything below can be done from a phone browser. No PC is needed.

## 1. Set up the repository

1. Create a GitHub account and a new repository (public or private).
2. Add the build workflow: **Add file > Create new file**, type `.github/workflows/zip-release.yml` in the name box (the slashes create the folders), paste the contents of `zip-release.yml`, and commit.
3. Optional: add `.github/workflows/build-variant.yml` the same way. It builds a second copy under another package name (see section 6).

## 2. Build and install

1. **Add file > Upload files** and upload `FlipClose.zip` to the repository root.
2. Open the **Actions** tab and wait for the run to finish. It unpacks the zip, deletes it, builds the app and creates a release.
3. Open **Releases**, download the APK, and install it. Android asks you to allow installs from the browser the first time.

Every new zip you upload wipes the repository root (except `.github` and zips), unpacks the new project and builds again. Keep other projects and the GitHub Pages site in their own repositories.

## 3. First run

Open FlipClose and check the status lines at the top of the screen.

- **Notifications:** allow them, so the countdown can show.
- **Allow running in background:** keeps the service from being killed.
- **Usage access:** needed for "only the app in front".
- **Accessibility:** only for Accessibility mode. If the toggle is greyed out, open App info, the three-dot menu, "Allow restricted settings".
- **Display over other apps:** optional.

Turn on **Run service**, pick **What to close** and **How to close**, press **Save**, then **Test close in 5 s**.

## 4. PIN lock

Set a 6-digit PIN under **PIN lock**. Write down the recovery code it shows once. The screen locks again 60 seconds after you leave the app. If you lose both, the locked screen has an exit that works after one hour.

## 5. Device owner (optional)

Needed for suspending apps, downtime, blocked apps, DNS, date/time lock, app protection and Chrome rules.

1. Remove all accounts from the device. Make sure no other app is device owner.
2. Start wireless debugging (for example through Shizuku) or connect ADB and run:
   `dpm set-device-owner com.flipclose/.AdminReceiver`
3. Reopen FlipClose: the status line should say "Device owner: yes".
4. **Test the exit now:** Device owner tools > Release device owner. It should say "Released". Then activate again.

Leaving device owner at any time: Device owner tools > Release device owner. It lifts every suspension and lock first.

## 6. A second copy under another package name

Run the **build-variant** workflow from the Actions tab (Run workflow). Choose a suffix, for example `two`, and an app name. The release it creates installs next to the original with empty settings. To make that copy device owner, use the full class name: `dpm set-device-owner com.flipclose.two/com.flipclose.AdminReceiver`.

Without signing secrets every build uses a new debug key, so Android may refuse to update an installed copy in place. Use a new suffix, or uninstall first. For updates in place, add the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD` and `KEY_ALIAS`.

## 7. Safe Browser

The second icon, "Safe Browser", opens only YouTube and WhatsApp Web.

- **YouTube:** add channels in FlipClose (Safe browser > Allowed YouTube channels). Handles like `@name` or channel links work. Untick one to remove it.
- **WhatsApp Web:** Status and Channels are hidden. No attachments or notifications.
- **Images:** a switch in FlipClose blocks images in the browser; it applies the next time you pick a site.

## 8. Troubleshooting

- **"Can't tell which app is open":** allow Usage access.
- **"Can't leave the app":** enable the accessibility service or the overlay permission.
- **"Not device owner":** activation did not work. Check for accounts or another device owner.
- **Chrome shows "unknown policy":** Chrome on Android does not support that policy; site blocking only.
- **Update fails with a conflict:** different signing key, see section 6.
