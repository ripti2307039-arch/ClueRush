package com.ripti.cluerush.game;

public final class StatsRules {

    public static final int WIN_XP = 50;
    public static final int DRAW_XP = 25;
    public static final int LOSS_XP = 10;
    public static final int WIN_RATING = 25;
    public static final int LOSS_RATING = -15;

    private static final long LEVEL_STEP = 50L;
    private static final int MAX_LEVEL = 200;
    private static final int[] TITLE_FROM_LEVEL = {1, 5, 10, 20};

    private StatsRules() {
    }

    public static long xpForLevel(int level) {
        return LEVEL_STEP * (level - 1L) * level;
    }

    public static int levelForXp(long xp) {
        int level = 1;
        while (level < MAX_LEVEL && xpForLevel(level + 1) <= xp) {
            level++;
        }
        return level;
    }

    public static long xpInLevel(long xp) {
        return xp - xpForLevel(levelForXp(xp));
    }

    public static long xpSpan(long xp) {
        int level = levelForXp(xp);
        return xpForLevel(level + 1) - xpForLevel(level);
    }

    public static long xpToNext(long xp) {
        return xpForLevel(levelForXp(xp) + 1) - xp;
    }

    public static int soloXp(int score) {
        return Math.max(0, score) / 10;
    }

    public static int matchXp(int score, MatchResult.Outcome outcome) {
        int bonus;
        switch (outcome) {
            case WIN:
                bonus = WIN_XP;
                break;
            case DRAW:
                bonus = DRAW_XP;
                break;
            default:
                bonus = LOSS_XP;
                break;
        }
        return soloXp(score) + bonus;
    }

    public static int ratingDelta(MatchResult.Outcome outcome) {
        switch (outcome) {
            case WIN:
                return WIN_RATING;
            case DRAW:
                return 0;
            default:
                return LOSS_RATING;
        }
    }

    public static long applyRating(long current, int delta) {
        return Math.max(0L, current + delta);
    }

    public static int titleIndex(int level) {
        int index = 0;
        for (int i = 0; i < TITLE_FROM_LEVEL.length; i++) {
            if (level >= TITLE_FROM_LEVEL[i]) {
                index = i;
            }
        }
        return index;
    }
}