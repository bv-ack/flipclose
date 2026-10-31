package com.flipclose;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Purpose: a full-screen cover with one big Cancel button, so taps cannot reach the app underneath. */
final class ConfirmOverlay {
    private final Context c;
    private final WindowManager wm;
    private LinearLayout box;
    private TextView text;

    ConfirmOverlay(Context c) {
        this.c = c;
        this.wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
    }

    static boolean allowed(Context c) { return Settings.canDrawOverlays(c); }

    void show(final Runnable onCancel) {
        if (box != null) return;
        box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(0xE6000000);

        text = new TextView(c);
        text.setTextColor(Color.WHITE);
        text.setTextSize(28);
        text.setGravity(Gravity.CENTER);
        box.addView(text);

        Button cancel = new Button(c);
        cancel.setText("Cancel");
        cancel.setTextSize(24);
        cancel.setPadding(80, 40, 80, 40);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onCancel.run(); }
        });
        box.addView(cancel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        try { wm.addView(box, lp); } catch (Exception e) { box = null; }
    }

    void setText(String s) { if (text != null) text.setText(s); }

    void hide() {
        if (box == null) return;
        try { wm.removeView(box); } catch (Exception e) { /* already gone */ }
        box = null;
    }
}
