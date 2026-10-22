package com.ripti.cluerush.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import androidx.core.content.ContextCompat;

import com.ripti.cluerush.util.NetworkUtils;

public class MatchStatusReceiver extends BroadcastReceiver {

    public interface Listener {
        void onNetworkChanged(boolean online);

        void onBatteryLow();
    }

    private final Listener listener;
    private boolean lastOnline;
    private boolean registered;

    public MatchStatusReceiver(Context context, Listener listener) {
        this.listener = listener;
        this.lastOnline = NetworkUtils.isOnline(context);
    }

    @SuppressWarnings("deprecation")
    public void register(Context context) {
        if (registered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        // Shudhu app er vitorer jonno, onno app dhorte parbe na
        ContextCompat.registerReceiver(context, this, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        lastOnline = NetworkUtils.isOnline(context);
        registered = true;
    }

    public void unregister(Context context) {
        if (!registered) {
            return;
        }
        context.unregisterReceiver(this);
        registered = false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            boolean online = NetworkUtils.isOnline(context);
            // Ek ei state e bar bar broadcast ashle ignore kore
            if (online != lastOnline) {
                lastOnline = online;
                listener.onNetworkChanged(online);
            }
        } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
            listener.onBatteryLow();
        }
    }
}