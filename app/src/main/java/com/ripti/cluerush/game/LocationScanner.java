package com.ripti.cluerush.game;

import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.CampusLocation;
import com.ripti.cluerush.util.GeoMath;

public final class LocationScanner {

    private static final double ZONE_MARGIN_METERS = 20.0;

    public static final class Hit {
        private final CampusLocation location;
        private final double distanceMeters;
        private final boolean inside;
        private final boolean nearZone;

        Hit(CampusLocation location, double distanceMeters, boolean inside, boolean nearZone) {
            this.location = location;
            this.distanceMeters = distanceMeters;
            this.inside = inside;
            this.nearZone = nearZone;
        }

        public CampusLocation getLocation() {
            return location;
        }

        public double getDistanceMeters() {
            return distanceMeters;
        }

        public boolean isInside() {
            return inside;
        }

        public boolean isNearZone() {
            return nearZone;
        }
    }

    private LocationScanner() {
    }

    public static Hit nearest(Arena arena, double lat, double lng) {
        CampusLocation best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CampusLocation location : arena.getLocations()) {
            double distance = distanceTo(location, lat, lng);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = location;
            }
        }
        if (best == null) {
            return null;
        }
        boolean inside = bestDistance <= best.getRadiusMeters();
        boolean nearZone = bestDistance <= best.getRadiusMeters() + ZONE_MARGIN_METERS;
        return new Hit(best, bestDistance, inside, nearZone);
    }

    public static double distanceTo(CampusLocation location, double lat, double lng) {
        return GeoMath.distanceMeters(lat, lng, location.getLatitude(), location.getLongitude());
    }
}