package com.ripti.cluerush.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.Difficulty;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DailyRepository {

    public interface Callback {
        void onChallenge(DailyChallenge challenge);
    }

    private static final String TAG = "DailyRepository";


    private static final String DAILY_URL =
            "https://raw.githubusercontent.com/ripti2307039-arch/ClueRush/main/daily.json";
    private static final int TIMEOUT_MS = 6000;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());


    public void load(List<Arena> arenas, Callback callback) {
        final List<Arena> snapshot = new ArrayList<>(arenas);
        IO.execute(() -> {
            String today = DailyChallenge.todayKey();
            DailyChallenge result = null;
            try {
                result = parse(httpGet(DAILY_URL), today, snapshot);
            } catch (IOException | JSONException e) {
                Log.w(TAG, "Daily JSON unavailable, generating locally: " + e);
            }
            if (result == null) {
                result = DailyChallenge.generate(snapshot, today);
            }
            final DailyChallenge finalResult = result;
            mainHandler.post(() -> callback.onChallenge(finalResult));
        });
    }


    private static DailyChallenge parse(String json, String today, List<Arena> arenas)
            throws JSONException {
        JSONObject challenges = new JSONObject(json).optJSONObject("challenges");
        if (challenges == null) {
            return null;
        }
        JSONObject entry = challenges.optJSONObject(today);
        if (entry == null) {
            return null;
        }
        String arenaId = entry.getString("arena");
        Difficulty difficulty = Difficulty.fromName(entry.getString("difficulty"));
        long seed = entry.getLong("seed");
        String title = entry.optString("title", "");

        boolean known = false;
        for (Arena arena : arenas) {
            if (arena.getId().equals(arenaId)) {
                known = true;
                break;
            }
        }
        if (!known || difficulty == null) {
            return null;
        }
        return new DailyChallenge(today, arenaId, difficulty, seed, title, true);
    }

    private static String httpGet(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                return body.toString();
            }
        } finally {
            connection.disconnect();
        }
    }
}