package com.ripti.cluerush.task;

import com.ripti.cluerush.model.Difficulty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class OddGenerator {

    private static final long[] PRIMES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47};
    private static final long[] SQUARES = {1, 4, 9, 16, 25, 36, 49, 64, 81, 100, 121, 144};
    private static final long[] POWERS_OF_TWO = {2, 4, 8, 16, 32, 64, 128, 256, 512};
    private static final long[] FIBONACCI = {1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144};
    private static final long[] TRIANGULAR = {1, 3, 6, 10, 15, 21, 28, 36, 45, 55};
    private static final long[] CUBES = {1, 8, 27, 64, 125, 216};
    private static final int[] MEDIUM_MULTIPLES = {3, 4, 6, 7, 8, 9};
    private static final int[] HARD_MULTIPLES = {7, 11, 13};

    private OddGenerator() {
    }

    static QuizTaskView.Question generate(String text, Difficulty difficulty, Random random) {
        switch (difficulty) {
            case EASY:
                return easy(text, random);
            case MEDIUM:
                return medium(text, random);
            default:
                return hard(text, random);
        }
    }

    private static QuizTaskView.Question easy(String text, Random random) {
        switch (random.nextInt(3)) {
            case 0:
                return build(text, step(2, 60, 2), step(1, 59, 2), random);
            case 1:
                return build(text, step(1, 59, 2), step(2, 60, 2), random);
            default:
                return build(text, step(5, 100, 5),
                        minus(range(6, 99), step(5, 100, 5)), random);
        }
    }

    private static QuizTaskView.Question medium(String text, Random random) {
        long[] composites = minus(range(4, 50), PRIMES);
        switch (random.nextInt(4)) {
            case 0:
            case 1: {
                int m = MEDIUM_MULTIPLES[random.nextInt(MEDIUM_MULTIPLES.length)];
                return multiples(text, m, random);
            }
            case 2:
                return build(text, PRIMES, composites, random);
            default:
                return build(text, composites, PRIMES, random);
        }
    }

    private static QuizTaskView.Question hard(String text, Random random) {
        switch (random.nextInt(6)) {
            case 0:
                return build(text, SQUARES, minus(range(2, 150), SQUARES), random);
            case 1:
                return build(text, POWERS_OF_TWO, minus(range(3, 520), POWERS_OF_TWO), random);
            case 2:
                return build(text, FIBONACCI, minus(range(4, 150), FIBONACCI), random);
            case 3:
                return build(text, TRIANGULAR, minus(range(2, 60), TRIANGULAR), random);
            case 4:
                return build(text, CUBES, minus(range(2, 250), CUBES), random);
            default: {
                int m = HARD_MULTIPLES[random.nextInt(HARD_MULTIPLES.length)];
                return multiples(text, m, random);
            }
        }
    }

    private static QuizTaskView.Question multiples(String text, int m, Random random) {
        long[] group = step(m * 2L, m * 14L, m);
        long[] outliers = minus(range(7, m * 14L), step(m, m * 14L, m));
        return build(text, group, outliers, random);
    }

    private static QuizTaskView.Question build(String text, long[] group, long[] outliers,
                                               Random random) {
        List<Long> pool = new ArrayList<>();
        for (long value : group) {
            pool.add(value);
        }
        Collections.shuffle(pool, random);
        long odd = outliers[random.nextInt(outliers.length)];

        List<String> options = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            options.add(String.valueOf(pool.get(i)));
        }
        String answer = String.valueOf(odd);
        options.add(answer);
        Collections.shuffle(options, random);
        return new QuizTaskView.Question(text, options, options.indexOf(answer));
    }

    private static long[] range(long low, long high) {
        return step(low, high, 1);
    }

    private static long[] step(long low, long high, long stride) {
        List<Long> values = new ArrayList<>();
        for (long v = low; v <= high; v += stride) {
            values.add(v);
        }
        return toArray(values);
    }

    private static long[] minus(long[] all, long[] remove) {
        Set<Long> blocked = new HashSet<>();
        for (long value : remove) {
            blocked.add(value);
        }
        List<Long> values = new ArrayList<>();
        for (long value : all) {
            if (!blocked.contains(value)) {
                values.add(value);
            }
        }
        return toArray(values);
    }

    private static long[] toArray(List<Long> values) {
        long[] result = new long[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }
}