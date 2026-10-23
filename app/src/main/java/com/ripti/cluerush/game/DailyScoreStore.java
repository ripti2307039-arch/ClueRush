package com.ripti.cluerush.game;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public final class DailyScoreStore {

    private static final String PREFS = "cluerush_daily_scores";

    private DailyScoreStore() {
    }


    @Nullable
    public static Integer get(Context context, String date) {
        SharedPreferences prefs = prefs(context);
        String key = key(date);
        return prefs.contains(key) ? prefs.getInt(key, 0) : null;
    }


    public static boolean submit(Context context, String date, int score) {
        Integer previous = get(context, date);
        if (previous != null && score <= previous) {
            return false;
        }
        prefs(context).edit().putInt(key(date), score).apply();
        return true;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String date) {
        return "score_" + date;
    }
}