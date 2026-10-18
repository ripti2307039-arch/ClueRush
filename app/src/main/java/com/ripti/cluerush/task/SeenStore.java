package com.ripti.cluerush.task;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

final class SeenStore {

    private static final String PREFS = "cluerush_seen_tasks";

    private SeenStore() {
    }

    static Set<String> get(Context context, String category) {
        Set<String> stored = prefs(context).getStringSet(category, null);
        return stored == null ? new HashSet<String>() : new HashSet<>(stored);
    }

    static void save(Context context, String category, Set<String> ids) {
        prefs(context).edit().putStringSet(category, new HashSet<>(ids)).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}