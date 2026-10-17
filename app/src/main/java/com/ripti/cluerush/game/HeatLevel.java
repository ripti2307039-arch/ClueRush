package com.ripti.cluerush.game;

public enum HeatLevel {
    COLD,
    COOL,
    WARM,
    HOT;

    public static HeatLevel forDistance(double meters) {
        if (meters <= 100.0) {
            return HOT;
        }
        if (meters <= 250.0) {
            return WARM;
        }
        if (meters <= 500.0) {
            return COOL;
        }
        return COLD;
    }
}