package com.ripti.cluerush.task;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class TaskHistory {

    private static final String PREFS = "cluerush_task_history";
    private static final String KEY_LAST = "last_game_types";

    private TaskHistory() {
    }

    public static List<TaskType> order(Context context, Random random, boolean avoidRecent) {
        List<TaskType> types = new ArrayList<>(java.util.Arrays.asList(TaskType.values()));
        Collections.shuffle(types, random);
        if (!avoidRecent) {
            return types;
        }
        Set<String> recent = prefs(context).getStringSet(KEY_LAST, new HashSet<String>());
        List<TaskType> fresh = new ArrayList<>();
        List<TaskType> stale = new ArrayList<>();
        for (TaskType type : types) {
            if (recent.contains(type.name())) {
                stale.add(type);
            } else {
                fresh.add(type);
            }
        }
        fresh.addAll(stale);
        return fresh;
    }

    public static void remember(Context context, List<TaskType> used) {
        Set<String> names = new HashSet<>();
        for (TaskType type : used) {
            names.add(type.name());
        }
        prefs(context).edit().putStringSet(KEY_LAST, names).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}