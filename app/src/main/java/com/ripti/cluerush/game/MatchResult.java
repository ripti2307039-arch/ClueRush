package com.ripti.cluerush.game;

public final class MatchResult {

    public enum Outcome {
        WIN,
        LOSS,
        DRAW
    }

    public enum Decider {
        SCORE,
        SOLVED,
        WRONG_PLACES,
        TIME,
        FORFEIT,
        DISCONNECT_OPPONENT,
        DISCONNECT_ME,
        CLINCH,
        DRAW
    }

    private final Outcome outcome;
    private final Decider decider;
    private final int myScore;
    private final int opponentScore;
    private final String opponentName;
    private final int[] opponentRoundPoints;

    public MatchResult(Outcome outcome, Decider decider, int myScore, int opponentScore,
                       String opponentName, int[] opponentRoundPoints) {
        this.outcome = outcome;
        this.decider = decider;
        this.myScore = myScore;
        this.opponentScore = opponentScore;
        this.opponentName = opponentName;
        this.opponentRoundPoints = opponentRoundPoints;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public Decider getDecider() {
        return decider;
    }

    public int getMyScore() {
        return myScore;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    public boolean isOpponentScoreKnown() {
        return opponentScore >= 0;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public int[] getOpponentRoundPoints() {
        return opponentRoundPoints;
    }
}