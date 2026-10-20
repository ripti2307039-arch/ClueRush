package com.ripti.cluerush.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class CodeGenerator {

    private CodeGenerator() {
    }

    static int[] generate(Random random, int length) {
        List<Integer> digits = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            digits.add(i);
        }
        Collections.shuffle(digits, random);
        int[] code = new int[length];
        for (int i = 0; i < length; i++) {
            code[i] = digits.get(i);
        }
        return code;
    }

    static int exact(int[] code, int[] guess) {
        int count = 0;
        for (int i = 0; i < code.length; i++) {
            if (code[i] == guess[i]) {
                count++;
            }
        }
        return count;
    }

    static int misplaced(int[] code, int[] guess) {
        int count = 0;
        for (int i = 0; i < guess.length; i++) {
            if (code[i] == guess[i]) {
                continue;
            }
            for (int digit : code) {
                if (digit == guess[i]) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }
}