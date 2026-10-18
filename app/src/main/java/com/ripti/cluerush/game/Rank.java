package com.ripti.cluerush.game;

public enum Rank {
    S("S", 3),
    A("A", 2),
    B("B", 1),
    C("C", 0);

    private static final double S_RATIO = 0.8;
    private static final double A_RATIO = 0.6;
    private static final double B_RATIO = 0.4;

    private final String label;
    private final int stars;

    Rank(String label, int stars) {
        this.label = label;
        this.stars = stars;
    }

    public String getLabel() {
        return label;
    }

    public int getStars() {
        return stars;
    }

    public static Rank forScore(int score, int maxScore) {
        double ratio = maxScore <= 0 ? 0.0 : score / (double) maxScore;
        if (ratio >= S_RATIO) {
            return S;
        }
        if (ratio >= A_RATIO) {
            return A;
        }
        if (ratio >= B_RATIO) {
            return B;
        }
        return C;
    }
}