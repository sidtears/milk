package com.tapstamp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.List;

public class StampWidget extends AppWidgetProvider {

    static final String ACTION_STAMP = "com.tapstamp.ACTION_STAMP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_STAMP.equals(intent.getAction())) {
            // Grab the time first, before anything else runs.
            long now = System.currentTimeMillis();
            Store.add(context, now);
            try {
                Reminder.reschedule(context);
            } catch (Exception ignored) {
                // A reminder problem must never stop the feed from being saved.
            }
            Toast.makeText(context, "Feed saved  " + Store.time(now), Toast.LENGTH_SHORT).show();
            refreshAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            mgr.updateAppWidget(id, build(context));
        }
    }

    static void refreshAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, StampWidget.class));
        for (int id : ids) {
            mgr.updateAppWidget(id, build(context));
        }
    }

    private static RemoteViews build(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        List<Long> taps = Store.all(context);
        if (taps.isEmpty()) {
            rv.setTextViewText(R.id.label, "TAP TO LOG");
            rv.setTextViewText(R.id.time, "--:--");
            rv.setTextViewText(R.id.sub, "No feeds yet");
        } else {
            long last = taps.get(taps.size() - 1);
            int today = Store.todayCount(taps);
            rv.setTextViewText(R.id.label, "LAST FEED");
            rv.setTextViewText(R.id.time, Store.time(last));
            rv.setTextViewText(R.id.sub, today == 0
                    ? Store.date(last)
                    : today + (today == 1 ? " feed today" : " feeds today"));
        }

        Intent i = new Intent(context, StampWidget.class).setAction(ACTION_STAMP);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.root, pi);
        return rv;
    }
}
