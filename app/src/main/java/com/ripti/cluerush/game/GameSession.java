package com.ripti.cluerush.game;

import com.ripti.cluerush.model.Round;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameSession {

    public static final int WRONG_PLACE_PENALTY = 10;

    public static final class TaskOutcome {
        private final boolean solved;
        private final int mistakes;
        private final int secondsLeft;

        TaskOutcome(boolean solved, int mistakes, int secondsLeft) {
            this.solved = solved;
            this.mistakes = mistakes;
            this.secondsLeft = secondsLeft;
        }

        public boolean isSolved() {
            return solved;
        }

        public int getMistakes() {
            return mistakes;
        }

        public int getSecondsLeft() {
            return secondsLeft;
        }
    }

    private final List<Round> rounds;
    private final Set<String> discovered = new HashSet<>();
    private final List<TaskOutcome> outcomes = new ArrayList<>();
    private int index;
    private int wrongAttempts;

    public GameSession(List<Round> rounds) {
        this.rounds = new ArrayList<>(rounds);
    }

    public Round getCurrent() {
        return index < rounds.size() ? rounds.get(index) : null;
    }

    public int getIndex() {
        return index;
    }

    public int getTotal() {
        return rounds.size();
    }

    public boolean isFinished() {
        return index >= rounds.size();
    }

    public void advance() {
        if (index < rounds.size()) {
            index++;
        }
    }

    public Set<String> getCompletedLocationIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < index && i < rounds.size(); i++) {
            ids.add(rounds.get(i).getLocation().getId());
        }
        return ids;
    }

    public boolean discover(String locationId) {
        return discovered.add(locationId);
    }

    public Set<String> getDiscoveredIds() {
        return new HashSet<>(discovered);
    }

    public void registerWrongAttempt() {
        wrongAttempts++;
    }

    public int getWrongAttempts() {
        return wrongAttempts;
    }

    public int getPenaltyPoints() {
        return wrongAttempts * WRONG_PLACE_PENALTY;
    }

    public void recordTask(boolean solved, int mistakes, int secondsLeft) {
        outcomes.add(new TaskOutcome(solved, mistakes, secondsLeft));
    }

    public List<TaskOutcome> getTaskOutcomes() {
        return Collections.unmodifiableList(outcomes);
    }

    public int getTasksSolved() {
        int solved = 0;
        for (TaskOutcome outcome : outcomes) {
            if (outcome.isSolved()) {
                solved++;
            }
        }
        return solved;
    }

    public int getTaskMistakes() {
        int total = 0;
        for (TaskOutcome outcome : outcomes) {
            total += outcome.getMistakes();
        }
        return total;
    }
}