package com.ripti.cluerush.model;

public class UserProfile {

    public static final int XP_PER_LEVEL = 100;

    private final long xp;
    private final long level;
    private final long totalWins;
    private final long matchesPlayed;

    public UserProfile(long xp, long level, long totalWins, long matchesPlayed) {
        this.xp = xp;
        this.level = Math.max(1, level);
        this.totalWins = totalWins;
        this.matchesPlayed = matchesPlayed;
    }

    public static UserProfile empty() {
        return new UserProfile(0, 1, 0, 0);
    }

    public long getLevel() {
        return level;
    }

    public long getTotalWins() {
        return totalWins;
    }

    public long getMatchesPlayed() {
        return matchesPlayed;
    }

    public int getXpInLevel() {
        return (int) (xp % XP_PER_LEVEL);
    }
}