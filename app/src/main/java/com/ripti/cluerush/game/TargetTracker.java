package com.ripti.cluerush.game;

import com.ripti.cluerush.model.CampusLocation;
import com.ripti.cluerush.util.GeoMath;

public final class TargetTracker {

    public static final double REVEAL_DISTANCE_METERS = 60.0;

    private static final double REVEAL_MARGIN_METERS = 20.0;

    private TargetTracker() {
    }

    public static TargetInfo compute(double playerLat, double playerLng,
                                     CampusLocation target, boolean hard) {
        double distance = GeoMath.distanceMeters(playerLat, playerLng,
                target.getLatitude(), target.getLongitude());
        double bearing = GeoMath.bearingDegrees(playerLat, playerLng,
                target.getLatitude(), target.getLongitude());
        boolean inside = distance <= target.getRadiusMeters();
        double revealDistance = Math.max(REVEAL_DISTANCE_METERS,
                target.getRadiusMeters() + REVEAL_MARGIN_METERS);
        boolean nearZone = distance <= revealDistance;
        return new TargetInfo(distance, bearing, inside, nearZone,
                HeatLevel.forDistance(distance, hard));
    }

    public static int directionIndex(double bearingDegrees) {
        return (int) (Math.round(bearingDegrees / 45.0) % 8);
    }
}