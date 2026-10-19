package com.ripti.cluerush.game;

import android.content.Context;
import android.util.Log;

import com.ripti.cluerush.model.CampusLocation;
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

public final class ClueBank {

    private static final String TAG = "ClueBank";
    private static final Map<String, Map<String, List<String>>> CACHE = new HashMap<>();

    private ClueBank() {
    }

    public static String pick(Context context, String arenaId, CampusLocation location,
                              Difficulty difficulty, Random random, boolean avoidRepeats) {
        List<String> all = load(context, arenaId).get(bucket(location.getId(), difficulty));
        if (all == null || all.isEmpty()) {
            return location.pickClue(random, difficulty);
        }

        String key = arenaId + "_" + location.getId() + "_" + difficulty.getKey();
        Set<String> seen = avoidRepeats ? ClueStore.get(context, key) : new HashSet<String>();

        List<String> fresh = new ArrayList<>();
        for (String clue : all) {
            if (!seen.contains(idOf(clue))) {
                fresh.add(clue);
            }
        }
        if (fresh.isEmpty()) {
            seen.clear();
            fresh.addAll(all);
        }

        String chosen = fresh.get(random.nextInt(fresh.size()));
        if (avoidRepeats) {
            seen.add(idOf(chosen));
            ClueStore.save(context, key, seen);
        }
        return chosen;
    }

    private static String idOf(String clue) {
        return Integer.toHexString(clue.hashCode());
    }

    private static String bucket(String locationId, Difficulty difficulty) {
        return locationId + "|" + difficulty.getKey();
    }

    private static synchronized Map<String, List<String>> load(Context context, String arenaId) {
        Map<String, List<String>> cached = CACHE.get(arenaId);
        if (cached != null) {
            return cached;
        }
        Map<String, List<String>> bank = new HashMap<>();
        String path = "clues/" + arenaId + ".txt";
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
                String[] parts = trimmed.split("\\|", 3);
                Difficulty level = parts.length == 3 ? Difficulty.fromName(parts[1]) : null;
                if (level == null || parts[0].trim().isEmpty() || parts[2].trim().isEmpty()) {
                    Log.w(TAG, path + " line " + number + " skipped: bad format");
                    continue;
                }
                String key = bucket(parts[0].trim(), level);
                List<String> list = bank.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                    bank.put(key, list);
                }
                list.add(parts[2].trim());
            }
        } catch (IOException e) {
            Log.i(TAG, path + " not found, using clues from the arena XML");
        }
        Map<String, List<String>> result = Collections.unmodifiableMap(bank);
        CACHE.put(arenaId, result);
        return result;
    }
}