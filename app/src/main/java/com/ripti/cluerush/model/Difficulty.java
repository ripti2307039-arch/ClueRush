package com.ripti.cluerush.model;

public enum Difficulty {
    EASY(Match.DIFFICULTY_EASY, true),
    MEDIUM(Match.DIFFICULTY_MEDIUM, false),
    HARD(Match.DIFFICULTY_HARD, false);

    private final String key;
    private final boolean showsHeat;

    Difficulty(String key, boolean showsHeat) {
        this.key = key;
        this.showsHeat = showsHeat;
    }

    public String getKey() {
        return key;
    }

    public boolean showsHeat() {
        return showsHeat;
    }

    public static Difficulty fromKey(String key) {
        for (Difficulty difficulty : values()) {
            if (difficulty.key.equals(key)) {
                return difficulty;
            }
        }
        return EASY;
    }

    public static Difficulty fromName(String name) {
        if (name == null) {
            return null;
        }
        for (Difficulty difficulty : values()) {
            if (difficulty.name().equalsIgnoreCase(name.trim())) {
                return difficulty;
            }
        }
        return null;
    }
}