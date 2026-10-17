package com.ripti.cluerush.game;

public class TargetInfo {

    private final double distanceMeters;
    private final double bearingDegrees;
    private final boolean inside;
    private final boolean nearZone;
    private final HeatLevel heat;

    public TargetInfo(double distanceMeters, double bearingDegrees, boolean inside,
                      boolean nearZone, HeatLevel heat) {
        this.distanceMeters = distanceMeters;
        this.bearingDegrees = bearingDegrees;
        this.inside = inside;
        this.nearZone = nearZone;
        this.heat = heat;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public double getBearingDegrees() {
        return bearingDegrees;
    }

    public boolean isInside() {
        return inside;
    }

    public boolean isNearZone() {
        return nearZone;
    }

    public HeatLevel getHeat() {
        return heat;
    }
}