package com.flipclose;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Purpose: restart listening after the device boots. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Prefs.enabled(c)) c.startForegroundService(new Intent(c, TriggerService.class));
    }
}
