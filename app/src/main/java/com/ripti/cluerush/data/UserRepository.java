package com.ripti.cluerush.data;

import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

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

            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .setValue(profile)
                    .addOnCompleteListener(task -> onDone.run());
        } catch (Exception e) {
            onDone.run();
        }
    }
}