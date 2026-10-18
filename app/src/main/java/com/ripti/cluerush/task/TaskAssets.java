package com.ripti.cluerush.task;

import android.content.Context;
import android.util.Log;

import com.ripti.cluerush.model.Difficulty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class TaskAssets {

    private static final String TAG = "TaskAssets";
    private static final Map<String, List<LevelRow>> CACHE = new HashMap<>();

    static final class LevelRow {
        final Difficulty level;
        final String[] columns;

        LevelRow(Difficulty level, String[] columns) {
            this.level = level;
            this.columns = columns;
        }
    }

    private TaskAssets() {
    }


    static synchronized List<LevelRow> load(Context context, String path, int minColumns) {
        List<LevelRow> cached = CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        List<LevelRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(path), StandardCharsets.UTF_8))) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\|");
                Difficulty level = Difficulty.fromName(parts[0]);
                if (level == null || parts.length < minColumns + 1) {
                    Log.w(TAG, path + " line " + number + " skipped: bad format");
                    continue;
                }
                String[] columns = new String[parts.length - 1];
                for (int i = 1; i < parts.length; i++) {
                    columns[i - 1] = parts[i].trim();
                }
                rows.add(new LevelRow(level, columns));
            }
        } catch (IOException e) {
            Log.e(TAG, path + " could not be read", e);
        }
        List<LevelRow> result = Collections.unmodifiableList(rows);
        CACHE.put(path, result);
        return result;
    }


    static List<LevelRow> pick(List<LevelRow> all, Difficulty level, int count, Random random) {
        List<LevelRow> exact = new ArrayList<>();
        List<LevelRow> others = new ArrayList<>();
        for (LevelRow row : all) {
            (row.level == level ? exact : others).add(row);
        }
        Collections.shuffle(exact, random);
        Collections.shuffle(others, random);
        final int target = level.ordinal();
        Collections.sort(others, (a, b) ->
                Math.abs(a.level.ordinal() - target) - Math.abs(b.level.ordinal() - target));

        List<LevelRow> result = new ArrayList<>(exact);
        result.addAll(others);
        return new ArrayList<>(result.subList(0, Math.min(count, result.size())));
    }
}