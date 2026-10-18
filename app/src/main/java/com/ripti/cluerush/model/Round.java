package com.ripti.cluerush.model;

import com.ripti.cluerush.task.TaskSpec;

public class Round {

    private final CampusLocation location;
    private final String clue;
    private final TaskSpec task;

    public Round(CampusLocation location, String clue, TaskSpec task) {
        this.location = location;
        this.clue = clue;
        this.task = task;
    }

    public CampusLocation getLocation() {
        return location;
    }

    public String getClue() {
        return clue;
    }

    public TaskSpec getTask() {
        return task;
    }
}