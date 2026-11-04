package com.flipclose;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Purpose: the ways to close apps, plus a few device-owner helpers. Each returns a short result text. */
final class Closers {
    static String run(Context c) {
        if (Prefs.mode(c) == Prefs.ACCESSIBILITY) return accessibility(c); // it cannot pick one app
        if (Prefs.scope(c) == Prefs.CURRENT) return current(c);
        return Prefs.mode(c) == Prefs.OWNER ? suspendBriefly(c, Apps.targets(c)) : soft(c);
    }

    /** Needs nothing: asks Android to stop background processes. Leaves the on-screen app alone. */
    static String soft(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        List<String> t = Apps.targets(c);
        for (String p : t) am.killBackgroundProcesses(p);
        return "Asked Android to stop " + t.size() + " apps";
    }

    /** Needs the accessibility service enabled: opens recents and presses "Clear all". */
    static String accessibility(Context c) {
        CloserAccessibilityService s = CloserAccessibilityService.instance;
        if (s == null) return "Accessibility service is not running";
        s.clearRecents(Prefs.label(c));
        return "Clearing recents";
    }

    /** Closes only the app in front: device owner suspends it; otherwise leave it, then stop it. */
    static String current(final Context c) {
        final String pkg = Apps.foreground(c);
        if (pkg == null) return "Can't tell which app is open: allow Usage access";
        if (Apps.protectedPackages(c).contains(pkg)) return "The open app is protected: " + pkg;
        if (Prefs.mode(c) == Prefs.OWNER) return suspendBriefly(c, Collections.singletonList(pkg));
        if (!goHome(c)) return "Can't leave the app: enable the accessibility service or the overlay permission";
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).killBackgroundProcesses(pkg);
            }
        }, 700);
        return "Closing " + pkg;
    }

    private static boolean goHome(Context c) {
        CloserAccessibilityService s = CloserAccessibilityService.instance;
        if (s != null) return s.home();
        if (Settings.canDrawOverlays(c)) {
            c.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }
        return false;
    }

    /** Device owner: suspend the apps, then lift the suspension, which stops them. */
    private static String suspendBriefly(Context c, List<String> pkgs) {
        final DevicePolicyManager d = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final ComponentName admin = new ComponentName(c, AdminReceiver.class);
        if (!d.isDeviceOwnerApp(c.getPackageName())) return "Not device owner";
        String[] failed = d.setPackagesSuspended(admin, pkgs.toArray(new String[0]), true);
        final List<String> ok = new ArrayList<>(pkgs);
        ok.removeAll(Arrays.asList(failed));
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                d.setPackagesSuspended(admin, ok.toArray(new String[0]), false);
            }
        }, 500);
        return "Closed " + ok.size() + " apps";
    }

    /** Undo: lifts any suspension this app may have left behind (device owner only). */
    static String restore(Context c) {
        DevicePolicyManager d = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!d.isDeviceOwnerApp(c.getPackageName())) return "Not device owner, nothing to restore";
        List<String> t = Apps.targets(c);
        d.setPackagesSuspended(new ComponentName(c, AdminReceiver.class), t.toArray(new String[0]), false);
        return "Lifted suspension on " + t.size() + " apps";
    }

    /** Device owner: lock Private DNS to Cloudflare's family filter, or give it back to automatic. */
    static String dns(Context c, boolean family) {
        if (Build.VERSION.SDK_INT < 29) return "Needs Android 10 or newer";
        DevicePolicyManager d = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(c, AdminReceiver.class);
        if (!d.isDeviceOwnerApp(c.getPackageName())) return "Not device owner";
        if (!family) {
            d.setGlobalPrivateDnsModeOpportunistic(admin);
            return "Private DNS set to automatic";
        }
        int r = d.setGlobalPrivateDnsModeSpecifiedHost(admin, "family.cloudflare-dns.com");
        return r == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR
                ? "Private DNS locked to Cloudflare family"
                : "DNS not set (error " + r + "); check the connection";
    }
}
