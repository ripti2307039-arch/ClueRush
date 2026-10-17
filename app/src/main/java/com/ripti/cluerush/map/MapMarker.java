package com.ripti.cluerush.map;

public class MapMarker {

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final boolean completed;

    public MapMarker(String id, String name, double latitude, double longitude, boolean completed) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.completed = completed;
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

    public boolean isCompleted() {
        return completed;
    }
}