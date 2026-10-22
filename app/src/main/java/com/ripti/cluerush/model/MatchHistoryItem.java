package com.ripti.cluerush.model;

import androidx.annotation.Nullable;

public final class MatchHistoryItem {

    private final long id;
    private final String uid;
    private final String arenaId;
    private final String arenaName;
    private final String difficulty;
    private final int myScore;
    @Nullable
    private final Integer opponentScore;
    private final String opponent;
    private final String outcome;
    private final String decider;
    private final long playedAt;

    public MatchHistoryItem(long id, String uid, String arenaId, String arenaName,
                            String difficulty, int myScore, @Nullable Integer opponentScore,
                            String opponent, String outcome, String decider, long playedAt) {
        this.id = id;
        this.uid = uid;
        this.arenaId = arenaId;
        this.arenaName = arenaName;
        this.difficulty = difficulty;
        this.myScore = myScore;
        this.opponentScore = opponentScore;
        this.opponent = opponent;
        this.outcome = outcome;
        this.decider = decider;
        this.playedAt = playedAt;
    }


    public static MatchHistoryItem create(String uid, String arenaId, String arenaName,
                                          String difficulty, int myScore,
                                          @Nullable Integer opponentScore, String opponent,
                                          String outcome, String decider) {
        return new MatchHistoryItem(0L, uid, arenaId, arenaName, difficulty, myScore,
                opponentScore, opponent, outcome, decider, System.currentTimeMillis());
    }

    public long getId() { return id; }
    public String getUid() { return uid; }
    public String getArenaId() { return arenaId; }
    public String getArenaName() { return arenaName; }
    public String getDifficulty() { return difficulty; }
    public int getMyScore() { return myScore; }
    public boolean hasOpponentScore() { return opponentScore != null; }
    public int getOpponentScore() { return opponentScore == null ? 0 : opponentScore; }
    public String getOpponent() { return opponent; }
    public String getOutcome() { return outcome; }
    public String getDecider() { return decider; }
    public long getPlayedAt() { return playedAt; }
}