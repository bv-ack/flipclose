package com.flipclose;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.Toast;

/** Purpose: the one settings screen, with live feedback so you can see what works. */
public class MainActivity extends Activity implements SensorEventListener {
    private static final String[] MODES = {"Soft", "Accessibility", "Device owner"};
    private TextView status;
    private SensorManager sm;
    private float z;

    private void toast(String t) { Toast.makeText(getApplicationContext(), t, Toast.LENGTH_SHORT).show(); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        sm = getSystemService(SensorManager.class);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);

        status = new TextView(this);
        root.addView(status);

        Switch run = new Switch(this);
        run.setText("Listen for flip");
        run.setChecked(Prefs.enabled(this));
        run.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) {
                Prefs.setEnabled(MainActivity.this, on);
                Intent i = new Intent(MainActivity.this, TriggerService.class);
                if (on) startForegroundService(i); else stopService(i);
                toast(on ? "Listening on" : "Listening off");
            }
        });
        root.addView(run);

        Switch dry = new Switch(this);
        dry.setText("Dry run (detect only, close nothing)");
        dry.setChecked(Prefs.dryRun(this));
        dry.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) {
                Prefs.setDryRun(MainActivity.this, on);
                toast(on ? "Dry run on: nothing will be closed" : "Dry run off: apps WILL be closed");
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
                toast("Mode saved: " + MODES[id - 1000]);
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
                toast("Saved: " + Prefs.countdown(MainActivity.this) + " s, button \"" + Prefs.label(MainActivity.this) + "\"");
            }
        }));

        root.addView(button("Test close in 5 s (selected mode)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                toast("Open some apps, then go home. Closing in 5 s");
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override public void run() {
                        String r;
                        try { r = Closers.run(MainActivity.this); } catch (Exception e) { r = "Failed: " + e.getMessage(); }
                        Prefs.setLast(MainActivity.this, "Test: " + r);
                        toast(r);
                    }
                }, 5000);
            }
        }));
        root.addView(button("Accessibility settings", new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        }));
        root.addView(button("Allow display over other apps (cover-screen confirm)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
        }));
        root.addView(button("App info (for \"Allow restricted settings\")", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
        }));
    }

    @Override protected void onResume() {
        super.onResume();
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        refresh();
    }

    @Override protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override public void onSensorChanged(SensorEvent e) { z = e.values[2]; refresh(); }
    @Override public void onAccuracyChanged(Sensor s, int a) {}

    private void refresh() {
        DevicePolicyManager d = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        status.setText(String.format("Sensor z: %.1f (%s)", z, z < -7.5f ? "face-down" : z > 7.5f ? "face-up" : "tilted")
                + "\nService: " + (TriggerService.running ? "running" : "stopped")
                + "\nDevice owner: " + (d.isDeviceOwnerApp(getPackageName()) ? "yes" : "no")
                + "\nAccessibility: " + (CloserAccessibilityService.instance != null ? "running" : "off")
                + "\nOverlay: " + (ConfirmOverlay.allowed(this) ? "allowed" : "off (notification is used)")
                + "\nLast: " + Prefs.last(this) + "\n");
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }
}
