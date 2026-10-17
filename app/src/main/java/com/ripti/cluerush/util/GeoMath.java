package com.ripti.cluerush.util;

public final class GeoMath {

    public static final double METERS_PER_DEGREE_LATITUDE = 111_320.0;

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoMath() {
    }

    public static double metersToLatitudeDegrees(double meters) {
        return meters / METERS_PER_DEGREE_LATITUDE;
    }

    public static double metersToLongitudeDegrees(double meters, double latitude) {
        double scale = Math.cos(Math.toRadians(latitude));
        if (Math.abs(scale) < 1e-6) {
            return 0.0;
        }
        return meters / (METERS_PER_DEGREE_LATITUDE * scale);
    }

    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    public static double bearingDegrees(double fromLat, double fromLng, double toLat, double toLng) {
        double north = (toLat - fromLat) * METERS_PER_DEGREE_LATITUDE;
        double east = (toLng - fromLng) * METERS_PER_DEGREE_LATITUDE
                * Math.cos(Math.toRadians(fromLat));
        double degrees = Math.toDegrees(Math.atan2(east, north));
        return (degrees + 360.0) % 360.0;
    }
}