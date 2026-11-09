package com.ripti.cluerush.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.Difficulty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Daily challenge from Firestore.
 *
 * Collection: dailyChallenges, document id = date (yyyy-MM-dd, Asia/Dhaka)
 * Fields: arena (string), difficulty (string), seed (number), title (string, optional)
 *
 * If the document is missing, invalid, or Firestore is unreachable, the challenge is
 * generated locally from the date, so every phone still gets the same one.
 */
public class DailyRepository {

    public interface Callback {
        void onChallenge(DailyChallenge challenge);
    }

    private static final String TAG = "DailyRepository";
    private static final String COLLECTION = "dailyChallenges";
    private static final long TIMEOUT_MS = 6000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void load(List<Arena> arenas, Callback callback) {
        final List<Arena> snapshot = new ArrayList<>(arenas);
        final String today = DailyChallenge.todayKey();
        final AtomicBoolean done = new AtomicBoolean(false);

        final Runnable timeout = () -> {
            if (done.compareAndSet(false, true)) {
                Log.w(TAG, "Firestore timed out, generating locally");
                callback.onChallenge(DailyChallenge.generate(snapshot, today));
            }
        };
        mainHandler.postDelayed(timeout, TIMEOUT_MS);

        try {
            FirebaseFirestore.getInstance()
                    .collection(COLLECTION)
                    .document(today)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!done.compareAndSet(false, true)) {
                            return;
                        }
                        mainHandler.removeCallbacks(timeout);
                        DailyChallenge result = null;
                        if (task.isSuccessful()) {
                            result = parse(task.getResult(), today, snapshot);
                        } else {
                            Log.w(TAG, "Daily doc unavailable, generating locally: "
                                    + task.getException());
                        }
                        if (result == null) {
                            result = DailyChallenge.generate(snapshot, today);
                        }
                        callback.onChallenge(result);
                    });
        } catch (RuntimeException e) {
            if (done.compareAndSet(false, true)) {
                mainHandler.removeCallbacks(timeout);
                Log.w(TAG, "Firestore init failed, generating locally: " + e);
                callback.onChallenge(DailyChallenge.generate(snapshot, today));
            }
        }
    }

    private static DailyChallenge parse(DocumentSnapshot doc, String today,
                                        List<Arena> arenas) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        String arenaId = doc.getString("arena");
        String difficultyName = doc.getString("difficulty");
        Object rawSeed = doc.get("seed");
        Long seed = null;
        if (rawSeed instanceof Number) {
            seed = ((Number) rawSeed).longValue();
        } else if (rawSeed instanceof String) {
            try {
                seed = Long.parseLong(((String) rawSeed).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        String title = doc.getString("title");
        if (arenaId == null || difficultyName == null || seed == null) {
            return null;
        }git
        Difficulty difficulty = Difficulty.fromName(difficultyName);

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
}