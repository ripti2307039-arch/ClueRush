
package com.ripti.cluerush.game;

import android.content.Context;

import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.CampusLocation;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.Round;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class CaptureSession {

    public static final int LOCATION_COUNT = 8;

    private final List<Round> rounds;
    private final Map<String, CaptureInfo> captured;
    private String selectedLocationId;

    public CaptureSession(Context context, Arena arena,
                          Difficulty difficulty, long seed) {

        if (context == null || arena == null || difficulty == null) {
            throw new IllegalArgumentException("Invalid capture session configuration");
        }

        if (arena.getLocations().size() < LOCATION_COUNT) {
            throw new IllegalArgumentException("Arena must contain at least 8 locations");
        }

        rounds = Collections.unmodifiableList(
                new ArrayList<>(RoundGenerator.generate(
                        context,
                        arena,
                        LOCATION_COUNT,
                        difficulty,
                        new Random(seed),
                        false
                ))
        );

        if (rounds.size() != LOCATION_COUNT) {
            throw new IllegalStateException("Could not generate 8 locations");
        }

        captured = new HashMap<>();
    }

    public List<Round> getRounds() {
        return rounds;
    }

    public Round getRound(int index) {
        if (index < 0 || index >= rounds.size()) {
            return null;
        }
        return rounds.get(index);
    }

    public Round getRoundByLocationId(String locationId) {
        for (Round round : rounds) {
            if (round.getLocation().getId().equals(locationId)) {
                return round;
            }
        }
        return null;
    }

    public boolean selectLocation(String locationId) {
        Round round = getRoundByLocationId(locationId);

        if (round == null || isCaptured(locationId)) {
            return false;
        }

        selectedLocationId = locationId;
        return true;
    }

    public String getSelectedLocationId() {
        return selectedLocationId;
    }

    public Round getSelectedRound() {
        return selectedLocationId == null
                ? null
                : getRoundByLocationId(selectedLocationId);
    }

    public void releaseSelection() {
        selectedLocationId = null;
    }

    public boolean isCaptured(String locationId) {
        return captured.containsKey(locationId);
    }

    public CaptureInfo getCaptureInfo(String locationId) {
        return captured.get(locationId);
    }

    public boolean applyCapture(String locationId, String team,
                                long capturedAt, int taskPoints) {

        if (getRoundByLocationId(locationId) == null
                || team == null
                || team.trim().isEmpty()
                || isCaptured(locationId)) {
            return false;
        }

        captured.put(locationId,
                new CaptureInfo(team, capturedAt, Math.max(0, taskPoints)));

        if (locationId.equals(selectedLocationId)) {
            selectedLocationId = null;
        }

        return true;
    }

    public int getCapturedCount() {
        return captured.size();
    }

    public int getRemainingCount() {
        return LOCATION_COUNT - captured.size();
    }

    public boolean isComplete() {
        return captured.size() >= LOCATION_COUNT;
    }

    public Map<String, CaptureInfo> getCapturedLocations() {
        return Collections.unmodifiableMap(new HashMap<>(captured));
    }

    public static class CaptureInfo {

        private final String team;
        private final long capturedAt;
        private final int taskPoints;

        public CaptureInfo(String team, long capturedAt, int taskPoints) {
            this.team = team;
            this.capturedAt = capturedAt;
            this.taskPoints = taskPoints;
        }

        public String getTeam() {
            return team;
        }

        public long getCapturedAt() {
            return capturedAt;
        }

        public int getTaskPoints() {
            return taskPoints;
        }
    }
}
