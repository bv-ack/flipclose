package com.flipclose;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/** Purpose: open recents, press its clear-all button, go home. Experimental: button text differs per OEM. */
public class CloserAccessibilityService extends AccessibilityService {
    static CloserAccessibilityService instance;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override protected void onServiceConnected() { instance = this; }
    @Override public boolean onUnbind(Intent i) { instance = null; return super.onUnbind(i); }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}

    void clearRecents(String label) {
        performGlobalAction(GLOBAL_ACTION_RECENTS);
        attempt(label, 0);
    }

    private void attempt(final String label, final int n) {
        h.postDelayed(new Runnable() {
            @Override public void run() {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null && click(root, label)) {
                    h.postDelayed(new Runnable() {
                        @Override public void run() { performGlobalAction(GLOBAL_ACTION_HOME); }
                    }, 400);
                    return;
                }
                if (root != null) scroll(root);
                if (n < 6) attempt(label, n + 1);
                else performGlobalAction(GLOBAL_ACTION_HOME);
            }
        }, 600);
    }

    private boolean click(AccessibilityNodeInfo root, String label) {
        List<AccessibilityNodeInfo> hits = root.findAccessibilityNodeInfosByText(label);
        for (AccessibilityNodeInfo n : hits) {
            for (AccessibilityNodeInfo x = n; x != null; x = x.getParent()) {
                if (x.isClickable() && x.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
        }
        return false;
    }

    /** The clear-all button can sit at the end of the carousel, so scroll the first scrollable view. */
    private boolean scroll(AccessibilityNodeInfo n) {
        if (n.isScrollable()) return n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c != null && scroll(c)) return true;
        }
        return false;
    }
}
