package com.ripti.cluerush.task;

import com.ripti.cluerush.model.Difficulty;

import java.util.Locale;
import java.util.Random;

final class QuizGenerator {

    private static final String TIMES = "\u00D7";
    private static final String MINUS = "\u2212";
    private static final int[] PERCENTS = {10, 20, 25, 50, 75};

    private QuizGenerator() {
    }

    static QuizTaskView.Question generate(Difficulty difficulty, Random random) {
        switch (difficulty) {
            case EASY:
                return easy(random);
            case MEDIUM:
                return medium(random);
            default:
                return hard(random);
        }
    }

    private static QuizTaskView.Question easy(Random random) {
        switch (random.nextInt(3)) {
            case 0: {
                int a = ChoiceBuilder.between(random, 11, 60);
                int b = ChoiceBuilder.between(random, 11, 60);
                return ChoiceBuilder.numeric("What is " + a + " + " + b + "?", a + b, 1, random);
            }
            case 1: {
                int a = ChoiceBuilder.between(random, 30, 99);
                int b = ChoiceBuilder.between(random, 11, a - 5);
                return ChoiceBuilder.numeric("What is " + a + " " + MINUS + " " + b + "?",
                        a - b, 1, random);
            }
            default: {
                int a = ChoiceBuilder.between(random, 2, 12);
                int b = ChoiceBuilder.between(random, 2, 12);
                return ChoiceBuilder.numeric("What is " + a + " " + TIMES + " " + b + "?",
                        a * b, Math.max(a, b), random);
            }
        }
    }

    private static QuizTaskView.Question medium(Random random) {
        switch (random.nextInt(5)) {
            case 0: {
                int a = ChoiceBuilder.between(random, 12, 25);
                int b = ChoiceBuilder.between(random, 3, 9);
                return ChoiceBuilder.numeric("What is " + a + " " + TIMES + " " + b + "?",
                        a * b, a, random);
            }
            case 1: {
                int a = ChoiceBuilder.between(random, 100, 499);
                int b = ChoiceBuilder.between(random, 100, 499);
                return ChoiceBuilder.numeric("What is " + a + " + " + b + "?", a + b, 10, random);
            }
            case 2: {
                int n = ChoiceBuilder.between(random, 8, 63);
                return ChoiceBuilder.binary("What is decimal " + n + " in binary?",
                        Integer.toBinaryString(n), random);
            }
            case 3: {
                int n = ChoiceBuilder.between(random, 8, 63);
                return ChoiceBuilder.numeric(
                        "What is binary " + Integer.toBinaryString(n) + " in decimal?",
                        n, 1, random);
            }
            default: {
                int percent = PERCENTS[random.nextInt(PERCENTS.length)];
                int number = 20 * ChoiceBuilder.between(random, 2, 20);
                return ChoiceBuilder.numeric(
                        "What is " + percent + "% of " + number + "?",
                        number * percent / 100, 5, random);
            }
        }
    }

    private static QuizTaskView.Question hard(Random random) {
        switch (random.nextInt(7)) {
            case 0: {
                int a = ChoiceBuilder.between(random, 21, 49);
                int b = ChoiceBuilder.between(random, 6, 19);
                return ChoiceBuilder.numeric("What is " + a + " " + TIMES + " " + b + "?",
                        a * b, a, random);
            }
            case 1: {
                int n = ChoiceBuilder.between(random, 64, 255);
                return ChoiceBuilder.binary("What is decimal " + n + " in binary?",
                        Integer.toBinaryString(n), random);
            }
            case 2: {
                int n = ChoiceBuilder.between(random, 64, 255);
                return ChoiceBuilder.numeric(
                        "What is binary " + Integer.toBinaryString(n) + " in decimal?",
                        n, 1, random);
            }
            case 3: {
                int n = ChoiceBuilder.between(random, 32, 255);
                return ChoiceBuilder.numeric(
                        "What is hexadecimal " + hexOf(n) + " in decimal?", n, 16, random);
            }
            case 4: {
                int n = ChoiceBuilder.between(random, 32, 255);
                return ChoiceBuilder.hex("What is decimal " + n + " in hexadecimal?",
                        hexOf(n), random);
            }
            case 5: {
                int a = ChoiceBuilder.between(random, 50, 199);
                int m = ChoiceBuilder.between(random, 3, 13);
                return ChoiceBuilder.numeric(
                        "What is the remainder when " + a + " is divided by " + m + "?",
                        a % m, 1, random);
            }
            default: {
                int n = ChoiceBuilder.between(random, 13, 29);
                return ChoiceBuilder.numeric("What is " + n + "\u00B2?", (long) n * n, n, random);
            }
        }
    }

    private static String hexOf(int value) {
        return Integer.toHexString(value).toUpperCase(Locale.ROOT);
    }
}