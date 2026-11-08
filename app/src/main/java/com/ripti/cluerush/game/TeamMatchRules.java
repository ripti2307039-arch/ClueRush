package com.ripti.cluerush.game;

import java.util.Map;

/**
 * Win rules for the 2v2 capture mode. Pure logic, no Android or Firebase types,
 * so every player's device reaches the same result from the same captured data.
 *
 * Order: more captured locations, then more task points, then the team whose
 * last capture happened earlier, otherwise a draw.
 */
public final class TeamMatchRules {

    public static final String TEAM_A = "A";
    public static final String TEAM_B = "B";

    public enum Decider {
        CAPTURES,
        TASK_POINTS,
        LAST_CAPTURE,
        DRAW,
        TEAM_LEFT
    }

    public static final class Result {

        private final String winner;
        private final Decider decider;
        private final int capturesA;
        private final int capturesB;
        private final int pointsA;
        private final int pointsB;

        Result(String winner, Decider decider, int capturesA, int capturesB,
               int pointsA, int pointsB) {
            this.winner = winner;
            this.decider = decider;
            this.capturesA = capturesA;
            this.capturesB = capturesB;
            this.pointsA = pointsA;
            this.pointsB = pointsB;
        }

        /** "A", "B", or null for a draw. */
        public String getWinner() {
            return winner;
        }

        public Decider getDecider() {
            return decider;
        }

        public int getCapturesA() {
            return capturesA;
        }

        public int getCapturesB() {
            return capturesB;
        }

        public int getPointsA() {
            return pointsA;
        }

        public int getPointsB() {
            return pointsB;
        }

        public int getCaptures(String team) {
            return TEAM_A.equals(team) ? capturesA : capturesB;
        }

        public MatchResult.Outcome outcomeFor(String team) {
            if (winner == null) {
                return MatchResult.Outcome.DRAW;
            }
            return winner.equals(team) ? MatchResult.Outcome.WIN : MatchResult.Outcome.LOSS;
        }
    }

    private TeamMatchRules() {
    }

    public static Result decide(Map<String, CaptureSession.CaptureInfo> captured) {
        int capturesA = 0;
        int capturesB = 0;
        int pointsA = 0;
        int pointsB = 0;
        long lastA = 0L;
        long lastB = 0L;

        for (CaptureSession.CaptureInfo info : captured.values()) {
            if (TEAM_A.equals(info.getTeam())) {
                capturesA++;
                pointsA += info.getTaskPoints();
                lastA = Math.max(lastA, info.getCapturedAt());
            } else if (TEAM_B.equals(info.getTeam())) {
                capturesB++;
                pointsB += info.getTaskPoints();
                lastB = Math.max(lastB, info.getCapturedAt());
            }
        }

        if (capturesA != capturesB) {
            return new Result(capturesA > capturesB ? TEAM_A : TEAM_B, Decider.CAPTURES,
                    capturesA, capturesB, pointsA, pointsB);
        }
        if (pointsA != pointsB) {
            return new Result(pointsA > pointsB ? TEAM_A : TEAM_B, Decider.TASK_POINTS,
                    capturesA, capturesB, pointsA, pointsB);
        }
        if (lastA > 0L && lastB > 0L && lastA != lastB) {
            return new Result(lastA < lastB ? TEAM_A : TEAM_B, Decider.LAST_CAPTURE,
                    capturesA, capturesB, pointsA, pointsB);
        }
        return new Result(null, Decider.DRAW, capturesA, capturesB, pointsA, pointsB);
    }

    /** The other team left or lost connection: the remaining team wins. */
    public static Result teamLeft(String winnerTeam,
                                  Map<String, CaptureSession.CaptureInfo> captured) {
        Result counted = decide(captured);
        return new Result(winnerTeam, Decider.TEAM_LEFT, counted.capturesA,
                counted.capturesB, counted.pointsA, counted.pointsB);
    }
}