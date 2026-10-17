package com.ripti.cluerush.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CampusLocation {

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final double radiusMeters;
    private final List<String> clues;

    public CampusLocation(String id, String name, double latitude, double longitude,
                          double radiusMeters, List<String> clues) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.clues = Collections.unmodifiableList(new ArrayList<>(clues));
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

    public double getRadiusMeters() {
        return radiusMeters;
    }

    public String pickClue(Random random) {
        return clues.get(random.nextInt(clues.size()));
    }
}