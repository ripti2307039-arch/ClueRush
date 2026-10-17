package com.ripti.cluerush.game;

import com.ripti.cluerush.model.Round;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameSession {

    private final List<Round> rounds;
    private int index;

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
}