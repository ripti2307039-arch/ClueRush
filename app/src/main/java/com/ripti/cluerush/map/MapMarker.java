
package com.ripti.cluerush.map;

public class MapMarker {

    private final String id;

    public static final String CAPTURE_MINE = "A";

    public static final String CAPTURE_ENEMY = "B";
    private final String name;
    private final double latitude;
    private final double longitude;
    private final double revealRadiusMeters;
    private final boolean completed;
    private final boolean discovered;
    private final String captureTeam;

    public MapMarker(String id, String name, double latitude, double longitude,
                     double revealRadiusMeters, boolean completed, boolean discovered) {
        this(id, name, latitude, longitude, revealRadiusMeters, completed, discovered, null);
    }

    public MapMarker(String id, String name, double latitude, double longitude,
                     double revealRadiusMeters, boolean completed, boolean discovered,
                     String captureTeam) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.revealRadiusMeters = revealRadiusMeters;
        this.completed = completed;
        this.discovered = discovered;
        this.captureTeam = captureTeam;
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

    public String getCaptureTeam() {
        return captureTeam;
    }

    public boolean isCaptured() {
        return "A".equals(captureTeam) || "B".equals(captureTeam);
    }
}
