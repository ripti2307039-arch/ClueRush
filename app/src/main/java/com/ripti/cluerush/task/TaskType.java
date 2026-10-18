package com.ripti.cluerush.task;

import com.ripti.cluerush.R;
import com.ripti.cluerush.model.Difficulty;

public enum TaskType {
    QUIZ(R.string.task_quiz_title, R.string.task_quiz_hint),
    SUDOKU(R.string.task_sudoku_title, R.string.task_sudoku_hint),
    WORD_SCRAMBLE(R.string.task_word_title, R.string.task_word_hint),
    MEMORY(R.string.task_memory_title, R.string.task_memory_hint);

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

    /** Easy / Medium / Hard-e koto second shomoy pabe. */
    public int timeLimitSeconds(Difficulty difficulty) {
        switch (this) {
            case QUIZ:
                return byLevel(difficulty, 60, 50, 45);
            case SUDOKU:
                return byLevel(difficulty, 150, 120, 100);
            case WORD_SCRAMBLE:
                return byLevel(difficulty, 45, 60, 90);
            default:
                return byLevel(difficulty, 60, 90, 120);
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