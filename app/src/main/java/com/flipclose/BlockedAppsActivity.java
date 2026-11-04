package com.flipclose;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Purpose: pick the apps that are always suspended ("blocked"), or suspended only during downtime. Can list every package. */
public class BlockedAppsActivity extends Activity {
    static final String WHICH = "which";

    private boolean downtime;
    private Set<String> chosen;
    private String[] pkgs = new String[0];
    private ListView lv;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        downtime = "downtime".equals(getIntent().getStringExtra(WHICH));
        setTitle(downtime ? "Downtime apps" : "Blocked apps");
        chosen = downtime ? Prefs.dtApps(this) : Prefs.blocked(this);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        Switch all = new Switch(this);
        all.setText("Show every package (including background components)");
        all.setPadding(30, 20, 30, 20);
        lv = new ListView(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        box.addView(all);
        box.addView(lv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(box);

        all.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) { fill(on); }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (lv.isItemChecked(pos)) chosen.add(pkgs[pos]); else chosen.remove(pkgs[pos]);
                if (downtime) Prefs.setDtApps(BlockedAppsActivity.this, chosen);
                else Prefs.setBlocked(BlockedAppsActivity.this, chosen);
            }
        });
        fill(false);
    }

    private void fill(boolean everything) {
        PackageManager pm = getPackageManager();
        Set<String> hard = Apps.hardProtected(this);
        Set<String> launch = new HashSet<>();
        for (ResolveInfo r : Apps.launchable(this)) launch.add(r.activityInfo.packageName);

        List<String[]> rows = new ArrayList<>();
        for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
            if (hard.contains(ai.packageName)) continue;
            boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (!everything && system && !launch.contains(ai.packageName)) continue;
            rows.add(new String[]{ai.loadLabel(pm).toString(), ai.packageName, system ? " [system]" : ""});
        }
        Collections.sort(rows, new Comparator<String[]>() {
            @Override public int compare(String[] a, String[] b) { return a[0].compareToIgnoreCase(b[0]); }
        });

        pkgs = new String[rows.size()];
        String[] text = new String[rows.size()];
        for (int i = 0; i < pkgs.length; i++) {
            pkgs[i] = rows.get(i)[1];
            text[i] = rows.get(i)[0] + rows.get(i)[2] + "\n" + pkgs[i];
        }
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, text));
        for (int i = 0; i < pkgs.length; i++) lv.setItemChecked(i, chosen.contains(pkgs[i]));
    }
}
