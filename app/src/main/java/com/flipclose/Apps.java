package com.flipclose;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Process;
import android.telecom.TelecomManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Purpose: decide which apps may be closed, and which app is in front. */
final class Apps {
    /** Never closed or suspended, whatever the lists say. */
    private static final String[] ALWAYS = {
        "android", "com.android.settings", "com.android.systemui", "com.android.phone",
        "com.android.server.telecom", "com.android.emergency",
        "com.android.packageinstaller", "com.google.android.packageinstaller",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller",
        "com.google.android.gms", "com.google.android.gsf",
        "com.android.webview", "com.google.android.webview",
        "moe.shizuku.privileged.api", "com.rosan.dhizuku"
    };

    static List<ResolveInfo> launchable(Context c) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        return c.getPackageManager().queryIntentActivities(i, 0);
    }

    /** The fixed list above, this app, launchers, keyboards, the dialer, and the user's never-close list. */
    static Set<String> protectedPackages(Context c) {
        Set<String> s = hardProtected(c);
        s.addAll(Prefs.keep(c));
        return s;
    }

    /** Fixed list, this app, launchers, keyboards and the dialer: never suspended by any rule. */
    static Set<String> hardProtected(Context c) {
        Set<String> s = new HashSet<>(Arrays.asList(ALWAYS));
        s.add(c.getPackageName());
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo r : c.getPackageManager().queryIntentActivities(home, 0)) {
            s.add(r.activityInfo.packageName);
        }
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            for (InputMethodInfo m : imm.getEnabledInputMethodList()) s.add(m.getPackageName());
        }
        try {
            TelecomManager tm = (TelecomManager) c.getSystemService(Context.TELECOM_SERVICE);
            String dialer = tm == null ? null : tm.getDefaultDialerPackage();
            if (dialer != null) s.add(dialer);
        } catch (Exception e) { /* keep going without it */ }
        return s;
    }

    /** Built-in system apps that were never updated; "close all" leaves these alone. */
    private static boolean pureSystem(ApplicationInfo ai) {
        return (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                && (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
    }

    /** Apps to close when the scope is "all". */
    static List<String> targets(Context c) {
        Set<String> skip = protectedPackages(c);
        skip.addAll(Prefs.applied(c)); // already suspended by a rule: do not lift it afterwards
        List<String> out = new ArrayList<>();
        for (ResolveInfo r : launchable(c)) {
            String p = r.activityInfo.packageName;
            if (!skip.contains(p) && !pureSystem(r.activityInfo.applicationInfo) && !out.contains(p)) out.add(p);
        }
        return out;
    }

    static boolean usageAllowed(Context c) {
        AppOpsManager a = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        return a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName())
                == AppOpsManager.MODE_ALLOWED;
    }

    /** The app most recently brought to the front, or null if unknown. Needs Usage access. */
    static String foreground(Context c) {
        UsageStatsManager u = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        UsageEvents ev = u.queryEvents(now - 12L * 3600L * 1000L, now);
        UsageEvents.Event e = new UsageEvents.Event();
        String pkg = null;
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e);
            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) pkg = e.getPackageName();
        }
        return pkg;
    }
}
