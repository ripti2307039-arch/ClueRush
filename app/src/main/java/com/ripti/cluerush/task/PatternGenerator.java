package com.ripti.cluerush.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class PatternGenerator {

    private PatternGenerator() {
    }

    static int[] generate(Random random, int cellCount, int length) {
        List<Integer> cells = new ArrayList<>(cellCount);
        for (int i = 0; i < cellCount; i++) {
            cells.add(i);
        }
        Collections.shuffle(cells, random);
        int size = Math.min(length, cellCount);
        int[] sequence = new int[size];
        for (int i = 0; i < size; i++) {
            sequence[i] = cells.get(i);
        }
        return sequence;
    }
}