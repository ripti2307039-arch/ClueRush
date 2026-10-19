package com.ripti.cluerush.model;

public class PlayerState {

    public static final String PHASE_EXPLORING = "exploring";
    public static final String PHASE_SOLVING = "solving";
    public static final String PHASE_FINISHED = "finished";
    public static final String PHASE_FORFEIT = "forfeit";

    private final String uid;
    private final String name;
    private final int rounds;
    private final String phase;
    private final int score;
    private final int solved;
    private final int wrong;
    private final int finalScore;
    private final long finishedAt;
    private final double latitude;
    private final double longitude;
    private final boolean hasPosition;
    private final boolean connected;
    private final String forfeitedUid;
    private final String roundPoints;

    public PlayerState(String uid, String name, int rounds, String phase, int score, int solved,
                       int wrong, int finalScore, long finishedAt, double latitude,
                       double longitude, boolean hasPosition, boolean connected,
                       String forfeitedUid, String roundPoints) {
        this.uid = uid;
        this.name = name;
        this.rounds = rounds;
        this.phase = phase;
        this.score = score;
        this.solved = solved;
        this.wrong = wrong;
        this.finalScore = finalScore;
        this.finishedAt = finishedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hasPosition = hasPosition;
        this.connected = connected;
        this.forfeitedUid = forfeitedUid;
        this.roundPoints = roundPoints;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public int getRounds() {
        return rounds;
    }

    public String getPhase() {
        return phase;
    }

    public int getScore() {
        return score;
    }

    public int getSolved() {
        return solved;
    }

    public int getWrong() {
        return wrong;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean hasPosition() {
        return hasPosition;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getForfeitedUid() {
        return forfeitedUid;
    }

    public boolean isFinished() {
        return PHASE_FINISHED.equals(phase);
    }

    public boolean isForfeit() {
        return PHASE_FORFEIT.equals(phase);
    }

    public int[] roundPointsArray() {
        if (roundPoints == null || roundPoints.isEmpty()) {
            return new int[0];
        }
        String[] parts = roundPoints.split(",");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                values[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                values[i] = 0;
            }
        }
        return values;
    }
}