package com.ripti.cluerush.game;

import android.content.Context;
import android.content.SharedPreferences;

import com.ripti.cluerush.model.Difficulty;

public final class BestScoreStore {

    private static final String PREFS = "cluerush_best_scores";

    private BestScoreStore() {
    }

    public static int get(Context context, String arenaId, Difficulty difficulty) {
        return prefs(context).getInt(key(arenaId, difficulty), 0);
    }

    public static boolean submit(Context context, String arenaId, Difficulty difficulty,
                                 int score) {
        if (score <= get(context, arenaId, difficulty)) {
            return false;
        }
        prefs(context).edit().putInt(key(arenaId, difficulty), score).apply();
        return true;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String arenaId, Difficulty difficulty) {
        return arenaId + "_" + difficulty.getKey();
    }
}