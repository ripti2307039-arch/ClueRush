package com.ripti.cluerush.task;

import com.ripti.cluerush.model.Difficulty;

public class TaskSpec {

    private final TaskType type;
    private final Difficulty difficulty;
    private final long seed;

    public TaskSpec(TaskType type, Difficulty difficulty, long seed) {
        this.type = type;
        this.difficulty = difficulty;
        this.seed = seed;
    }

    public TaskType getType() {
        return type;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public long getSeed() {
        return seed;
    }
}