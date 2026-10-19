package com.ripti.cluerush.game;

public enum HeatLevel {
    COLD,
    COOL,
    WARM,
    HOT;

    private static final double HOT_FACTOR = 2.0;
    private static final double WARM_FACTOR = 5.0;
    private static final double COOL_FACTOR = 12.0;

    public static HeatLevel forDistance(double meters, double radiusMeters) {
        if (meters <= radiusMeters * HOT_FACTOR) {
            return HOT;
        }
        if (meters <= radiusMeters * WARM_FACTOR) {
            return WARM;
        }
        if (meters <= radiusMeters * COOL_FACTOR) {
            return COOL;
        }
        return COLD;
    }
}