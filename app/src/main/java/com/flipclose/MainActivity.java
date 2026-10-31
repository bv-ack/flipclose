package com.flipclose;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/** Purpose: the one settings screen. */
public class MainActivity extends Activity {
    private TextView status;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);

        Switch run = new Switch(this);
        run.setText("Listen for flip");
        run.setChecked(Prefs.enabled(this));
        run.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) {
                Prefs.setEnabled(MainActivity.this, on);
                Intent i = new Intent(MainActivity.this, TriggerService.class);
                if (on) startForegroundService(i); else stopService(i);
            }
        });
        root.addView(run);

        Switch dry = new Switch(this);
        dry.setText("Dry run (detect only, close nothing)");
        dry.setChecked(Prefs.dryRun(this));
        dry.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) {
                Prefs.setDryRun(MainActivity.this, on);
            }
        });
        root.addView(dry);

        RadioGroup g = new RadioGroup(this);
        String[] names = {"Soft: stop background apps", "Accessibility: clear recents", "Device owner: suspend all apps"};
        for (int i = 0; i < names.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setText(names[i]);
            r.setId(1000 + i);
            g.addView(r);
        }
        g.check(1000 + Prefs.mode(this));
        g.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup grp, int id) {
                Prefs.setMode(MainActivity.this, id - 1000);
            }
        });
        root.addView(g);

        final EditText sec = new EditText(this);
        sec.setHint("Countdown seconds");
        sec.setInputType(InputType.TYPE_CLASS_NUMBER);
        sec.setText(String.valueOf(Prefs.countdown(this)));
        root.addView(sec);

        final EditText label = new EditText(this);
        label.setHint("Recents button text (Accessibility mode)");
        label.setText(Prefs.label(this));
        root.addView(label);

        root.addView(button("Save", new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { Prefs.setCountdown(MainActivity.this, Integer.parseInt(sec.getText().toString().trim())); }
                catch (NumberFormatException e) { /* keep old value */ }
                Prefs.setLabel(MainActivity.this, label.getText().toString().trim());
            }
        }));
        root.addView(button("Never-close apps", new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, NeverCloseActivity.class)); }
        }));
        root.addView(button("Accessibility settings", new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        }));
        root.addView(button("App info (for \"Allow restricted settings\")", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
        }));

        status = new TextView(this);
        status.setPadding(0, 30, 0, 0);
        root.addView(status);
    }

    @Override protected void onResume() {
        super.onResume();
        DevicePolicyManager d = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        status.setText("Device owner: " + (d.isDeviceOwnerApp(getPackageName()) ? "yes" : "no")
                + "\nAccessibility service: " + (CloserAccessibilityService.instance != null ? "running" : "off"));
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }
}
