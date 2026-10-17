package com.ripti.cluerush.model;

public class Round {

    private final CampusLocation location;
    private final String clue;

    public Round(CampusLocation location, String clue) {
        this.location = location;
        this.clue = clue;
    }

    public CampusLocation getLocation() {
        return location;
    }

    public String getClue() {
        return clue;
    }
}