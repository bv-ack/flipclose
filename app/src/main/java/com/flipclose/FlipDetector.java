package com.flipclose;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.Looper;

/** Purpose: turn raw accelerometer data into "flipped face-down" and "flipped back". */
final class FlipDetector implements SensorEventListener {
    interface Callback { void onFlipDown(); void onFlipBack(); }

    private static final float DOWN = -7.5f;   // z below this = face-down
    private static final float UP = -3f;       // z above this = not face-down
    private static final long HOLD_MS = 1000;  // must stay face-down this long

    private final Callback cb;
    private final Handler h = new Handler(Looper.getMainLooper());
    private float z;
    private boolean first = true, armed = false, down = false, fired = false;

    private final Runnable confirm = new Runnable() {
        @Override public void run() {
            if (down) { fired = true; cb.onFlipDown(); }
        }
    };

    FlipDetector(Callback cb) { this.cb = cb; }

    @Override public void onSensorChanged(SensorEvent e) {
        float raw = e.values[2];
        z = first ? raw : 0.8f * z + 0.2f * raw;
        first = false;
        if (z > UP) {
            armed = true; // only trigger after the device has been seen not face-down
            if (down) {
                down = false;
                h.removeCallbacks(confirm);
                if (fired) { fired = false; cb.onFlipBack(); }
            }
        } else if (z < DOWN && armed && !down) {
            down = true;
            h.postDelayed(confirm, HOLD_MS);
        }
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}
}
