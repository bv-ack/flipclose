package com.flipclose;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/** Purpose: keep the right apps suspended: the blocked list always, the downtime list during downtime. */
final class Enforcer {
    static boolean downtimeActive(Context c) {
        if (!Prefs.dtOn(c)) return false;
        Calendar now = Calendar.getInstance();
        int m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int s = Prefs.dtStart(c), e = Prefs.dtEnd(c);
        if (s == e) return false;
        return s < e ? (m >= s && m < e) : (m >= s || m < e); // a window can run past midnight
    }

    /** Suspends what should be suspended and lifts what should not. force = redo it even if nothing changed. */
    static String apply(Context c, boolean force) {
        DevicePolicyManager d = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!d.isDeviceOwnerApp(c.getPackageName())) return "Not device owner";
        ComponentName admin = new ComponentName(c, AdminReceiver.class);

        Set<String> want = new HashSet<>(Prefs.blocked(c));
        if (downtimeActive(c)) want.addAll(Prefs.dtApps(c));
        want.removeAll(Apps.hardProtected(c));
        Set<String> have = Prefs.applied(c);
        if (!force && want.equals(have)) return "No change";

        Set<String> lift = new HashSet<>(have);
        lift.removeAll(want);
        if (!lift.isEmpty()) d.setPackagesSuspended(admin, lift.toArray(new String[0]), false);
        Set<String> failed = new HashSet<>();
        if (!want.isEmpty()) {
            failed.addAll(Arrays.asList(d.setPackagesSuspended(admin, want.toArray(new String[0]), true)));
        }
        want.removeAll(failed);
        Prefs.setApplied(c, want);
        return "Suspended " + want.size() + ", lifted " + lift.size()
                + (failed.isEmpty() ? "" : ", " + failed.size() + " could not be suspended");
    }
}
