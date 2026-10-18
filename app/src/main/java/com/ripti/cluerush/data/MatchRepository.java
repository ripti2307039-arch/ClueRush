package com.ripti.cluerush.data;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.ripti.cluerush.R;
import com.ripti.cluerush.model.LobbyPlayer;
import com.ripti.cluerush.model.Match;
import com.ripti.cluerush.util.MatchCodeGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MatchRepository {

    private static final int MAX_CODE_ATTEMPTS = 5;

    public interface CreateCallback {
        void onCreated(String code);

        void onError(@StringRes int messageRes);
    }

    public interface JoinCallback {
        void onJoined();

        void onError(@StringRes int messageRes);
    }

    public interface MatchListener {
        void onMatchChanged(Match match);

        void onMatchRemoved();
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

    public void createMatch(String uid, String name, String arenaId, String difficulty,
                            CreateCallback callback) {
        try {
            attemptCreate(uid, name, arenaId, difficulty, callback, 0);
        } catch (Exception e) {
            callback.onError(R.string.error_generic);
        }
    }

    private void attemptCreate(final String uid, final String name, final String arenaId,
                               final String difficulty, final CreateCallback callback,
                               final int attempt) {
        if (attempt >= MAX_CODE_ATTEMPTS) {
            callback.onError(R.string.error_generic);
            return;
        }
        final String code = MatchCodeGenerator.generate();
        final DatabaseReference reference = matches().child(code);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    attemptCreate(uid, name, arenaId, difficulty, callback, attempt + 1);
                    return;
                }
                Map<String, Object> players = new HashMap<>();
                players.put(uid, playerData(name));

                Map<String, Object> data = new HashMap<>();
                data.put("arenaId", arenaId);
                data.put("hostUid", uid);
                data.put("status", Match.STATUS_WAITING);
                data.put("difficulty", difficulty);
                data.put("durationSeconds", Match.durationFor(difficulty));
                data.put("seed", new Random().nextLong());
                data.put("maxPlayers", Match.DEFAULT_MAX_PLAYERS);
                data.put("createdAt", ServerValue.TIMESTAMP);
                data.put("players", players);

                reference.setValue(data).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reference.onDisconnect().removeValue();
                        callback.onCreated(code);
                    } else {
                        callback.onError(R.string.error_generic);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(errorMessage(error));
            }
        });
    }

    public void joinMatch(String uid, String name, String code, JoinCallback callback) {
        try {
            DatabaseReference reference = matches().child(code);
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        callback.onError(R.string.error_match_not_found);
                        return;
                    }
                    Match match = parse(code, snapshot);
                    boolean alreadyIn = snapshot.child("players").hasChild(uid);
                    if (!alreadyIn) {
                        if (match.getPlayers().size() >= match.getMaxPlayers()) {
                            callback.onError(R.string.error_match_full);
                            return;
                        }
                        if (!Match.STATUS_WAITING.equals(match.getStatus())) {
                            callback.onError(R.string.error_match_started);
                            return;
                        }
                    }
                    DatabaseReference playerReference = reference.child("players").child(uid);
                    playerReference.setValue(playerData(name)).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            playerReference.onDisconnect().removeValue();
                            callback.onJoined();
                        } else {
                            callback.onError(R.string.error_generic);
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.onError(errorMessage(error));
                }
            });
        } catch (Exception e) {
            callback.onError(R.string.error_generic);
        }
    }

    public ListenerHandle observeMatch(String code, MatchListener listener) {
        DatabaseReference reference = matches().child(code);
        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    listener.onMatchRemoved();
                } else {
                    listener.onMatchChanged(parse(code, snapshot));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onMatchRemoved();
            }
        };
        reference.addValueEventListener(valueListener);
        return new ListenerHandle(reference, valueListener);
    }

    public void setReady(String code, String uid, boolean ready) {
        try {
            matches().child(code).child("players").child(uid).child("ready").setValue(ready);
        } catch (Exception ignored) {
        }
    }

    public void setStatus(String code, String status) {
        try {
            matches().child(code).child("status").setValue(status);
        } catch (Exception ignored) {
        }
    }

    public void startMatch(String code, long startAt) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Match.STATUS_STARTING);
            updates.put("startAt", startAt);
            matches().child(code).updateChildren(updates);
        } catch (Exception ignored) {
        }
    }

    public void leaveMatch(String code, String uid, boolean isHost) {
        try {
            DatabaseReference reference = matches().child(code);
            if (isHost) {
                reference.onDisconnect().cancel();
                reference.removeValue();
            } else {
                DatabaseReference playerReference = reference.child("players").child(uid);
                playerReference.onDisconnect().cancel();
                playerReference.removeValue();
            }
        } catch (Exception ignored) {
        }
    }

    private DatabaseReference matches() {
        return FirebaseDatabase.getInstance().getReference("matches");
    }

    private Map<String, Object> playerData(String name) {
        Map<String, Object> player = new HashMap<>();
        player.put("name", name);
        player.put("avatarId", 0);
        player.put("ready", false);
        player.put("connected", true);
        return player;
    }

    private Match parse(String code, DataSnapshot snapshot) {
        String hostUid = readString(snapshot, "hostUid", "");
        List<LobbyPlayer> players = new ArrayList<>();
        for (DataSnapshot child : snapshot.child("players").getChildren()) {
            String uid = child.getKey();
            if (uid == null) {
                continue;
            }
            Object readyValue = child.child("ready").getValue();
            boolean ready = readyValue instanceof Boolean && (Boolean) readyValue;
            LobbyPlayer player = new LobbyPlayer(uid, readString(child, "name", "Player"),
                    ready, uid.equals(hostUid));
            if (player.isHost()) {
                players.add(0, player);
            } else {
                players.add(player);
            }
        }
        return new Match(code,
                readString(snapshot, "arenaId", ""),
                hostUid,
                readString(snapshot, "status", Match.STATUS_WAITING),
                readString(snapshot, "difficulty", Match.DIFFICULTY_EASY),
                (int) readLong(snapshot, "maxPlayers", Match.DEFAULT_MAX_PLAYERS),
                (int) readLong(snapshot, "durationSeconds", Match.DEFAULT_DURATION_SECONDS),
                readLong(snapshot, "seed", 0L),
                readLong(snapshot, "startAt", 0L),
                players);
    }

    private String readString(DataSnapshot snapshot, String key, String fallback) {
        Object value = snapshot.child(key).getValue();
        return value instanceof String ? (String) value : fallback;
    }

    private long readLong(DataSnapshot snapshot, String key, long fallback) {
        Object value = snapshot.child(key).getValue();
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    @StringRes
    private int errorMessage(DatabaseError error) {
        int code = error.getCode();
        if (code == DatabaseError.NETWORK_ERROR || code == DatabaseError.DISCONNECTED) {
            return R.string.error_no_internet;
        }
        return R.string.error_generic;
    }
}