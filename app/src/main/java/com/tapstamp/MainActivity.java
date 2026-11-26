package com.tapstamp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Feed history: log a feed, undo, delete any entry, copy, and switch the 3-hour reminder. */
public class MainActivity extends Activity {

    private static final int CREAM = 0xFFFFF8F1;
    private static final int INK = 0xFF2F3E5C;
    private static final int BLUE = 0xFFCFE8F7;
    private static final int PEACH = 0xFFF7B7A3;

    private ArrayAdapter<String> adapter;
    private final List<String> rows = new ArrayList<>();
    private TextView count;
    private Button reminderBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (18 * d);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, (int) (36 * d), pad, pad);
        root.setBackgroundColor(CREAM);

        TextView title = new TextView(this);
        title.setText("Feed Log");
        title.setTextSize(26);
        title.setTextColor(INK);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Add the widget: long-press your home screen, choose Widgets, then Feed Log. "
                + "Tap a feed below to delete it.");
        hint.setTextSize(13);
        hint.setTextColor(0xFF6B7A99);
        hint.setPadding(0, (int) (4 * d), 0, (int) (14 * d));
        root.addView(hint);

        Button stamp = pill("Log a feed now", PEACH, d, 15);
        stamp.setOnClickListener(v -> {
            Store.add(this, System.currentTimeMillis());
            afterChange();
        });
        root.addView(stamp, rowParams(d));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);

        Button undo = pill("Undo last", BLUE, d, 13);
        undo.setOnClickListener(v -> {
            int n = Store.all(this).size();
            if (n > 0) {
                Store.remove(this, n - 1);
                afterChange();
            }
        });
        bar.addView(undo, weight(d, 0, 4));

        Button copy = pill("Copy all", BLUE, d, 13);
        copy.setOnClickListener(v -> copyAll());
        bar.addView(copy, weight(d, 4, 4));

        Button clear = pill("Clear", BLUE, d, 13);
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete all feeds?")
                .setPositiveButton("Delete all", (dlg, w) -> {
                    Store.clear(this);
                    afterChange();
                })
                .setNegativeButton("Cancel", null)
                .show());
        bar.addView(clear, weight(d, 4, 0));
        root.addView(bar, rowParams(d));

        reminderBtn = pill("", BLUE, d, 13);
        reminderBtn.setOnClickListener(v -> {
            Reminder.setEnabled(this, !Reminder.enabled(this));
            updateReminderLabel();
        });
        root.addView(reminderBtn, rowParams(d));

        count = new TextView(this);
        count.setTextSize(14);
        count.setTextColor(INK);
        count.setTypeface(null, Typeface.BOLD);
        count.setPadding(0, (int) (10 * d), 0, (int) (6 * d));
        root.addView(count);

        ListView list = new ListView(this);
        list.setDivider(new ColorDrawable(0xFFF0E4D8));
        list.setDividerHeight((int) d);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(INK);
                tv.setTextSize(16);
                return tv;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            final int index = rows.size() - 1 - position; // list is newest first
            new AlertDialog.Builder(this)
                    .setTitle("Delete this feed?")
                    .setMessage(rows.get(position))
                    .setPositiveButton("Delete", (dlg, w) -> {
                        Store.remove(this, index);
                        afterChange();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        // Android 13+ needs permission before notifications can show.
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private void afterChange() {
        StampWidget.refreshAll(this);
        Reminder.reschedule(this);
        reload();
    }

    private Button pill(String text, int color, float d, int sp) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(sp);
        btn.setTextColor(INK);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(24 * d);
        btn.setBackground(bg);
        return btn;
    }

    private LinearLayout.LayoutParams rowParams(float d) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, (int) (8 * d));
        return lp;
    }

    private LinearLayout.LayoutParams weight(float d, int leftDp, int rightDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins((int) (leftDp * d), 0, (int) (rightDp * d), 0);
        return lp;
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        updateReminderLabel();
    }

    private void updateReminderLabel() {
        reminderBtn.setText("Reminder every 3 hours: " + (Reminder.enabled(this) ? "On" : "Off"));
    }

    private void reload() {
        List<Long> taps = Store.all(this);
        rows.clear();
        int n = taps.size();
        for (int i = n - 1; i >= 0; i--) {
            rows.add(Store.full(taps.get(i)));
        }
        adapter.notifyDataSetChanged();
        int today = Store.todayCount(taps);
        count.setText(n == 0 ? "No feeds logged yet"
                : n + (n == 1 ? " feed logged" : " feeds logged") + "  ·  " + today + " today");
    }

    private void copyAll() {
        List<Long> taps = Store.all(this);
        List<String> lines = new ArrayList<>();
        for (Long t : taps) lines.add(Store.full(t));
        Collections.reverse(lines);
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Feed Log", String.join("\n", lines)));
        Toast.makeText(this, "Copied " + lines.size() + " feeds", Toast.LENGTH_SHORT).show();
    }
}
