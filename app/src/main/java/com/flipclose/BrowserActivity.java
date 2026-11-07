package com.flipclose;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Purpose: a small browser for YouTube (allowed channels only) and WhatsApp Web (Status and Channels hidden). */
public class BrowserActivity extends Activity {
    private static final String DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
    private static final String[] HOSTS = {"youtube.com", "youtu.be", "whatsapp.com"}; // sites that may be opened at all
    private static final String START = "https://m.youtube.com/_start";                  // our own start page

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Map<String, Boolean> cache = new HashMap<>(); // video id -> channel allowed
    private WebView web;
    private boolean whatsapp, pending;
    private String lastChecked = "";
    private String waJs = "";

    private void toast(String t) { Toast.makeText(getApplicationContext(), t, Toast.LENGTH_SHORT).show(); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try { waJs = readAsset("wa.js"); } catch (Exception e) { waJs = ""; }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = new LinearLayout(this);
        bar.addView(tab("YouTube", new View.OnClickListener() { @Override public void onClick(View v) { openSite(false); } }),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(tab("WhatsApp", new View.OnClickListener() { @Override public void onClick(View v) { openSite(true); } }),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(tab("Back", new View.OnClickListener() { @Override public void onClick(View v) { if (web.canGoBack()) web.goBack(); } }),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(tab("Reload", new View.OnClickListener() { @Override public void onClick(View v) { web.reload(); } }),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(bar);
        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String sc = u.getScheme();
                if ("about".equals(sc) || "data".equals(sc)) return false;
                if (!"https".equals(sc) && !"http".equals(sc)) return true;   // no app links, no other schemes
                if (!hostAllowed(u)) { toast("Blocked: " + u.getHost()); return true; }
                return false;
            }

            @Override public void onPageFinished(WebView v, String url) {
                if (whatsapp && Prefs.waHide(BrowserActivity.this) && !waJs.isEmpty()) v.evaluateJavascript(waJs, null);
            }
        });
        openSite(false);
    }

    private Button tab(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }

    private void openSite(boolean wa) {
        whatsapp = wa;
        pending = false;
        WebSettings s = web.getSettings();
        s.setUserAgentString(wa ? DESKTOP_UA : null);   // WhatsApp Web needs a desktop browser
        s.setUseWideViewPort(wa);
        s.setLoadWithOverviewMode(wa);
        s.setBlockNetworkImage(Prefs.bImagesOff(this));
        if (wa) web.loadUrl("https://web.whatsapp.com/");
        else if (Prefs.ytOnly(this)) showStart();
        else web.loadUrl("https://m.youtube.com/");
    }

    private boolean hostAllowed(Uri u) {
        String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
        for (String d : HOSTS) if (host.equals(d) || host.endsWith("." + d)) return true;
        return false;
    }

    // ---- YouTube: only allowed channels ----

    private boolean allowed(String key) {
        for (String ch : Prefs.channels(this)) if (ch.equalsIgnoreCase(key)) return true;
        return false;
    }

    private void showStart() {
        lastChecked = START;
        StringBuilder sb = new StringBuilder("<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                + "<body style='font-family:sans-serif;padding:16px;background:#111;color:#eee'><h3>Allowed channels</h3>");
        List<String> list = new ArrayList<>(Prefs.channels(this));
        Collections.sort(list);
        if (list.isEmpty()) sb.append("<p>None yet. Add some in FlipClose settings.</p>");
        for (String ch : list) {
            String url = ch.startsWith("@") ? "https://m.youtube.com/" + ch + "/videos" : "https://m.youtube.com/channel/" + ch + "/videos";
            sb.append("<p><a style='color:#8ab4f8;font-size:20px' href='").append(url).append("'>").append(ch).append("</a></p>");
        }
        sb.append("</body></html>");
        web.loadDataWithBaseURL(START, sb.toString(), "text/html", "UTF-8", START);
    }

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (pending) pauseVideos();
            if (!whatsapp && Prefs.ytOnly(BrowserActivity.this)) {
                web.evaluateJavascript("location.href", new ValueCallback<String>() {
                    @Override public void onReceiveValue(String v) { seen(v); }
                });
            }
            h.postDelayed(this, 600);
        }
    };

