package com.ripti.cluerush.model;

public class Clue {

    private final String text;
    private final Difficulty level;

    public Clue(String text, Difficulty level) {
        this.text = text;
        this.level = level;
    }

    public String getText() {
        return text;
    }

    public Difficulty getLevel() {
        return level;
    }
}