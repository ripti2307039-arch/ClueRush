package com.ripti.cluerush.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class LightsGenerator {

    private LightsGenerator() {
    }

    static boolean[] generate(Random random, int size, int presses) {
        int total = size * size;
        boolean[] lit = new boolean[total];
        do {
            java.util.Arrays.fill(lit, false);
            List<Integer> cells = new ArrayList<>(total);
            for (int i = 0; i < total; i++) {
                cells.add(i);
            }
            Collections.shuffle(cells, random);
            for (int i = 0; i < Math.min(presses, total); i++) {
                press(lit, size, cells.get(i));
            }
        } while (countLit(lit) == 0);
        return lit;
    }

    static void press(boolean[] lit, int size, int index) {
        int row = index / size;
        int col = index % size;
        toggle(lit, size, row, col);
        toggle(lit, size, row - 1, col);
        toggle(lit, size, row + 1, col);
        toggle(lit, size, row, col - 1);
        toggle(lit, size, row, col + 1);
    }

    static int countLit(boolean[] lit) {
        int count = 0;
        for (boolean on : lit) {
            if (on) {
                count++;
            }
        }
        return count;
    }

    private static void toggle(boolean[] lit, int size, int row, int col) {
        if (row < 0 || col < 0 || row >= size || col >= size) {
            return;
        }
        lit[row * size + col] = !lit[row * size + col];
    }
}