package com.ripti.cluerush.model;

public class GeoBounds {

    private final double north;
    private final double south;
    private final double east;
    private final double west;

    public GeoBounds(double north, double south, double east, double west) {
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
    }

    public double getNorth() {
        return north;
    }

    public double getSouth() {
        return south;
    }

    public double getEast() {
        return east;
    }

    public double getWest() {
        return west;
    }

    public double clampLatitude(double latitude) {
        return Math.max(south, Math.min(north, latitude));
    }

    public double clampLongitude(double longitude) {
        return Math.max(west, Math.min(east, longitude));
    }

    public GeoBounds expandedBy(double degrees) {
        return new GeoBounds(north + degrees, south - degrees, east + degrees, west - degrees);
    }
}