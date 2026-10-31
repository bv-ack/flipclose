package com.flipclose;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** Purpose: keep the sensor alive, run the countdown, call the closer. */
public class TriggerService extends Service implements FlipDetector.Callback {
    private static final String QUIET = "quiet", ALERT = "alert", CANCEL = "com.flipclose.CANCEL";

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tickR = new Runnable() { @Override public void run() { tick(); } };
    private NotificationManager nm;
    private SensorManager sm;
    private FlipDetector det;
    private int left = -1; // seconds remaining; -1 = no countdown running

    @Override public void onCreate() {
        nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(QUIET, "Listening", NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(ALERT, "Countdown", NotificationManager.IMPORTANCE_HIGH));
        startForeground(1, new Notification.Builder(this, QUIET)
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setContentTitle("FlipClose is listening").build());
        sm = getSystemService(SensorManager.class);
        det = new FlipDetector(this);
        sm.registerListener(det, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override public int onStartCommand(Intent i, int flags, int id) {
        if (i != null && CANCEL.equals(i.getAction())) cancel();
        return START_STICKY;
    }

    @Override public void onFlipDown() { left = Prefs.countdown(this); tick(); }

    @Override public void onFlipBack() { if (left >= 0) cancel(); }

    private void tick() {
        if (left < 0) return;
        if (left == 0) {
            left = -1;
            String r;
            if (Prefs.dryRun(this)) r = "Dry run: flip detected, nothing closed";
            else {
                try { r = Closers.run(this); } catch (Exception e) { r = "Failed: " + e.getMessage(); }
            }
            show("FlipClose", r, false);
            return;
        }
        show("Closing apps in " + left + " s", "Flip back or tap Cancel", true);
        left--;
        h.postDelayed(tickR, 1000);
    }

    private void cancel() {
        left = -1;
        h.removeCallbacks(tickR);
        nm.cancel(2);
    }

    private void show(String title, String text, boolean cancelable) {
        Notification.Builder b = new Notification.Builder(this, ALERT)
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setContentTitle(title).setContentText(text).setOnlyAlertOnce(true);
        if (cancelable) {
            PendingIntent pi = PendingIntent.getService(this, 0,
                    new Intent(this, TriggerService.class).setAction(CANCEL),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            b.addAction(new Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_delete), "Cancel", pi).build());
        } else {
            b.setTimeoutAfter(6000);
        }
        nm.notify(2, b.build());
    }

    @Override public void onDestroy() {
        sm.unregisterListener(det);
        h.removeCallbacksAndMessages(null);
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
