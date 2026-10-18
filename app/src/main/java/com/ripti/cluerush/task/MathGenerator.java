package com.ripti.cluerush.task;

import com.ripti.cluerush.model.Difficulty;

import java.util.Random;

public final class MathGenerator {

    private static final String TIMES = "\u00D7";
    private static final String MINUS = "\u2212";
    private static final String DIVIDE = "\u00F7";

    public static final class Problem {
        final String text;
        final long answer;

        Problem(String text, long answer) {
            this.text = text;
            this.answer = answer;
        }

        public String getText() {
            return text;
        }

        public long getAnswer() {
            return answer;
        }
    }

    private MathGenerator() {
    }

    public static Problem generate(Difficulty difficulty, Random random) {
        switch (difficulty) {
            case EASY:
                return easy(random);
            case MEDIUM:
                return medium(random);
            default:
                return hard(random);
        }
    }

    private static Problem easy(Random random) {
        if (random.nextBoolean()) {
            int a = ChoiceBuilder.between(random, 10, 60);
            int b = ChoiceBuilder.between(random, 10, 60);
            return new Problem(a + " + " + b, a + b);
        }
        int a = ChoiceBuilder.between(random, 30, 99);
        int b = ChoiceBuilder.between(random, 10, a - 5);
        return new Problem(a + " " + MINUS + " " + b, a - b);
    }

    private static Problem medium(Random random) {
        switch (random.nextInt(3)) {
            case 0: {
                int a = ChoiceBuilder.between(random, 30, 250);
                int b = ChoiceBuilder.between(random, 30, 250);
                return new Problem(a + " + " + b, a + b);
            }
            case 1: {
                int a = ChoiceBuilder.between(random, 100, 400);
                int b = ChoiceBuilder.between(random, 20, a - 20);
                return new Problem(a + " " + MINUS + " " + b, a - b);
            }
            default: {
                int a = ChoiceBuilder.between(random, 6, 15);
                int b = ChoiceBuilder.between(random, 3, 9);
                return new Problem(a + " " + TIMES + " " + b, a * b);
            }
        }
    }

    private static Problem hard(Random random) {
        switch (random.nextInt(6)) {
            case 0: {
                int a = ChoiceBuilder.between(random, 12, 29);
                int b = ChoiceBuilder.between(random, 4, 12);
                return new Problem(a + " " + TIMES + " " + b, a * b);
            }
            case 1: {
                int b = ChoiceBuilder.between(random, 3, 12);
                int q = ChoiceBuilder.between(random, 6, 25);
                return new Problem((b * q) + " " + DIVIDE + " " + b, q);
            }
            case 2: {
                int a = ChoiceBuilder.between(random, 3, 12);
                int b = ChoiceBuilder.between(random, 3, 12);
                int c = ChoiceBuilder.between(random, 5, 60);
                return new Problem(a + " " + TIMES + " " + b + " + " + c, a * b + c);
            }
            case 3: {
                int a = ChoiceBuilder.between(random, 10, 60);
                int b = ChoiceBuilder.between(random, 3, 9);
                int c = ChoiceBuilder.between(random, 3, 9);
                return new Problem(a + " + " + b + " " + TIMES + " " + c, a + b * c);
            }
            case 4: {
                int a = ChoiceBuilder.between(random, 5, 12);
                int b = ChoiceBuilder.between(random, 5, 12);
                int c = ChoiceBuilder.between(random, 5, a * b - 10);
                return new Problem(a + " " + TIMES + " " + b + " " + MINUS + " " + c,
                        a * b - c);
            }
            default: {
                int a = ChoiceBuilder.between(random, 100, 499);
                int b = ChoiceBuilder.between(random, 100, 499);
                return new Problem(a + " + " + b, a + b);
            }
        }
    }
}