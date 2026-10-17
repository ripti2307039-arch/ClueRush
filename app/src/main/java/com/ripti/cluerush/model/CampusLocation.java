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
    private final List<Clue> clues;

    public CampusLocation(String id, String name, double latitude, double longitude,
                          double radiusMeters, List<Clue> clues) {
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

    public String pickClue(Random random, Difficulty difficulty) {
        List<Clue> exact = new ArrayList<>();
        List<Clue> general = new ArrayList<>();
        for (Clue clue : clues) {
            if (clue.getLevel() == null) {
                general.add(clue);
            } else if (clue.getLevel() == difficulty) {
                exact.add(clue);
            }
        }
        List<Clue> pool = !exact.isEmpty() ? exact : (!general.isEmpty() ? general : clues);
        return pool.get(random.nextInt(pool.size())).getText();
    }
}