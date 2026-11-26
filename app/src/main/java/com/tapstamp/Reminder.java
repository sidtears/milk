package com.tapstamp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

/** Schedules one reminder, 3 hours after the most recent feed. Each new feed replaces it. */
final class Reminder {
    static final long INTERVAL_MS = 3L * 60 * 60 * 1000;
    private static final String PREFS = "tapstamp_settings";
    private static final String KEY_ON = "reminder_on";

    private Reminder() {}

    static boolean enabled(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ON, true);
    }

    static void setEnabled(Context c, boolean on) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ON, on).apply();
        reschedule(c);
    }

    /** Cancels any pending reminder and sets a new one from the latest feed, if there is one. */
    static void reschedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pending(c);
        am.cancel(pi);

        List<Long> taps = Store.all(c);
        if (!enabled(c) || taps.isEmpty()) return;

        long due = taps.get(taps.size() - 1) + INTERVAL_MS;
        if (due <= System.currentTimeMillis()) return;

        // Inexact alarm: needs no special permission, may arrive a few minutes late.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pi);
    }

    private static PendingIntent pending(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        return PendingIntent.getBroadcast(c, 100, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
