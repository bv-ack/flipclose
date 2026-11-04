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
import android.os.PowerManager;
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
    private static final String[] SCOPES = {"Close all apps", "Close only the app in front"};
    private static final String[] MODES = {"Soft: stop apps in the background", "Accessibility: clear recents", "Device owner: suspend apps"};

    private interface Choice { void pick(int i); }

    private TextView status;
    private SensorManager sm;
    private float z;
    private String slow = "";

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
        run.setText("Listen for face-down");
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

        root.addView(heading("What to close"));
        root.addView(radios(SCOPES, Prefs.scope(this), 2000, new Choice() {
            @Override public void pick(int i) { Prefs.setScope(MainActivity.this, i); toast("Saved: " + SCOPES[i]); }
        }));
        root.addView(heading("How to close"));
        root.addView(radios(MODES, Prefs.mode(this), 1000, new Choice() {
            @Override public void pick(int i) { Prefs.setMode(MainActivity.this, i); toast("Saved: " + MODES[i]); }
        }));

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

        root.addView(button("Test close in 5 s", new View.OnClickListener() {
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

        root.addView(heading("Permissions and tools"));
        root.addView(button("Never-close apps", open(new Intent(this, NeverCloseActivity.class))));
        root.addView(button("Usage access (to know the app in front)", open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        root.addView(button("Allow running in background (battery)", open(new Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())))));
        root.addView(button("Accessibility settings", open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        root.addView(button("Display over other apps (optional)", open(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())))));
        root.addView(button("App info (for \"Allow restricted settings\")", open(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))));

        root.addView(heading("Device owner tools"));
        root.addView(button("Cloudflare family DNS", dnsClick(true)));
        root.addView(button("Reset DNS to automatic", dnsClick(false)));
        root.addView(button("Restore apps (undo suspension)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                String r;
                try { r = Closers.restore(MainActivity.this); } catch (Exception e) { r = "Failed: " + e.getMessage(); }
                toast(r);
            }
        }));
    }

    @Override protected void onResume() {
        super.onResume();
        DevicePolicyManager d = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        slow = "\nDevice owner: " + (d.isDeviceOwnerApp(getPackageName()) ? "yes" : "no")
                + "\nUsage access: " + (Apps.usageAllowed(this) ? "yes" : "no")
                + "\nBackground: " + (pm.isIgnoringBatteryOptimizations(getPackageName()) ? "unrestricted" : "optimized (may be killed)")
                + "\nOverlay: " + (ConfirmOverlay.allowed(this) ? "allowed" : "off");
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
        status.setText(String.format("Sensor z: %.1f (%s)", z, z < -7.5f ? "face-down" : z > 7.5f ? "face-up" : "tilted")
                + "\nService: " + (TriggerService.running ? "running" : "stopped")
                + "\nAccessibility: " + (CloserAccessibilityService.instance != null ? "running" : "off")
                + slow
                + "\nLast: " + Prefs.last(this) + "\n");
    }

    private TextView heading(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(16);
        v.setPadding(0, 40, 0, 10);
        return v;
    }

    private RadioGroup radios(String[] names, int selected, int base, final Choice ch) {
        final int b = base;
        RadioGroup g = new RadioGroup(this);
        for (int i = 0; i < names.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setText(names[i]);
            r.setId(b + i);
            g.addView(r);
        }
        g.check(b + selected);
        g.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup grp, int id) { ch.pick(id - b); }
        });
        return g;
    }

    private View.OnClickListener open(final Intent i) {
        return new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(i); }
        };
    }

    private View.OnClickListener dnsClick(final boolean family) {
        return new View.OnClickListener() {
            @Override public void onClick(View v) {
                toast("Working...");
                new Thread(new Runnable() {
                    @Override public void run() {
                        String r;
                        try { r = Closers.dns(MainActivity.this, family); } catch (Exception e) { r = "Failed: " + e.getMessage(); }
                        final String fr = r;
                        runOnUiThread(new Runnable() { @Override public void run() { toast(fr); } });
                    }
                }).start();
            }
        };
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }
}
