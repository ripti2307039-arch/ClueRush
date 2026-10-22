package com.ripti.cluerush.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ripti.cluerush.MainActivity;
import com.ripti.cluerush.R;
import com.ripti.cluerush.ui.game.GameActivity;

public final class NotificationHelper {

    public static final String CHANNEL_MATCH = "match_progress";
    public static final String CHANNEL_RESULT = "match_result";
    public static final int ID_MATCH = 1001;
    public static final int ID_RESULT = 1002;

    private NotificationHelper() {
    }

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel progress = new NotificationChannel(CHANNEL_MATCH,
                context.getString(R.string.channel_match_name),
                NotificationManager.IMPORTANCE_LOW);
        progress.setDescription(context.getString(R.string.channel_match_desc));
        NotificationChannel result = new NotificationChannel(CHANNEL_RESULT,
                context.getString(R.string.channel_result_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        result.setDescription(context.getString(R.string.channel_result_desc));
        manager.createNotificationChannel(progress);
        manager.createNotificationChannel(result);
    }

    public static Notification buildOngoing(Context context, String arenaName, long endAtMillis) {
        // Running GameActivity ke front e ano, notun kore create na kore
        Intent open = new Intent(context, GameActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MATCH)
                .setSmallIcon(R.drawable.ic_action_trophy)
                .setContentTitle(context.getString(R.string.notif_match_title))
                .setContentText(context.getString(R.string.notif_match_text, arenaName))
                .setContentIntent(pending)
                .addAction(0, context.getString(R.string.notif_action_open), pending)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);

        if (endAtMillis > System.currentTimeMillis()) {
            // System nijei countdown dekhay, tai ticking er jonno alada thread lage na
            builder.setWhen(endAtMillis)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true);
        }
        return builder.build();
    }

    public static void showResult(Context context, String title, String text) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            return;
        }
        createChannels(context);
        Intent home = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 1, home,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_RESULT)
                .setSmallIcon(R.drawable.ic_action_trophy)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        manager.notify(ID_RESULT, notification);
    }
}