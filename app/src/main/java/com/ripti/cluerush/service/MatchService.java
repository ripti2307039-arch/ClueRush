package com.ripti.cluerush.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.util.NotificationHelper;

public class MatchService extends Service {

    private static final String EXTRA_ARENA = "extra_arena_name";
    private static final String EXTRA_END_AT = "extra_end_at";

    public static void start(Context context, String arenaName, long endAtMillis) {
        Intent intent = new Intent(context, MatchService.class)
                .putExtra(EXTRA_ARENA, arenaName)
                .putExtra(EXTRA_END_AT, endAtMillis);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, MatchService.class));
    }

    /** Ongoing notification soriye result notification dekhay. */
    public static void finish(Context context, String title, String text) {
        stop(context);
        NotificationHelper.showResult(context, title, text);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String arena = intent.getStringExtra(EXTRA_ARENA);
        long endAt = intent.getLongExtra(EXTRA_END_AT, 0L);
        Notification notification = NotificationHelper.buildOngoing(
                this, arena == null ? "" : arena, endAt);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationHelper.ID_MATCH, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NotificationHelper.ID_MATCH, notification);
        }
        // Process mara gele match ar cholbe na, tai restart dorkar nai
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}