    /** YouTube changes pages without a real page load, so the address is checked every 0.6 s. */
    private void seen(String raw) {
        if (raw == null) return;
        String url;
        try {
            Object o = new JSONTokener(raw).nextValue();
            if (!(o instanceof String)) return;
            url = (String) o;
        } catch (Exception e) { return; }
        if (url.equals(lastChecked)) return;
        lastChecked = url;
        pending = false;
        check(url);
    }

    private void check(String url) {
        Uri u = Uri.parse(url);
        String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
        if (!(host.equals("youtube.com") || host.endsWith(".youtube.com"))) return;
        if (host.startsWith("consent.")) return;
        String path = u.getPath() == null ? "" : u.getPath();
        if (path.equals("/_start")) return;
        if (path.startsWith("/@")) {
            String name = path.substring(2).split("/")[0].toLowerCase(Locale.ROOT);
            if (!allowed("@" + name)) block("That channel is not on your list");
            return;
        }
        if (path.startsWith("/channel/")) {
            String[] parts = path.split("/");
            if (parts.length > 2 && allowed(parts[2])) return;
            block("That channel is not on your list");
            return;
        }
        String id = null;
        if (path.startsWith("/watch")) id = u.getQueryParameter("v");
        else if (path.startsWith("/shorts/") || path.startsWith("/live/")) id = path.substring(path.lastIndexOf('/') + 1);
        if (id != null && !id.isEmpty()) { verdictFor(id, url); return; }
        block("Only your allowed channels can be opened");   // home, search, feeds, playlists...
    }

    private void verdictFor(final String id, final String url) {
        Boolean known = cache.get(id);
        if (known != null) { if (!known) block("That channel is not on your list"); return; }
        pending = true;
        pauseVideos();
        new Thread(new Runnable() {
            @Override public void run() {
                final Boolean ok = authorAllowed(id);
                h.post(new Runnable() {
                    @Override public void run() {
                        if (ok != null) cache.put(id, ok);
                        if (!url.equals(lastChecked)) return;   // the page moved on meanwhile
                        pending = false;
                        if (ok != null && ok) playVideos();
                        else block(ok == null ? "Could not check the channel, so it was blocked" : "That channel is not on your list");
                    }
                });
            }
        }).start();
    }

    /** Asks YouTube's public oEmbed address who made the video. null = could not find out (counts as blocked). */
    private Boolean authorAllowed(String id) {
        try {
            String target = "https://www.youtube.com/watch?v=" + id;
            URL u = new URL("https://www.youtube.com/oembed?format=json&url=" + URLEncoder.encode(target, "UTF-8"));
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(4000);
            c.setReadTimeout(4000);
            if (c.getResponseCode() != 200) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            String author = new JSONObject(sb.toString()).optString("author_url", "");
            String last = author.substring(author.lastIndexOf('/') + 1);
            return !last.isEmpty() && allowed(last);
        } catch (Exception e) {
            return null;
        }
    }

    private void block(String why) {
        pending = false;
        pauseVideos();
        toast(why);
        showStart();
    }

    private void pauseVideos() {
        web.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){v.pause();})", null);
    }

    private void playVideos() {
        web.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){v.play();})", null);
    }

    // ---- lifecycle ----

    @Override protected void onResume() {
        super.onResume();
        web.onResume();
        cache.clear();
        lastChecked = "";   // check the page again, the list may have changed
        pending = false;
        h.post(poll);
    }

    @Override protected void onPause() {
        super.onPause();
        h.removeCallbacksAndMessages(null);
        web.onPause();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        web.destroy();
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    private String readAsset(String name) throws Exception {
        InputStream in = getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }
}
