package com.tapstamp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;

import java.util.List;

/** Shows a normal "time for a feed" notification, and re-arms the reminder after a reboot. */
public class ReminderReceiver extends BroadcastReceiver {

    // New id so the sound setting below applies even if an older channel exists on the phone.
    private static final String CHANNEL = "feed_reminders_v2";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Reminder.reschedule(context);
            return;
        }
        if (!Reminder.enabled(context)) return;

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Default importance: the phone's standard notification sound, no full-screen popup.
        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "Feed reminders", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
        nm.createNotificationChannel(ch);

        List<Long> taps = Store.all(context);
        String text = taps.isEmpty()
                ? "Tap to open Feed Log"
                : "Last feed was at " + Store.time(taps.get(taps.size() - 1));

        PendingIntent open = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("Time for a feed")
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        nm.notify(1, n);
    }
}
