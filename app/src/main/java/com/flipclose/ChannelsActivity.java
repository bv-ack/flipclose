package com.flipclose;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Purpose: the list of YouTube channels the Safe Browser may open. Untick one to remove it. */
public class ChannelsActivity extends Activity {
    private final List<String> items = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView lv;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Allowed YouTube channels");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final EditText input = new EditText(this);
        input.setHint("Handle like @name, or a channel link");
        input.setSingleLine(true);
        Button add = new Button(this);
        add.setText("Add");
        lv = new ListView(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        box.addView(input);
        box.addView(add);
        box.addView(lv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(box);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, items);
        lv.setAdapter(adapter);
        reload();

        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String key = normalize(input.getText().toString());
                if (key == null) {
                    Toast.makeText(getApplicationContext(), "Use @name, a youtube.com/@name link, or a channel ID (UC...)", Toast.LENGTH_LONG).show();
                    return;
                }
                Set<String> s = Prefs.channels(ChannelsActivity.this);
                s.add(key);
                Prefs.setChannels(ChannelsActivity.this, s);
                input.setText("");
                reload();
            }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (lv.isItemChecked(pos)) return;
                Set<String> s = Prefs.channels(ChannelsActivity.this);
                s.remove(items.get(pos));
                Prefs.setChannels(ChannelsActivity.this, s);
                reload();
            }
        });
    }

    private void reload() {
        items.clear();
        items.addAll(new TreeSet<>(Prefs.channels(this)));
        adapter.notifyDataSetChanged();
        for (int i = 0; i < items.size(); i++) lv.setItemChecked(i, true);
    }

    /** "@Name", a channel link, or a channel ID to the form that is stored. null = not valid. */
    private static String normalize(String raw) {
        String s = raw.trim();
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        if (s.isEmpty()) return null;
        int at = s.indexOf("/@");
        if (at >= 0) {
            s = s.substring(at + 1);
        } else {
            int ch = s.indexOf("/channel/");
            if (ch >= 0) s = s.substring(ch + 9);
        }
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        if (s.matches("(?i)UC[A-Za-z0-9_-]{20,}")) return s;   // channel ID, capitals kept
        if (s.startsWith("@")) s = s.substring(1);
        s = s.toLowerCase(Locale.ROOT);
        return s.matches("[a-z0-9._-]{3,30}") ? "@" + s : null;
    }
}
