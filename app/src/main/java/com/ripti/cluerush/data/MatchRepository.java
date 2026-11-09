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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MatchRepository {

    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final int TEAM_SLOTS = 2;

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

    public interface SwitchCallback {
        void onResult(boolean success);
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

    private static class PlayerRow {
        final String uid;
        final String name;
        final boolean ready;
        final boolean host;
        final long joinedAt;
        final String team;
        final long teamSince;

        PlayerRow(String uid, String name, boolean ready, boolean host,
                  long joinedAt, String team, long teamSince) {
            this.teamSince = teamSince;
            this.uid = uid;
            this.name = name;
            this.ready = ready;
            this.host = host;
            this.joinedAt = joinedAt;
            this.team = team;
        }
    }

    public void createMatch(String uid, String name, String arenaId,
                            String difficulty, CreateCallback callback) {
        try {
            attemptCreate(uid, name, arenaId, difficulty,
                    Match.MODE_CLASSIC, callback, 0);
        } catch (Exception e) {
            callback.onError(R.string.error_generic);
        }
    }

    public void createTeamCaptureMatch(String uid, String name, String arenaId,
                                       String difficulty, CreateCallback callback) {
        try {
            attemptCreate(uid, name, arenaId, difficulty,
                    Match.MODE_TEAM_CAPTURE, callback, 0);
        } catch (Exception e) {
            callback.onError(R.string.error_generic);
        }
    }

    private void attemptCreate(final String uid,
                               final String name,
                               final String arenaId,
                               final String difficulty,
                               final String gameMode,
                               final CreateCallback callback,
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
                    attemptCreate(uid, name, arenaId, difficulty,
                            gameMode, callback, attempt + 1);
                    return;
                }

                boolean teamMode = Match.MODE_TEAM_CAPTURE.equals(gameMode);

                Map<String, Object> players = new HashMap<>();
                players.put(uid, teamMode
                        ? teamPlayerData(name, LobbyPlayer.TEAM_A)
                        : playerData(name));

                Map<String, Object> data = new HashMap<>();

                data.put("arenaId", arenaId);
                data.put("hostUid", uid);
                data.put("status", Match.STATUS_WAITING);
                data.put("difficulty", difficulty);
                data.put("durationSeconds", Match.durationFor(difficulty));
                data.put("seed", new Random().nextLong());
                data.put("createdAt", ServerValue.TIMESTAMP);
                data.put("players", players);
                data.put("gameMode", gameMode);
                data.put("maxPlayers", teamMode
                        ? Match.TEAM_CAPTURE_MAX_PLAYERS
                        : Match.DEFAULT_MAX_PLAYERS);

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

    public void joinMatch(String uid, String name, String code,
                          JoinCallback callback) {
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

                    if (match.isTeamCaptureMode()) {
                        joinTeamCaptureMatch(reference, snapshot, match,
                                uid, name, callback);
                        return;
                    }

                    boolean alreadyIn = snapshot.child("players").hasChild(uid);

                    if (!alreadyIn) {

                        if (!Match.STATUS_WAITING.equals(match.getStatus())) {
                            callback.onError(R.string.error_match_started);
                            return;
                        }

                        if (match.getPlayers().size() >= match.getMaxPlayers()) {
                            callback.onError(R.string.error_match_full);
                            return;
                        }
                    }

                    DatabaseReference playerReference =
                            reference.child("players").child(uid);

                    playerReference.setValue(playerData(name))
                            .addOnCompleteListener(task -> {

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

    /**
     * 2v2 join: writes only this player's own node (allowed by the rules).
     * New players fill slots serially: Team A (2) first, then Team B (2).
     * They can switch teams in the lobby before the match starts.
     */
    private void joinTeamCaptureMatch(DatabaseReference reference,
                                      DataSnapshot snapshot,
                                      Match match,
                                      String uid,
                                      String name,
                                      JoinCallback callback) {

        if (snapshot.child("players").hasChild(uid)) {
            callback.onJoined();
            return;
        }

        if (!Match.STATUS_WAITING.equals(match.getStatus())) {
            callback.onError(R.string.error_match_started);
            return;
        }

        if (match.getPlayers().size() >= match.getMaxPlayers()) {
            callback.onError(R.string.error_match_full);
            return;
        }

        int teamACount = 0;
        for (LobbyPlayer player : match.getPlayers()) {
            if (LobbyPlayer.TEAM_A.equals(player.getTeam())) {
                teamACount++;
            }
        }

        String team = teamACount < TEAM_SLOTS
                ? LobbyPlayer.TEAM_A
                : LobbyPlayer.TEAM_B;

        DatabaseReference playerReference = reference.child("players").child(uid);

        playerReference.setValue(teamPlayerData(name, team))
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        playerReference.onDisconnect().removeValue();
                        verifySeat(reference, uid, callback);
                    } else {
                        diagnoseJoinFailure(reference, callback);
                    }
                });
    }

    /**
     * Rules cannot count children, so after joining we re-check our seat:
     * players are ordered by host first, then server join time. If we land
     * beyond maxPlayers we remove ourselves and report "match full".
     */
    private void verifySeat(DatabaseReference reference,
                            String uid,
                            JoinCallback callback) {

        reference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String hostUid = readString(snapshot, "hostUid", "");
                int max = (int) readLong(snapshot, "maxPlayers",
                        Match.TEAM_CAPTURE_MAX_PLAYERS);

                List<PlayerRow> rows = readRows(snapshot, hostUid);
                sortTeamRows(rows);

                int index = -1;
                for (int i = 0; i < rows.size(); i++) {
                    if (rows.get(i).uid.equals(uid)) {
                        index = i;
                        break;
                    }
                }

                if (index >= max) {
                    DatabaseReference mine = reference.child("players").child(uid);
                    mine.onDisconnect().cancel();
                    mine.removeValue().addOnCompleteListener(
                            t -> callback.onError(R.string.error_match_full));
                    return;
                }

                callback.onJoined();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(errorMessage(error));
            }
        });
    }

    private void diagnoseJoinFailure(DatabaseReference reference,
                                     JoinCallback callback) {

        reference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    callback.onError(R.string.error_match_not_found);
                    return;
                }

                String status = readString(snapshot, "status", Match.STATUS_WAITING);
                int max = (int) readLong(snapshot, "maxPlayers",
                        Match.TEAM_CAPTURE_MAX_PLAYERS);
                long count = snapshot.child("players").getChildrenCount();

                if (!Match.STATUS_WAITING.equals(status)) {
                    callback.onError(R.string.error_match_started);
                } else if (count >= max) {
                    callback.onError(R.string.error_match_full);
                } else {
                    callback.onError(R.string.error_generic);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(errorMessage(error));
            }
        });
    }

    /**
     * 2v2 lobby: the player moves to the other team. Always sets ready=false
     * so a team change must be re-confirmed. The rules reject this once the
     * match has left the "waiting" status.
     */
    public void switchTeam(String code, String uid, String team,
                           SwitchCallback callback) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("team", team);
            updates.put("teamAt", ServerValue.TIMESTAMP);
            updates.put("ready", false);

            matches()
                    .child(code)
                    .child("players")
                    .child(uid)
                    .updateChildren(updates)
                    .addOnCompleteListener(task -> {
                        if (callback != null) {
                            callback.onResult(task.isSuccessful());
                        }
                    });

        } catch (Exception e) {
            if (callback != null) {
                callback.onResult(false);
            }
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
            matches()
                    .child(code)
                    .child("players")
                    .child(uid)
                    .child("ready")
                    .setValue(ready);

        } catch (Exception ignored) {
        }
    }

    public void setStatus(String code, String status) {
        try {
            matches()
                    .child(code)
                    .child("status")
                    .setValue(status);

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

                DatabaseReference playerReference =
                        reference.child("players").child(uid);

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

    private Map<String, Object> teamPlayerData(String name, String team) {

        Map<String, Object> player = playerData(name);
        player.put("team", team);
        player.put("joinedAt", ServerValue.TIMESTAMP);

        return player;
    }

    private List<PlayerRow> readRows(DataSnapshot snapshot, String hostUid) {

        List<PlayerRow> rows = new ArrayList<>();

        for (DataSnapshot child : snapshot.child("players").getChildren()) {

            String uid = child.getKey();

            if (uid == null) {
                continue;
            }

            Object readyValue = child.child("ready").getValue();

            boolean ready = readyValue instanceof Boolean
                    && (Boolean) readyValue;

            rows.add(new PlayerRow(
                    uid,
                    readString(child, "name", "Player"),
                    ready,
                    uid.equals(hostUid),
                    readLong(child, "joinedAt", Long.MAX_VALUE),
                    readString(child, "team", null),
                    readLong(child, "teamAt", readLong(child, "joinedAt", Long.MAX_VALUE))
            ));
        }

        return rows;
    }

    private void sortTeamRows(List<PlayerRow> rows) {

        Collections.sort(rows, (a, b) -> {
            if (a.host != b.host) {
                return a.host ? -1 : 1;
            }
            int byTime = Long.compare(a.joinedAt, b.joinedAt);
            if (byTime != 0) {
                return byTime;
            }
            return a.uid.compareTo(b.uid);
        });
    }

    private Match parse(String code, DataSnapshot snapshot) {

        String hostUid = readString(snapshot, "hostUid", "");
        String gameMode = readString(snapshot, "gameMode", Match.MODE_CLASSIC);
        boolean teamMode = Match.MODE_TEAM_CAPTURE.equals(gameMode);
        int maxPlayers = (int) readLong(snapshot, "maxPlayers",
                Match.DEFAULT_MAX_PLAYERS);

        List<PlayerRow> rows = readRows(snapshot, hostUid);
        List<LobbyPlayer> players = new ArrayList<>();

        if (teamMode) {

            sortTeamRows(rows);

            int shown = Math.min(rows.size(), Math.max(1, maxPlayers));

            for (int i = 0; i < shown; i++) {
                PlayerRow row = rows.get(i);

                String team = row.team != null
                        ? row.team
                        : (i < TEAM_SLOTS ? LobbyPlayer.TEAM_A : LobbyPlayer.TEAM_B);

                players.add(new LobbyPlayer(
                        row.uid, row.name, row.ready, row.host, team, row.teamSince));
            }

        } else {

            for (PlayerRow row : rows) {
                LobbyPlayer player = new LobbyPlayer(
                        row.uid, row.name, row.ready, row.host, null);

                if (player.isHost()) {
                    players.add(0, player);
                } else {
                    players.add(player);
                }
            }
        }

        return new Match(
                code,
                readString(snapshot, "arenaId", ""),
                hostUid,
                readString(snapshot, "status", Match.STATUS_WAITING),
                readString(snapshot, "difficulty", Match.DIFFICULTY_EASY),
                maxPlayers,
                (int) readLong(snapshot, "durationSeconds", Match.DEFAULT_DURATION_SECONDS),
                readLong(snapshot, "seed", 0L),
                readLong(snapshot, "startAt", 0L),
                players,
                gameMode
        );
    }

    private String readString(DataSnapshot snapshot,
                              String key,
                              String fallback) {

        Object value = snapshot.child(key).getValue();

        return value instanceof String
                ? (String) value
                : fallback;
    }

    private long readLong(DataSnapshot snapshot,
                          String key,
                          long fallback) {

        Object value = snapshot.child(key).getValue();

        return value instanceof Number
                ? ((Number) value).longValue()
                : fallback;
    }

    @StringRes
    private int errorMessage(DatabaseError error) {

        int code = error.getCode();

        if (code == DatabaseError.NETWORK_ERROR
                || code == DatabaseError.DISCONNECTED) {

            return R.string.error_no_internet;
        }

        return R.string.error_generic;
    }
}