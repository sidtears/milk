package com.tapstamp;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Saves feed timestamps (epoch millis) in SharedPreferences, oldest first. */
final class Store {
    private static final String PREFS = "tapstamp";
    private static final String KEY = "taps";

    private Store() {}

    static synchronized void add(Context c, long millis) {
        List<Long> l = all(c);
        l.add(millis);
        write(c, l);
    }

    static synchronized List<Long> all(Context c) {
        String cur = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "");
        List<Long> out = new ArrayList<>();
        if (cur.isEmpty()) return out;
        for (String s : cur.split(",")) {
            try { out.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    /** Removes the entry at this position in the oldest-first list. */
    static synchronized void remove(Context c, int index) {
        List<Long> l = all(c);
        if (index < 0 || index >= l.size()) return;
        l.remove(index);
        write(c, l);
    }

    static synchronized void clear(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }

    private static void write(Context c, List<Long> l) {
        SharedPreferences.Editor e = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (l.isEmpty()) {
            e.remove(KEY);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(l.get(i));
            }
            e.putString(KEY, sb.toString());
        }
        e.apply();
    }

    /** 12-hour time, hours and minutes only, e.g. "2:35 PM". */
    static String time(long millis) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(millis));
    }

    /** e.g. "Thu 26 Nov". */
    static String date(long millis) {
        return new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date(millis));
    }

    /** e.g. "Thu 26 Nov  ·  2:35 PM". */
    static String full(long millis) {
        return date(millis) + "  ·  " + time(millis);
    }

    /** How many feeds were logged since midnight today. */
    static int todayCount(List<Long> taps) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        int n = 0;
        for (Long t : taps) if (t >= start) n++;
        return n;
    }
}
