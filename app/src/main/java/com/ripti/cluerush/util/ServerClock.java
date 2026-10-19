package com.ripti.cluerush.util;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public final class ServerClock {

    private static volatile long offsetMs;
    private static boolean started;

    private ServerClock() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object value = snapshot.getValue();
                        if (value instanceof Number) {
                            offsetMs = ((Number) value).longValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    public static long offset() {
        return offsetMs;
    }

    public static long now() {
        return System.currentTimeMillis() + offsetMs;
    }

    public static long toLocal(long serverMs) {
        return serverMs - offsetMs;
    }

    public static long fromLocal(long localMs) {
        return localMs + offsetMs;
    }
}