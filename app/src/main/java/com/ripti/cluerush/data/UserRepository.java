package com.ripti.cluerush.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ripti.cluerush.model.UserProfile;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

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

    /** Keeps the saved name and the leaderboard name in step with the new display name. */
    public void updateName(String uid, String name) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.getReference("users").child(uid).child("name").setValue(name);

            DatabaseReference entry = database.getReference("leaderboard").child(uid);
            entry.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Only rename players who are already on the leaderboard.
                    if (snapshot.exists()) {
                        entry.child("name").setValue(name);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        } catch (Exception ignored) {
            // The Auth display name is already updated; this only mirrors it.
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