package com.flipclose;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Purpose: decide which apps may be closed. */
final class Apps {
    static List<ResolveInfo> launchable(Context c) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        return c.getPackageManager().queryIntentActivities(i, 0);
    }

    /** Never closed: this app, the launcher, keyboards, . */
    static Set<String> protectedPackages(Context c) {
        Set<String> s = new HashSet<>();
        s.add(c.getPackageName());
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo r : c.getPackageManager().queryIntentActivities(home, 0)) {
            s.add(r.activityInfo.packageName);
        }
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            for (InputMethodInfo m : imm.getEnabledInputMethodList()) s.add(m.getPackageName());
        }
        return s;
    }

    static List<String> targets(Context c) {
        Set<String> skip = protectedPackages(c);
        List<String> out = new ArrayList<>();
        for (ResolveInfo r : launchable(c)) {
            String p = r.activityInfo.packageName;
            if (!skip.contains(p) && !out.contains(p)) out.add(p);
        }
        return out;
    }
}
