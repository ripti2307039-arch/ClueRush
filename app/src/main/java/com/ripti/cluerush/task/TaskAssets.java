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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

        String id() {
            return level.getKey() + ":" + Integer.toHexString(columns[0].hashCode());
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

    static List<LevelRow> pick(Context context, String category, List<LevelRow> all,
                               Difficulty level, int count, Random random,
                               boolean avoidRepeats) {
        Set<String> seen = avoidRepeats
                ? SeenStore.get(context, category) : new HashSet<String>();

        List<LevelRow> fresh = new ArrayList<>();
        List<LevelRow> used = new ArrayList<>();
        List<LevelRow> others = new ArrayList<>();
        for (LevelRow row : all) {
            if (row.level != level) {
                others.add(row);
            } else if (seen.contains(row.id())) {
                used.add(row);
            } else {
                fresh.add(row);
            }
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(used, random);
        Collections.shuffle(others, random);
        final int target = level.ordinal();
        Collections.sort(others, (a, b) ->
                Math.abs(a.level.ordinal() - target) - Math.abs(b.level.ordinal() - target));

        List<LevelRow> ordered = new ArrayList<>(fresh);
        ordered.addAll(used);
        ordered.addAll(others);
        List<LevelRow> chosen = new ArrayList<>(
                ordered.subList(0, Math.min(count, ordered.size())));

        if (avoidRepeats) {
            Set<String> updated = new HashSet<>(seen);
            if (fresh.size() < count) {
                for (LevelRow row : used) {
                    updated.remove(row.id());
                }
            }
            for (LevelRow row : chosen) {
                updated.add(row.id());
            }
            SeenStore.save(context, category, updated);
        }
        return chosen;
    }
}