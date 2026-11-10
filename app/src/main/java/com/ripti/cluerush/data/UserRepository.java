package com.ripti.cluerush.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.UserProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class UserRepository {

    private static final String TAG = "UserRepository";

    public interface ProfileCallback {
        void onLoaded(UserProfile profile);
    }

    public void createProfile(String uid, String name, String email, Runnable onDone) {
        try {
            Map<String, Object> profile = new HashMap<>();
            profile.put("name", name);
            profile.put("email", email);
            profile.put("avatarId", 0);
            profile.put("xp", 0);
            profile.put("level", 1);
            profile.put("totalWins", 0);
            profile.put("matchesPlayed", 0);
            profile.put("draws", 0);
            profile.put("rating", 0);
            profile.put("soloGames", 0);

            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .setValue(profile)
                    .addOnCompleteListener(task -> onDone.run());
        } catch (Exception e) {
            onDone.run();
        }
    }

    /**
     * Keeps the saved name in step with the new display name everywhere other players can
     * see it: users/{uid}, leaderboard/{uid} and every scores/{arena_difficulty}/{uid} the
     * player has. onFail gets the Firebase error text (for example PERMISSION_DENIED) on
     * the main thread, so a rejected write is not silent any more.
     */
    public void updateName(Context context, String uid, String name,
                           Consumer<String> onFail) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.getReference("users").child(uid).child("name").setValue(name)
                    .addOnSuccessListener(v -> status(onFail, "Server saved your name: " + name))
                    .addOnFailureListener(e -> fail(onFail, "users/name: " + e.getMessage()));

            DatabaseReference entry = database.getReference("leaderboard").child(uid);
            entry.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Only rename players who are already on the leaderboard.
                    if (snapshot.exists()) {
                        entry.child("name").setValue(name).addOnFailureListener(e ->
                                fail(onFail, "leaderboard/name: " + e.getMessage()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    fail(onFail, "leaderboard read: " + error.getMessage());
                }
            });

            // Best-score boards keep their own copy of the name. Each board is checked on
            // its own path (no read of the whole scores node, which the rules may not allow).
            for (Arena arena : ArenaCatalog.getAll(context)) {
                for (Difficulty difficulty : Difficulty.values()) {
                    DatabaseReference mine = database.getReference("scores")
                            .child(StatsRepository.boardKey(arena.getId(), difficulty.getKey()))
                            .child(uid);
                    mine.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                mine.child("name").setValue(name).addOnFailureListener(e ->
                                        fail(onFail, "scores/name: " + e.getMessage()));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            fail(onFail, "scores read: " + error.getMessage());
                        }
                    });
                }
            }
        } catch (Exception e) {
            fail(onFail, "updateName: " + e);
        }
    }

    private static void fail(Consumer<String> onFail, String message) {
        Log.w(TAG, "sync failed: " + message);
        status(onFail, "Not synced to server: " + message);
    }

    private static void status(Consumer<String> onStatus, String message) {
        if (onStatus != null) {
            new Handler(Looper.getMainLooper()).post(() -> onStatus.accept(message));
        }
    }

    public void loadProfile(String uid, ProfileCallback callback) {
        try {
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            callback.onLoaded(parse(snapshot));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onLoaded(UserProfile.empty());
                        }
                    });
        } catch (Exception e) {
            callback.onLoaded(UserProfile.empty());
        }
    }

    private UserProfile parse(DataSnapshot snapshot) {
        return new UserProfile(
                readLong(snapshot, "xp", 0),
                readLong(snapshot, "totalWins", 0),
                readLong(snapshot, "matchesPlayed", 0),
                readLong(snapshot, "draws", 0),
                readLong(snapshot, "rating", 0),
                readLong(snapshot, "soloGames", 0));
    }

    private long readLong(DataSnapshot snapshot, String key, long fallback) {
        Object value = snapshot.child(key).getValue();
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}