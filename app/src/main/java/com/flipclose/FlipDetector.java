package com.flipclose;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.Looper;

/**
 * Purpose: turn raw accelerometer data into "flipped" and "flipped back".
 * Two kinds of flip:
 *  1. flat face-down (gravity on the screen axis), held about a second;
 *  2. upside-down: from a steady upright pose, gravity reverses and the device is steady again.
 */
final class FlipDetector implements SensorEventListener {
    interface Callback { void onFlipDown(); void onFlipBack(); boolean upsideEnabled(); }

    private static final float DOWN = -7.5f, UP = -3f;   // face-down thresholds on z
    private static final long HOLD_MS = 1000;
    private static final float JOLT = 1.0f;              // summed change that counts as movement
    private static final long STEADY_MS = 800;           // how long the device must be still

    private final Callback cb;
    private final Handler h = new Handler(Looper.getMainLooper());

    // face-down state
    private float z;
    private boolean first = true, armed = false, down = false, fired = false;
    private final Runnable confirm = new Runnable() {
        @Override public void run() {
            if (down) { fired = true; cb.onFlipDown(); }
        }
    };

    // upside-down state
    private final float[] prev = new float[3], pose = new float[3];
    private float[] ref;          // last steady pose before a flip
    private long steadySince;
    private boolean relFired;

    FlipDetector(Callback cb) { this.cb = cb; }

    @Override public void onSensorChanged(SensorEvent e) {
        faceDown(e.values[2]);
        if (cb.upsideEnabled()) upsideDown(e);
        else { ref = null; relFired = false; }
    }

    private void faceDown(float raw) {
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

    private void upsideDown(SensorEvent e) {
        float[] v = e.values;
        long now = e.timestamp / 1000000L;
        float jolt = Math.abs(v[0] - prev[0]) + Math.abs(v[1] - prev[1]) + Math.abs(v[2] - prev[2]);
        for (int i = 0; i < 3; i++) { pose[i] = 0.5f * pose[i] + 0.5f * v[i]; prev[i] = v[i]; }
        if (jolt > JOLT) { steadySince = now; return; }
        if (now - steadySince < STEADY_MS) return;

        // the device has been still long enough: pose can be trusted
        if (ref == null) { ref = pose.clone(); return; }
        float c = cos(pose, ref);
        boolean upright = Math.abs(ref[2]) < 5f && Math.abs(pose[2]) < 5f; // flat flips are handled above
        if (!relFired) {
            if (c < -0.8f && upright) { relFired = true; cb.onFlipDown(); }
            else ref = pose.clone();            // same pose, or a new resting pose
        } else if (c > -0.8f) {                 // came back, or moved somewhere else
            relFired = false;
            if (c <= 0.8f) ref = pose.clone();
            cb.onFlipBack();
        }
    }

    private static float cos(float[] a, float[] b) {
        float d = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        float n = (float) Math.sqrt((a[0] * a[0] + a[1] * a[1] + a[2] * a[2]) * (b[0] * b[0] + b[1] * b[1] + b[2] * b[2]));
        return n == 0 ? 1 : d / n;
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}
}
