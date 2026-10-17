package com.ripti.cluerush.map;

public class MapMarker {

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final double revealRadiusMeters;
    private final boolean completed;
    private final boolean discovered;

    public MapMarker(String id, String name, double latitude, double longitude,
                     double revealRadiusMeters, boolean completed, boolean discovered) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.revealRadiusMeters = revealRadiusMeters;
        this.completed = completed;
        this.discovered = discovered;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getRevealRadiusMeters() {
        return revealRadiusMeters;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isDiscovered() {
        return discovered;
    }
}