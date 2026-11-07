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
    private static Set<String> set(Context c, String k) { return new HashSet<>(p(c).getStringSet(k, new HashSet<String>())); }
    private static void put(Context c, String k, Set<String> v) { p(c).edit().putStringSet(k, v).apply(); }
    static Set<String> blocked(Context c) { return set(c, "blocked"); }          // always suspended
    static void setBlocked(Context c, Set<String> v) { put(c, "blocked", v); }
    static Set<String> dtApps(Context c) { return set(c, "dtapps"); }            // suspended during downtime
    static void setDtApps(Context c, Set<String> v) { put(c, "dtapps", v); }
    static Set<String> applied(Context c) { return set(c, "applied"); }          // suspended by the rules right now
    static void setApplied(Context c, Set<String> v) { put(c, "applied", v); }
    static boolean dtOn(Context c) { return p(c).getBoolean("dton", false); }
    static void setDtOn(Context c, boolean v) { p(c).edit().putBoolean("dton", v).apply(); }
    static int dtStart(Context c) { return p(c).getInt("dts", 22 * 60); }       // minutes after midnight
    static int dtEnd(Context c) { return p(c).getInt("dte", 7 * 60); }
    static void setDtTimes(Context c, int s, int e) { p(c).edit().putInt("dts", s).putInt("dte", e).apply(); }
    static Set<String> channels(Context c) { return set(c, "ytch"); }            // YouTube channels the browser may open
    static void setChannels(Context c, Set<String> v) { put(c, "ytch", v); }
    static boolean ytOnly(Context c) { return p(c).getBoolean("ytonly", true); }
    static void setYtOnly(Context c, boolean v) { p(c).edit().putBoolean("ytonly", v).apply(); }
    static boolean waHide(Context c) { return p(c).getBoolean("wahide", true); }
    static void setWaHide(Context c, boolean v) { p(c).edit().putBoolean("wahide", v).apply(); }
    static boolean bImagesOff(Context c) { return p(c).getBoolean("bimg", false); }
    static void setBImagesOff(Context c, boolean v) { p(c).edit().putBoolean("bimg", v).apply(); }
    static boolean chromeImagesOff(Context c) { return p(c).getBoolean("chimg", false); }
    static void setChromeImagesOff(Context c, boolean v) { p(c).edit().putBoolean("chimg", v).apply(); }
    static String chromeBlock(Context c) { return p(c).getString("chblock", ""); }
    static void setChromeBlock(Context c, String v) { p(c).edit().putString("chblock", v).apply(); }
    static String last(Context c) { return p(c).getString("last", "nothing yet"); }
    static void setLast(Context c, String v) { p(c).edit().putString("last", v).apply(); }
    static Set<String> keep(Context c) {
        return new HashSet<>(p(c).getStringSet("keep", new HashSet<String>()));
    }
    static void setKeep(Context c, Set<String> v) { p(c).edit().putStringSet("keep", v).apply(); }
}
