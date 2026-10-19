package com.ripti.cluerush.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ripti.cluerush.model.PlayerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchLiveRepository {

    public interface Listener {
        void onPlayers(List<PlayerState> players);

        void onRemoved();
    }

    public static class Handle {

        private final DatabaseReference reference;
        private final ValueEventListener listener;

        Handle(DatabaseReference reference, ValueEventListener listener) {
            this.reference = reference;
            this.listener = listener;
        }

        public void remove() {
            reference.removeEventListener(listener);
        }
    }

    public Handle observe(String code, Listener listener) {
        DatabaseReference reference = matches().child(code);
        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    listener.onRemoved();
                    return;
                }
                List<PlayerState> players = new ArrayList<>();
                for (DataSnapshot child : snapshot.child("players").getChildren()) {
                    PlayerState state = parsePlayer(child);
                    if (state != null) {
                        players.add(state);
                    }
                }
                listener.onPlayers(players);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onRemoved();
            }
        };
        reference.addValueEventListener(valueListener);
        return new Handle(reference, valueListener);
    }

    public Handle armPresence(String code, String uid, boolean host) {
        DatabaseReference match = matches().child(code);
        DatabaseReference player = match.child("players").child(uid);
        try {
            if (host) {
                match.onDisconnect().cancel();
            }
            player.onDisconnect().cancel();
        } catch (Exception ignored) {
        }
        DatabaseReference info = FirebaseDatabase.getInstance().getReference(".info/connected");
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    try {
                        DatabaseReference connected = player.child("connected");
                        connected.onDisconnect().setValue(false);
                        connected.setValue(true);
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        info.addValueEventListener(listener);
        return new Handle(info, listener);
    }

    public void update(String code, String uid, Map<String, Object> values) {
        try {
            matches().child(code).child("players").child(uid).updateChildren(values);
        } catch (Exception ignored) {
        }
    }

    public void deleteMatch(String code) {
        try {
            matches().child(code).removeValue();
        } catch (Exception ignored) {
        }
    }

    private DatabaseReference matches() {
        return FirebaseDatabase.getInstance().getReference("matches");
    }

    private PlayerState parsePlayer(DataSnapshot child) {
        String uid = child.getKey();
        if (uid == null) {
            return null;
        }
        Object lat = child.child("lat").getValue();
        Object lng = child.child("lng").getValue();
        boolean hasPosition = lat instanceof Number && lng instanceof Number;
        Object connectedValue = child.child("connected").getValue();
        boolean connected = !(connectedValue instanceof Boolean) || (Boolean) connectedValue;
        return new PlayerState(uid,
                readString(child, "name", "Player"),
                (int) readLong(child, "rounds", 0L),
                readString(child, "phase", PlayerState.PHASE_EXPLORING),
                (int) readLong(child, "score", 0L),
                (int) readLong(child, "solved", 0L),
                (int) readLong(child, "wrong", 0L),
                (int) readLong(child, "finalScore", 0L),
                readLong(child, "finishedAt", 0L),
                hasPosition ? ((Number) lat).doubleValue() : 0.0,
                hasPosition ? ((Number) lng).doubleValue() : 0.0,
                hasPosition,
                connected,
                readString(child, "forfeitedUid", ""),
                readString(child, "roundPoints", ""));
    }

    private String readString(DataSnapshot snapshot, String key, String fallback) {
        Object value = snapshot.child(key).getValue();
        return value instanceof String ? (String) value : fallback;
    }

    private long readLong(DataSnapshot snapshot, String key, long fallback) {
        Object value = snapshot.child(key).getValue();
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}