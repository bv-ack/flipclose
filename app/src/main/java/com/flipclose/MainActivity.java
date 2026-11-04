package com.flipclose;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
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
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Purpose: the one settings screen (behind the PIN, if set), with live feedback so you can see what works. */
public class MainActivity extends Activity implements SensorEventListener {
    private static final String[] SCOPES = {"Close all apps", "Close only the app in front"};
    private static final String[] MODES = {"Soft: stop apps in the background", "Accessibility: clear recents (always all apps)", "Device owner: suspend apps"};
    private static final long RELOCK_MS = 60000L;

    private static boolean unlocked;
    private static long leftAt;

    private interface Choice { void pick(int i); }

    private final Handler ui = new Handler(Looper.getMainLooper());
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
    }

    @Override protected void onResume() {
        super.onResume();
        if (unlocked && SystemClock.elapsedRealtime() - leftAt > RELOCK_MS) unlocked = false;
        show();
    }

    @Override protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override protected void onStop() {
        super.onStop();
        leftAt = SystemClock.elapsedRealtime();
    }

    private void show() {
        if (Lock.isSet(this) && !unlocked) { gate(); return; }
        buildMain();
        try { Enforcer.apply(this, false); } catch (Exception e) { /* not device owner */ }
        DevicePolicyManager d = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        slow = "\nDevice owner: " + (d.isDeviceOwnerApp(getPackageName()) ? "yes" : "no")
                + "\nUsage access: " + (Apps.usageAllowed(this) ? "yes" : "no")
                + "\nBackground: " + (pm.isIgnoringBatteryOptimizations(getPackageName()) ? "unrestricted" : "optimized (may be killed)")
                + "\nOverlay: " + (ConfirmOverlay.allowed(this) ? "allowed" : "off")
                + "\nPIN: " + (Lock.isSet(this) ? "set" : "not set");
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        refresh();
    }

    // ---- locked screen ----

    private void gate() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(60, 60, 60, 60);
        TextView t = new TextView(this);
        t.setText("FlipClose is locked");
        t.setTextSize(22);
        t.setGravity(Gravity.CENTER);
        box.addView(t);
        final EditText pin = pinField("PIN");
        box.addView(pin);
        final TextView msg = new TextView(this);
        msg.setGravity(Gravity.CENTER);
        box.addView(msg);
        box.addView(button("Unlock", new View.OnClickListener() {
            @Override public void onClick(View v) {
                long wait = Lock.waitMs(MainActivity.this);
                if (wait > 0) { msg.setText("Too many tries. Wait " + (wait / 1000 + 1) + " s"); return; }
                if (Lock.check(MainActivity.this, pin.getText().toString())) { unlocked = true; show(); }
                else msg.setText("Wrong PIN");
            }
        }));
        box.addView(button("Forgot PIN (recovery code)", new View.OnClickListener() {
            @Override public void onClick(View v) { recoveryDialog(); }
        }));
        setContentView(box);
    }

    private void recoveryDialog() {
        final EditText code = new EditText(this);
        code.setHint("Recovery code");
        new AlertDialog.Builder(this).setTitle("Recovery").setView(code)
                .setPositiveButton("Unlock", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if (Lock.useRecovery(MainActivity.this, code.getText().toString())) {
                            unlocked = true;
                            toast("PIN removed. Set a new one");
                            show();
                        } else {
                            long wait = Lock.waitMs(MainActivity.this);
                            toast(wait > 0 ? "Too many tries. Wait " + (wait / 1000 + 1) + " s" : "Wrong code");
                        }
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void setPinDialog() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(40, 20, 40, 0);
        final EditText a = pinField("New 6-digit PIN");
        final EditText b = pinField("Repeat PIN");
        l.addView(a);
        l.addView(b);
        new AlertDialog.Builder(this).setTitle("Set PIN").setView(l)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String p1 = a.getText().toString();
                        if (!p1.matches("\\d{6}")) { toast("The PIN must be exactly 6 digits"); return; }
                        if (!p1.equals(b.getText().toString())) { toast("The PINs don't match"); return; }
                        showCode(Lock.set(MainActivity.this, p1));
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showCode(String code) {
        unlocked = true;
        new AlertDialog.Builder(this).setTitle("Recovery code").setCancelable(false)
                .setMessage("Write this down or give it to someone you trust. It is shown only once, and it removes the PIN if you forget it.\n\n" + code)
                .setPositiveButton("I saved it", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { show(); }
                }).show();
    }

    private EditText pinField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return e;
    }

    // ---- main screen ----

    private void buildMain() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);

        status = new TextView(this);
        root.addView(status);

        Switch run = new Switch(this);
        run.setText("Run service (face-down and downtime)");
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

        root.addView(heading("What to close (Soft and Device owner)"));
        root.addView(radios(SCOPES, Prefs.scope(this), 2000, new Choice() {
            @Override public void pick(int i) { Prefs.setScope(MainActivity.this, i); toast("Saved: " + SCOPES[i]); }
        }));
        root.addView(heading("How to close"));
        root.addView(radios(MODES, Prefs.mode(this), 1000, new Choice() {
            @Override public void pick(int i) {
                Prefs.setMode(MainActivity.this, i);
                toast("Saved: " + MODES[i]);
                ui.post(new Runnable() { @Override public void run() { show(); } });
            }
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
                ui.postDelayed(new Runnable() {
                    @Override public void run() {
                        String r;
                        try { r = Closers.run(MainActivity.this); } catch (Exception e) { r = "Failed: " + e.getMessage(); }
                        Prefs.setLast(MainActivity.this, "Test: " + r);
                        toast(r);
                    }
                }, 5000);
            }
        }));

        root.addView(heading("Downtime and blocked apps (device owner)"));
        Switch dt = new Switch(this);
        dt.setText("Downtime on (the service must be running)");
        dt.setChecked(Prefs.dtOn(this));
        dt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) {
                Prefs.setDtOn(MainActivity.this, on);
                applyNow(false);
            }
        });
        root.addView(dt);
        final EditText from = timeField("Start, 24 h (22:00)", Prefs.dtStart(this));
        final EditText to = timeField("End, 24 h (07:00)", Prefs.dtEnd(this));
        root.addView(from);
        root.addView(to);
        root.addView(button("Save downtime times", new View.OnClickListener() {
            @Override public void onClick(View v) {
                int s = parseTime(from.getText().toString()), e = parseTime(to.getText().toString());
                if (s < 0 || e < 0) { toast("Use the form HH:MM, for example 22:00"); return; }
                Prefs.setDtTimes(MainActivity.this, s, e);
                toast("Saved: " + hm(s) + " to " + hm(e));
                applyNow(false);
            }
        }));
        root.addView(button("Downtime apps", open(new Intent(this, BlockedAppsActivity.class)
                .putExtra(BlockedAppsActivity.WHICH, "downtime"))));
        root.addView(button("Blocked apps (always suspended)", open(new Intent(this, BlockedAppsActivity.class)
                .putExtra(BlockedAppsActivity.WHICH, "blocked"))));
        root.addView(button("Apply rules now", new View.OnClickListener() {
            @Override public void onClick(View v) { applyNow(true); }
        }));

        root.addView(heading("PIN lock (protects this screen)"));
        root.addView(button(Lock.isSet(this) ? "Change PIN" : "Set 6-digit PIN", new View.OnClickListener() {
            @Override public void onClick(View v) { setPinDialog(); }
        }));
        if (Lock.isSet(this)) {
            root.addView(button("Remove PIN", new View.OnClickListener() {
                @Override public void onClick(View v) { Lock.clear(MainActivity.this); toast("PIN removed"); show(); }
            }));
        }

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
        root.addView(button("Protect this app (no uninstall, no clearing data)", ownerTool(0)));
        root.addView(button("Remove app protection", ownerTool(1)));
        root.addView(button("Lock date and time", ownerTool(2)));
        root.addView(button("Unlock date and time", ownerTool(3)));
        root.addView(button("Restore apps (undo suspension)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                String r;
                try { r = Closers.restore(MainActivity.this); } catch (Exception e) { r = "Failed: " + e.getMessage(); }
                toast(r);
            }
        }));
    }

    @Override public void onSensorChanged(SensorEvent e) { z = e.values[2]; refresh(); }
    @Override public void onAccuracyChanged(Sensor s, int a) {}

    private void refresh() {
        if (status == null) return;
        status.setText(String.format("Sensor z: %.1f (%s)", z, z < -7.5f ? "face-down" : z > 7.5f ? "face-up" : "tilted")
                + "\nService: " + (TriggerService.running ? "running" : "stopped")
                + "\nAccessibility: " + (CloserAccessibilityService.instance != null ? "running" : "off")
                + slow
                + "\nDowntime: " + (Prefs.dtOn(this) ? hm(Prefs.dtStart(this)) + "-" + hm(Prefs.dtEnd(this))
                        + (Enforcer.downtimeActive(this) ? " (active)" : " (idle)") : "off")
                + "\nSuspended by rules: " + Prefs.applied(this).size()
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

    private void applyNow(boolean force) {
        String r;
        try { r = Enforcer.apply(this, force); } catch (Exception e) { r = "Failed: " + e.getMessage(); }
        toast(r);
    }

    private View.OnClickListener ownerTool(final int what) {
        return new View.OnClickListener() {
            @Override public void onClick(View v) {
                String r;
                try {
                    switch (what) {
                        case 0: r = Closers.protectApp(MainActivity.this, true); break;
                        case 1: r = Closers.protectApp(MainActivity.this, false); break;
                        case 2: r = Closers.lockTime(MainActivity.this, true); break;
                        default: r = Closers.lockTime(MainActivity.this, false);
                    }
                } catch (Exception e) { r = "Failed: " + e.getMessage(); }
                toast(r);
            }
        };
    }

    private EditText timeField(String hint, int minutes) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        e.setText(hm(minutes));
        return e;
    }

    private static String hm(int minutes) { return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60); }

    /** "HH:MM" to minutes after midnight, or -1 if it is not a valid time. */
    private static int parseTime(String s) {
        Matcher m = Pattern.compile("^\\s*(\\d{1,2}):(\\d{2})\\s*$").matcher(s);
        if (!m.matches()) return -1;
        int h = Integer.parseInt(m.group(1)), mi = Integer.parseInt(m.group(2));
        return h > 23 || mi > 59 ? -1 : h * 60 + mi;
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }
}
