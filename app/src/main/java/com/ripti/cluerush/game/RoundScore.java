package com.ripti.cluerush.game;

public class RoundScore {

    private final boolean solved;
    private final int mistakes;
    private final int secondsLeft;
    private final int base;
    private final int timeBonus;
    private final int mistakePenalty;
    private final double streakMultiplier;
    private final int streakBonus;
    private final int findBonus;

    public RoundScore(boolean solved, int mistakes, int secondsLeft, int base, int timeBonus,
                      int mistakePenalty, double streakMultiplier, int streakBonus,
                      int findBonus) {
        this.solved = solved;
        this.mistakes = mistakes;
        this.secondsLeft = secondsLeft;
        this.base = base;
        this.timeBonus = timeBonus;
        this.mistakePenalty = mistakePenalty;
        this.streakMultiplier = streakMultiplier;
        this.streakBonus = streakBonus;
        this.findBonus = findBonus;
    }

    public boolean isSolved() {
        return solved;
    }

    public int getMistakes() {
        return mistakes;
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }

    public int getBase() {
        return base;
    }

    public int getTimeBonus() {
        return timeBonus;
    }

    public int getMistakePenalty() {
        return mistakePenalty;
    }

    public double getStreakMultiplier() {
        return streakMultiplier;
    }

    public int getStreakBonus() {
        return streakBonus;
    }

    public int getFindBonus() {
        return findBonus;
    }

    public int getTotal() {
        return base + timeBonus - mistakePenalty + streakBonus + findBonus;
    }
}