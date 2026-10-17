package com.ripti.cluerush.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Match {

    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_READY = "ready";
    public static final String STATUS_PLAYING = "playing";
    public static final String STATUS_FINISHED = "finished";

    public static final String DIFFICULTY_EASY = "easy";
    public static final String DIFFICULTY_HARD = "hard";

    public static final int DEFAULT_MAX_PLAYERS = 2;
    public static final int DEFAULT_DURATION_SECONDS = 600;

    private final String code;
    private final String arenaId;
    private final String hostUid;
    private final String status;
    private final String difficulty;
    private final int maxPlayers;
    private final int durationSeconds;
    private final List<LobbyPlayer> players;

    public Match(String code, String arenaId, String hostUid, String status, String difficulty,
                 int maxPlayers, int durationSeconds, List<LobbyPlayer> players) {
        this.code = code;
        this.arenaId = arenaId;
        this.hostUid = hostUid;
        this.status = status;
        this.difficulty = difficulty;
        this.maxPlayers = Math.max(1, maxPlayers);
        this.durationSeconds = durationSeconds;
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
    }

    public String getCode() {
        return code;
    }

    public String getArenaId() {
        return arenaId;
    }

    public String getHostUid() {
        return hostUid;
    }

    public String getStatus() {
        return status;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public List<LobbyPlayer> getPlayers() {
        return players;
    }

    public boolean isHost(String uid) {
        return hostUid.equals(uid);
    }

    public boolean isReady(String uid) {
        for (LobbyPlayer player : players) {
            if (player.getUid().equals(uid)) {
                return player.isReady();
            }
        }
        return false;
    }

    public boolean allPlayersReady() {
        if (players.size() < maxPlayers) {
            return false;
        }
        for (LobbyPlayer player : players) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }
}