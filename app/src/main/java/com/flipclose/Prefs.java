package com.flipclose;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Purpose: every saved setting in one place. */
final class Prefs {
    static final int SOFT = 0, ACCESSIBILITY = 1, OWNER = 2;   // how apps are closed
    static final int ALL = 0, CURRENT = 1;                      // which apps are closed

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("flipclose", Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c) { return p(c).getBoolean("enabled", false); }
    static void setEnabled(Context c, boolean v) { p(c).edit().putBoolean("enabled", v).apply(); }
    static int mode(Context c) { return p(c).getInt("mode", SOFT); }
    static void setMode(Context c, int v) { p(c).edit().putInt("mode", v).apply(); }
    /** Not chosen yet: device owner defaults to the app in front, the other modes to all apps. */
    static int scope(Context c) {
        int s = p(c).getInt("scope", -1);
        return s >= 0 ? s : (mode(c) == OWNER ? CURRENT : ALL);
    }
    static void setScope(Context c, int v) { p(c).edit().putInt("scope", v).apply(); }
    static int countdown(Context c) { return p(c).getInt("count", 5); }
    static void setCountdown(Context c, int v) { p(c).edit().putInt("count", Math.max(0, v)).apply(); }
    static String label(Context c) { return p(c).getString("label", "Clear all"); }
    static void setLabel(Context c, String v) { p(c).edit().putString("label", v).apply(); }
    static String last(Context c) { return p(c).getString("last", "nothing yet"); }
    static void setLast(Context c, String v) { p(c).edit().putString("last", v).apply(); }
    static Set<String> keep(Context c) {
        return new HashSet<>(p(c).getStringSet("keep", new HashSet<String>()));
    }
    static void setKeep(Context c, Set<String> v) { p(c).edit().putStringSet("keep", v).apply(); }
}
