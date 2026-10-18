package com.ripti.cluerush.task;

import com.ripti.cluerush.model.Difficulty;

import java.util.Random;

final class SequenceGenerator {

    private static final long[] PRIMES = {
            2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61
    };

    private SequenceGenerator() {
    }

    static QuizTaskView.Question generate(Difficulty difficulty, Random random) {
        int length;
        long[] terms;
        switch (difficulty) {
            case EASY:
                length = 5;
                terms = easy(random, length);
                break;
            case MEDIUM:
                length = 6;
                terms = medium(random, length);
                break;
            default:
                length = 7;
                terms = hard(random, length);
                break;
        }
        int shown = length - 1;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            text.append(terms[i]).append(", ");
        }
        text.append("?");
        long lastStep = Math.abs(terms[shown] - terms[shown - 1]);
        return ChoiceBuilder.numeric(text.toString(), terms[shown], lastStep, random);
    }

    private static long[] easy(Random random, int n) {
        switch (random.nextInt(3)) {
            case 0:
                return arithmetic(random, n, 2, 9, false);
            case 1:
                return arithmetic(random, n, 2, 9, true);
            default:
                return geometric(random, n, 2, 3);
        }
    }

    private static long[] medium(Random random, int n) {
        switch (random.nextInt(5)) {
            case 0:
                return geometric(random, n, 2 + random.nextInt(2), 4);
            case 1:
                return squares(random, n);
            case 2:
                return triangular(random, n);
            case 3:
                return alternatingAdd(random, n);
            default:
                return arithmetic(random, n, 6, 15, random.nextBoolean());
        }
    }

    private static long[] hard(Random random, int n) {
        switch (random.nextInt(6)) {
            case 0:
                return fibonacciLike(random, n);
            case 1:
                return quadratic(random, n);
            case 2:
                return cubes(random, n);
            case 3:
                return multiplyAdd(random, n);
            case 4:
                return interleaved(random, n);
            default:
                return primes(random, n);
        }
    }

    private static long[] arithmetic(Random random, int n, int minStep, int maxStep,
                                     boolean down) {
        long step = ChoiceBuilder.between(random, minStep, maxStep);
        long start = down
                ? step * n + ChoiceBuilder.between(random, 5, 25)
                : ChoiceBuilder.between(random, 1, 20);
        long[] terms = new long[n];
        for (int i = 0; i < n; i++) {
            terms[i] = down ? start - i * step : start + i * step;
        }
        return terms;
    }

    private static long[] geometric(Random random, int n, int ratio, int maxStart) {
        long[] terms = new long[n];
        terms[0] = ChoiceBuilder.between(random, 1, maxStart);
        for (int i = 1; i < n; i++) {
            terms[i] = terms[i - 1] * ratio;
        }
        return terms;
    }

    private static long[] squares(Random random, int n) {
        int k = ChoiceBuilder.between(random, 1, 7);
        long[] terms = new long[n];
        for (int i = 0; i < n; i++) {
            terms[i] = (long) (k + i) * (k + i);
        }
        return terms;
    }

    private static long[] triangular(Random random, int n) {
        int k = ChoiceBuilder.between(random, 1, 5);
        long[] terms = new long[n];
        for (int i = 0; i < n; i++) {
            long m = k + i;
            terms[i] = m * (m + 1) / 2;
        }
        return terms;
    }

    private static long[] alternatingAdd(Random random, int n) {
        long a = ChoiceBuilder.between(random, 2, 6);
        long b = ChoiceBuilder.between(random, 7, 12);
        long[] terms = new long[n];
        terms[0] = ChoiceBuilder.between(random, 1, 15);
        for (int i = 0; i < n - 1; i++) {
            terms[i + 1] = terms[i] + (i % 2 == 0 ? a : b);
        }
        return terms;
    }

    private static long[] fibonacciLike(Random random, int n) {
        long[] terms = new long[n];
        terms[0] = ChoiceBuilder.between(random, 1, 5);
        terms[1] = ChoiceBuilder.between(random, 1, 6);
        for (int i = 2; i < n; i++) {
            terms[i] = terms[i - 1] + terms[i - 2];
        }
        return terms;
    }

    private static long[] quadratic(Random random, int n) {
        long d = ChoiceBuilder.between(random, 1, 4);
        long k = ChoiceBuilder.between(random, 1, 3);
        long[] terms = new long[n];
        terms[0] = ChoiceBuilder.between(random, 1, 10);
        for (int i = 0; i < n - 1; i++) {
            terms[i + 1] = terms[i] + d + i * k;
        }
        return terms;
    }

    private static long[] cubes(Random random, int n) {
        int k = ChoiceBuilder.between(random, 1, 3);
        long[] terms = new long[n];
        for (int i = 0; i < n; i++) {
            long m = k + i;
            terms[i] = m * m * m;
        }
        return terms;
    }

    private static long[] multiplyAdd(Random random, int n) {
        long multiplier = 2 + random.nextInt(2);
        long add = ChoiceBuilder.between(random, 1, 5);
        long[] terms = new long[n];
        terms[0] = ChoiceBuilder.between(random, 1, 4);
        for (int i = 0; i < n - 1; i++) {
            terms[i + 1] = i % 2 == 0 ? terms[i] * multiplier : terms[i] + add;
        }
        return terms;
    }

    private static long[] interleaved(Random random, int n) {
        long a = ChoiceBuilder.between(random, 1, 9);
        long b = ChoiceBuilder.between(random, 20, 40);
        long x = ChoiceBuilder.between(random, 2, 5);
        long y = ChoiceBuilder.between(random, 3, 8);
        long[] terms = new long[n];
        for (int i = 0; i < n; i++) {
            terms[i] = i % 2 == 0 ? a + (i / 2) * x : b + (i / 2) * y;
        }
        return terms;
    }

    private static long[] primes(Random random, int n) {
        int offset = random.nextInt(PRIMES.length - n + 1);
        long[] terms = new long[n];
        System.arraycopy(PRIMES, offset, terms, 0, n);
        return terms;
    }
}