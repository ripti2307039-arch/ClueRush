package com.ripti.cluerush.game;

import android.content.Context;

import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.CampusLocation;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.Round;
import com.ripti.cluerush.task.TaskSpec;
import com.ripti.cluerush.task.TaskType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RoundGenerator {

    private RoundGenerator() {
    }

    public static List<Round> generate(Context context, Arena arena, int roundCount,
                                       Difficulty difficulty, Random random,
                                       boolean avoidRepeats) {
        List<CampusLocation> pool = new ArrayList<>(arena.getLocations());
        Collections.shuffle(pool, random);

        List<TaskType> types = new ArrayList<>(Arrays.asList(TaskType.values()));
        Collections.shuffle(types, random);

        int count = Math.min(roundCount, pool.size());
        List<Round> rounds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            CampusLocation location = pool.get(i);
            TaskType type = types.get(i % types.size());
            TaskSpec task = new TaskSpec(type, difficulty, random.nextLong());
            String clue = ClueBank.pick(context, arena.getId(), location, difficulty,
                    random, avoidRepeats);
            rounds.add(new Round(location, clue, task));
        }
        return rounds;
    }
}