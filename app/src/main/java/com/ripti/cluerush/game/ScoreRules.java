package com.ripti.cluerush.game;

import com.ripti.cluerush.model.Difficulty;

public final class ScoreRules {

    public static final int WRONG_PLACE_PENALTY = 10;

    private static final double TIME_BONUS_SHARE = 0.5;
    private static final double FIND_BONUS_SHARE = 0.5;
    private static final double MIN_SOLVED_SHARE = 0.2;
    private static final double PERFECT_SHARE = 1.5;
    private static final double MAX_POINTS_SHARE = 2.0;
    private static final double STREAK_STEP = 0.1;
    private static final int STREAK_CAP = 3;

    private ScoreRules() {
    }

    public static int base(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 100;
            case MEDIUM:
                return 200;
            default:
                return 300;
        }
    }

    public static int mistakePenalty(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 5;
            case MEDIUM:
                return 10;
            default:
                return 15;
        }
    }

    public static int findParSeconds(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 180;
            case MEDIUM:
                return 150;
            default:
                return 120;
        }
    }

    public static int perfectBonus(Difficulty difficulty) {
        return (int) Math.round(base(difficulty) * PERFECT_SHARE);
    }

    public static int maxPoints(Difficulty difficulty, int rounds) {
        return (int) Math.round(rounds * base(difficulty) * MAX_POINTS_SHARE);
    }

    public static double streakMultiplier(int streak) {
        return 1.0 + STREAK_STEP * Math.min(streak, STREAK_CAP);
    }

    public static RoundScore score(Difficulty difficulty, boolean solved, int mistakes,
                                   int secondsLeft, int limitSeconds, double findSeconds,
                                   int streak) {
        int base = base(difficulty);
        double findFraction = clamp(1.0 - findSeconds / findParSeconds(difficulty));
        int findBonus = (int) Math.round(base * FIND_BONUS_SHARE * findFraction);

        if (!solved) {
            return new RoundScore(false, mistakes, secondsLeft, 0, 0, 0, 1.0, 0, findBonus);
        }

        double timeFraction = limitSeconds <= 0 ? 0.0 : clamp(secondsLeft / (double) limitSeconds);
        int timeBonus = (int) Math.round(base * TIME_BONUS_SHARE * timeFraction);
        int raw = base + timeBonus - mistakes * mistakePenalty(difficulty);
        int floor = (int) Math.round(base * MIN_SOLVED_SHARE);
        int taskPoints = Math.max(floor, raw);
        int appliedPenalty = base + timeBonus - taskPoints;
        double multiplier = streakMultiplier(streak);
        int streakBonus = (int) Math.round(taskPoints * (multiplier - 1.0));

        return new RoundScore(true, mistakes, secondsLeft, base, timeBonus, appliedPenalty,
                multiplier, streakBonus, findBonus);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}