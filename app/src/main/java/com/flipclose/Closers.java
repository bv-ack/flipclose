package com.flipclose;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Purpose: the three ways to close apps. Each returns a short result text. */
final class Closers {
    static String run(Context c) {
        switch (Prefs.mode(c)) {
            case Prefs.ACCESSIBILITY: return accessibility(c);
            case Prefs.OWNER: return owner(c);
            default: return soft(c);
        }
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

    /** Undo: lifts any suspension this app may have left behind (device owner only). */
    static String restore(Context c) {
        DevicePolicyManager d = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!d.isDeviceOwnerApp(c.getPackageName())) return "Not device owner, nothing to restore";
        List<String> t = Apps.targets(c);
        d.setPackagesSuspended(new ComponentName(c, AdminReceiver.class), t.toArray(new String[0]), false);
        return "Lifted suspension on " + t.size() + " apps";
    }

    /** Needs device owner: suspends every closable app, then lifts the suspension. */
    static String owner(Context c) {
        final DevicePolicyManager d = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final ComponentName admin = new ComponentName(c, AdminReceiver.class);
        if (!d.isDeviceOwnerApp(c.getPackageName())) return "Not device owner";
        List<String> t = Apps.targets(c);
        String[] failed = d.setPackagesSuspended(admin, t.toArray(new String[0]), true);
        final List<String> ok = new ArrayList<>(t);
        ok.removeAll(Arrays.asList(failed));
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                d.setPackagesSuspended(admin, ok.toArray(new String[0]), false);
            }
        }, 500);
        return "Closed " + ok.size() + " apps";
    }
}
