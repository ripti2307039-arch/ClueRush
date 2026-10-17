package com.ripti.cluerush.game;

import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.CampusLocation;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.Round;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RoundGenerator {

    private RoundGenerator() {
    }

    public static List<Round> generate(Arena arena, int roundCount, Difficulty difficulty,
                                       Random random) {
        List<CampusLocation> pool = new ArrayList<>(arena.getLocations());
        Collections.shuffle(pool, random);
        int count = Math.min(roundCount, pool.size());
        List<Round> rounds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            CampusLocation location = pool.get(i);
            rounds.add(new Round(location, location.pickClue(random, difficulty)));
        }
        return rounds;
    }
}