package com.ripti.cluerush.model;

import com.ripti.cluerush.game.StatsRules;

public class UserProfile {

    private final long xp;
    private final long totalWins;
    private final long matchesPlayed;
    private final long draws;
    private final long rating;
    private final long soloGames;

    public UserProfile(long xp, long totalWins, long matchesPlayed, long draws, long rating,
                       long soloGames) {
        this.xp = Math.max(0L, xp);
        this.totalWins = totalWins;
        this.matchesPlayed = matchesPlayed;
        this.draws = draws;
        this.rating = Math.max(0L, rating);
        this.soloGames = soloGames;
    }

    public static UserProfile empty() {
        return new UserProfile(0, 0, 0, 0, 0, 0);
    }

    public long getXp() {
        return xp;
    }

    public int getLevel() {
        return StatsRules.levelForXp(xp);
    }

    public int getTitleIndex() {
        return StatsRules.titleIndex(getLevel());
    }

    public long getTotalWins() {
        return totalWins;
    }

    public long getMatchesPlayed() {
        return matchesPlayed;
    }

    public long getDraws() {
        return draws;
    }

    public long getRating() {
        return rating;
    }

    public long getSoloGames() {
        return soloGames;
    }

    public int getXpInLevel() {
        return (int) StatsRules.xpInLevel(xp);
    }

    public int getXpSpan() {
        return (int) StatsRules.xpSpan(xp);
    }

    public int getWinRatePercent() {
        if (matchesPlayed <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * totalWins / matchesPlayed);
    }
}