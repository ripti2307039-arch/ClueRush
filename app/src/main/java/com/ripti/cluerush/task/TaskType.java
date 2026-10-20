package com.ripti.cluerush.task;

import com.ripti.cluerush.R;
import com.ripti.cluerush.model.Difficulty;

public enum TaskType {
    SUDOKU(R.string.task_sudoku_title, R.string.task_sudoku_hint),
    WORD_SCRAMBLE(R.string.task_word_title, R.string.task_word_hint),
    MEMORY(R.string.task_memory_title, R.string.task_memory_hint),
    SLIDING(R.string.task_sliding_title, R.string.task_sliding_hint),
    MATH(R.string.task_math_title, R.string.task_math_hint),
    SEQUENCE(R.string.task_sequence_title, R.string.task_sequence_hint),
    ODD_ONE_OUT(R.string.task_odd_title, R.string.task_odd_hint),
    PATTERN_LOCK(R.string.task_pattern_title, R.string.task_pattern_hint),
    CODE_BREAKER(R.string.task_code_title, R.string.task_code_hint),
    LIGHTS_OUT(R.string.task_lights_title, R.string.task_lights_hint),
    COLOR_CLASH(R.string.task_clash_title, R.string.task_clash_hint);

    private final int titleRes;
    private final int hintRes;

    TaskType(int titleRes, int hintRes) {
        this.titleRes = titleRes;
        this.hintRes = hintRes;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public int getHintRes() {
        return hintRes;
    }

    public int timeLimitSeconds(Difficulty difficulty) {
        switch (this) {
            case SUDOKU:
                return byLevel(difficulty, 120, 180, 240);
            case WORD_SCRAMBLE:
                return byLevel(difficulty, 75, 100, 130);
            case MEMORY:
                return byLevel(difficulty, 60, 90, 120);
            case SLIDING:
                return byLevel(difficulty, 90, 120, 180);
            case MATH:
                return byLevel(difficulty, 60, 75, 90);
            case SEQUENCE:
                return byLevel(difficulty, 45, 60, 75);
            case ODD_ONE_OUT:
                return byLevel(difficulty, 40, 50, 60);
            case PATTERN_LOCK:
                return byLevel(difficulty, 60, 75, 90);
            case CODE_BREAKER:
                return byLevel(difficulty, 90, 120, 150);
            case LIGHTS_OUT:
                return byLevel(difficulty, 60, 90, 150);
            default:
                return byLevel(difficulty, 30, 40, 45);
        }
    }

    private static int byLevel(Difficulty difficulty, int easy, int medium, int hard) {
        switch (difficulty) {
            case EASY:
                return easy;
            case MEDIUM:
                return medium;
            default:
                return hard;
        }
    }
}