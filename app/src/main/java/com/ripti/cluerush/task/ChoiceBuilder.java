package com.ripti.cluerush.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class ChoiceBuilder {

    private static final String HEX_DIGITS = "0123456789ABCDEF";
    private static final int WRONG_COUNT = 3;

    private ChoiceBuilder() {
    }

    static int between(Random random, int low, int high) {
        return low + random.nextInt(high - low + 1);
    }

    static QuizTaskView.Question numeric(String text, long correct, long extraStep,
                                         Random random) {
        Set<String> wrong = new LinkedHashSet<>();
        long[] steps = {1L, 2L, 3L, 5L, 10L, Math.max(1L, correct / 10L), Math.max(1L, extraStep)};
        int guard = 0;
        while (wrong.size() < WRONG_COUNT && guard++ < 200) {
            long step = steps[random.nextInt(steps.length)];
            long delta = step * (1 + random.nextInt(2));
            long candidate = random.nextBoolean() ? correct + delta : correct - delta;
            if (candidate >= 0L && candidate != correct) {
                wrong.add(String.valueOf(candidate));
            }
        }
        long filler = correct + 1L;
        while (wrong.size() < WRONG_COUNT) {
            filler++;
            wrong.add(String.valueOf(filler));
        }
        return assemble(text, String.valueOf(correct), wrong, random);
    }

    static QuizTaskView.Question binary(String text, String correct, Random random) {
        Set<String> wrong = new LinkedHashSet<>();
        int guard = 0;
        while (wrong.size() < WRONG_COUNT && guard++ < 100) {
            char[] bits = correct.toCharArray();
            int flips = 1 + random.nextInt(2);
            for (int i = 0; i < flips; i++) {
                int position = 1 + random.nextInt(bits.length - 1);
                bits[position] = bits[position] == '0' ? '1' : '0';
            }
            String candidate = new String(bits);
            if (!candidate.equals(correct)) {
                wrong.add(candidate);
            }
        }
        String filler = correct;
        while (wrong.size() < WRONG_COUNT) {
            filler = filler + "1";
            wrong.add(filler);
        }
        return assemble(text, correct, wrong, random);
    }

    static QuizTaskView.Question hex(String text, String correct, Random random) {
        Set<String> wrong = new LinkedHashSet<>();
        int guard = 0;
        while (wrong.size() < WRONG_COUNT && guard++ < 100) {
            char[] digits = correct.toCharArray();
            int position = random.nextInt(digits.length);
            int start = position == 0 ? 1 : 0;
            digits[position] = HEX_DIGITS.charAt(start + random.nextInt(16 - start));
            String candidate = new String(digits);
            if (!candidate.equals(correct)) {
                wrong.add(candidate);
            }
        }
        String filler = correct;
        while (wrong.size() < WRONG_COUNT) {
            filler = filler + "0";
            wrong.add(filler);
        }
        return assemble(text, correct, wrong, random);
    }

    private static QuizTaskView.Question assemble(String text, String correct,
                                                  Collection<String> wrong, Random random) {
        List<String> options = new ArrayList<>(wrong);
        options.add(correct);
        Collections.shuffle(options, random);
        return new QuizTaskView.Question(text, options, options.indexOf(correct));
    }
}