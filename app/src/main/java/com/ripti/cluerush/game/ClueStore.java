package com.ripti.cluerush.game;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

final class ClueStore {

    private static final String PREFS = "cluerush_seen_clues";

    private ClueStore() {
    }

    static Set<String> get(Context context, String key) {
        Set<String> stored = prefs(context).getStringSet(key, null);
        return stored == null ? new HashSet<String>() : new HashSet<>(stored);
    }

    static void save(Context context, String key, Set<String> ids) {
        prefs(context).edit().putStringSet(key, new HashSet<>(ids)).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}