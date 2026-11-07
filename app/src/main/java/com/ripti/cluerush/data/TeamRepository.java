package com.ripti.cluerush.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Team-private data for the 2v2 capture mode:
 * matches/{code}/teams/{team}/claims/{uid} = locationId
 */
public class TeamRepository {

    public interface ClaimsListener {
        /** Map of uid to the locationId that player currently claims. */
        void onClaimsChanged(Map<String, String> claims);

        void onCancelled(DatabaseError error);
    }

    public static class ListenerHandle {

        private final DatabaseReference reference;
        private final ValueEventListener listener;

        ListenerHandle(DatabaseReference reference, ValueEventListener listener) {
            this.reference = reference;
            this.listener = listener;
        }

        public void remove() {
            reference.removeEventListener(listener);
        }
    }

    public ListenerHandle observeClaims(String code, String team,
                                        ClaimsListener listener) {

        DatabaseReference reference = claimsReference(code, team);

        ValueEventListener valueListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> claims = new HashMap<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    Object value = child.getValue();

                    if (uid != null && value instanceof String) {
                        claims.put(uid, (String) value);
                    }
                }

                listener.onClaimsChanged(claims);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onCancelled(error);
            }
        };

        reference.addValueEventListener(valueListener);

        return new ListenerHandle(reference, valueListener);
    }

    public void claim(String code, String team, String uid, String locationId) {
        if (isBlank(code) || isBlank(team) || isBlank(uid) || isBlank(locationId)) {
            return;
        }

        try {
            DatabaseReference reference = claimsReference(code, team).child(uid);
            reference.onDisconnect().removeValue();
            reference.setValue(locationId);
        } catch (Exception ignored) {
            // A failed claim only affects teammate hints, never the game itself.
        }
    }

    public void release(String code, String team, String uid) {
        if (isBlank(code) || isBlank(team) || isBlank(uid)) {
            return;
        }

        try {
            claimsReference(code, team).child(uid).removeValue();
        } catch (Exception ignored) {
            // Nothing to recover: the claim is cleared on disconnect as well.
        }
    }

    private DatabaseReference claimsReference(String code, String team) {
        return FirebaseDatabase.getInstance()
                .getReference("matches")
                .child(code)
                .child("teams")
                .child(team)
                .child("claims");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}