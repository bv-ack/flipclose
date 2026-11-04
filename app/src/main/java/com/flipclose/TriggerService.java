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
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Purpose: keep the sensor alive, run the countdown, call the closer, and report what happened. */
public class TriggerService extends Service implements FlipDetector.Callback {
    static boolean running;
    private static final String QUIET = "quiet", ALERT = "alert", CANCEL = "com.flipclose.CANCEL";

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tickR = new Runnable() { @Override public void run() { tick(); } };
    private NotificationManager nm;
    private SensorManager sm;
    private FlipDetector det;
    private ConfirmOverlay overlay;
    private int left = -1; // seconds remaining; -1 = no countdown running

    @Override public void onCreate() {
        running = true;
        nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(QUIET, "Listening", NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(ALERT, "Countdown", NotificationManager.IMPORTANCE_HIGH));
        startForeground(1, new Notification.Builder(this, QUIET)
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setContentTitle("FlipClose is listening").build());
        sm = getSystemService(SensorManager.class);
        det = new FlipDetector(this);
        overlay = new ConfirmOverlay(this);
        sm.registerListener(det, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        say("Listening started");
    }

    @Override public int onStartCommand(Intent i, int flags, int id) {
        if (i != null && CANCEL.equals(i.getAction())) { cancel(); say("Cancelled (button)"); }
        return START_STICKY;
    }

    @Override public void onFlipDown() {
        say("Flip detected, countdown " + Prefs.countdown(this) + " s");
        left = Prefs.countdown(this);
        tick();
    }

    @Override public void onFlipBack() {
        if (left >= 0) { cancel(); say("Cancelled (flipped back)"); }
    }

    private void tick() {
        if (left < 0) return;
        if (left == 0) {
            left = -1;
            overlay.hide();
            String r;
            try { r = Closers.run(this); } catch (Exception e) { r = "Failed: " + e.getMessage(); }
            say(r);
            show("FlipClose", r, false);
            return;
        }
        String msg = "Closing apps in " + left + " s";
        if (ConfirmOverlay.allowed(this)) {
            overlay.show(new Runnable() {
                @Override public void run() { cancel(); say("Cancelled (button)"); }
            });
            overlay.setText(msg + "\nFlip back or tap Cancel");
        } else {
            show(msg, "Flip back or tap Cancel", true);
        }
        left--;
        h.postDelayed(tickR, 1000);
    }

    private void cancel() {
        left = -1;
        h.removeCallbacks(tickR);
        nm.cancel(2);
        overlay.hide();
    }

    /** Toasts work even if notifications are blocked, and the text is kept for the app screen. */
    private void say(String t) {
        String stamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        Prefs.setLast(this, stamp + "  " + t);
        Toast.makeText(getApplicationContext(), t, Toast.LENGTH_LONG).show();
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
        running = false;
        overlay.hide();
        sm.unregisterListener(det);
        h.removeCallbacksAndMessages(null);
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
