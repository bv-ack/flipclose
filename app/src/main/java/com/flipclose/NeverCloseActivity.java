package com.flipclose;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Purpose: pick the apps that must never be closed. */
public class NeverCloseActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = Apps.launchable(this);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));
        final String[] pkgs = new String[apps.size()];
        String[] names = new String[apps.size()];
        final Set<String> keep = Prefs.keep(this);
        final ListView lv = new ListView(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for (int i = 0; i < pkgs.length; i++) {
            pkgs[i] = apps.get(i).activityInfo.packageName;
            names[i] = apps.get(i).loadLabel(pm).toString();
        }
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, names));
        for (int i = 0; i < pkgs.length; i++) lv.setItemChecked(i, keep.contains(pkgs[i]));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (lv.isItemChecked(pos)) keep.add(pkgs[pos]); else keep.remove(pkgs[pos]);
                Prefs.setKeep(NeverCloseActivity.this, keep);
            }
        });
        setContentView(lv);
    }
}
