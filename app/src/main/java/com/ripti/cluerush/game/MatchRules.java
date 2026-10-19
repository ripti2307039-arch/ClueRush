package com.ripti.cluerush.game;

import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.PlayerState;

public final class MatchRules {

    private static final long TIME_TIE_MS = 1000L;

    private MatchRules() {
    }

    public static int finishCountdownSeconds(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 90;
            case MEDIUM:
                return 75;
            default:
                return 60;
        }
    }

    public static int potential(Difficulty difficulty, int totalRounds, int rounds, int score) {
        int remaining = Math.max(0, totalRounds - rounds);
        return score + remaining * ScoreRules.maxRoundPoints(difficulty)
                + ScoreRules.maxBonusPoints(difficulty);
    }

    public static MatchResult decide(PlayerState me, PlayerState opponent) {
        int mine = me.getFinalScore();
        int theirs = opponent.getFinalScore();
        if (mine != theirs) {
            return build(mine > theirs, MatchResult.Decider.SCORE, me, opponent);
        }
        if (me.getSolved() != opponent.getSolved()) {
            return build(me.getSolved() > opponent.getSolved(), MatchResult.Decider.SOLVED,
                    me, opponent);
        }
        if (me.getWrong() != opponent.getWrong()) {
            return build(me.getWrong() < opponent.getWrong(), MatchResult.Decider.WRONG_PLACES,
                    me, opponent);
        }
        long gap = me.getFinishedAt() - opponent.getFinishedAt();
        if (Math.abs(gap) > TIME_TIE_MS) {
            return build(gap < 0L, MatchResult.Decider.TIME, me, opponent);
        }
        return new MatchResult(MatchResult.Outcome.DRAW, MatchResult.Decider.DRAW, mine, theirs,
                opponent.getName(), opponent.roundPointsArray());
    }

    private static MatchResult build(boolean win, MatchResult.Decider decider, PlayerState me,
                                     PlayerState opponent) {
        return new MatchResult(win ? MatchResult.Outcome.WIN : MatchResult.Outcome.LOSS,
                decider, me.getFinalScore(), opponent.getFinalScore(), opponent.getName(),
                opponent.roundPointsArray());
    }
}