package com.ripti.cluerush.task;

import android.content.Context;
import android.util.Log;

import com.ripti.cluerush.model.Difficulty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class TaskFactory {

    private static final String TAG = "TaskFactory";
    private static final String QUIZ_FILE = "tasks/quiz.txt";
    private static final String WORDS_FILE = "tasks/words.txt";

    private TaskFactory() {
    }

    /** Content na pele null ferot dey. Caller task skip kore. */
    public static BaseTaskView create(Context context, TaskSpec spec) {
        Random random = new Random(spec.getSeed());
        Difficulty difficulty = spec.getDifficulty();
        switch (spec.getType()) {
            case QUIZ:
                return createQuiz(context, difficulty, random);
            case SUDOKU:
                return createSudoku(context, difficulty, random);
            case WORD_SCRAMBLE:
                return createWords(context, difficulty, random);
            case MEMORY:
                return createMemory(context, difficulty, random);
            default:
                return null;
        }
    }

    private static BaseTaskView createQuiz(Context context, Difficulty difficulty, Random random) {
        int count = byLevel(difficulty, 3, 4, 5);
        int pass = byLevel(difficulty, 2, 3, 4);
        List<TaskAssets.LevelRow> rows = TaskAssets.pick(
                TaskAssets.load(context, QUIZ_FILE, 3), difficulty, count, random);
        if (rows.isEmpty()) {
            Log.e(TAG, "No quiz questions found in " + QUIZ_FILE);
            return null;
        }
        List<QuizTaskView.Question> questions = new ArrayList<>();
        for (TaskAssets.LevelRow row : rows) {
            int end = Math.min(row.columns.length, 5);
            List<String> options = new ArrayList<>(Arrays.asList(row.columns).subList(1, end));
            String correct = options.get(0);
            Collections.shuffle(options, random);
            questions.add(new QuizTaskView.Question(row.columns[0], options, options.indexOf(correct)));
        }
        return new QuizTaskView(context, questions, Math.min(pass, questions.size()));
    }

    private static BaseTaskView createSudoku(Context context, Difficulty difficulty, Random random) {
        int blanks = byLevel(difficulty, 6, 9, 11);
        SudokuGenerator.Puzzle puzzle = SudokuGenerator.generate(random, blanks);
        return new SudokuTaskView(context, puzzle.cells, puzzle.solution);
    }

    private static BaseTaskView createWords(Context context, Difficulty difficulty, Random random) {
        int count = byLevel(difficulty, 1, 2, 3);
        List<TaskAssets.LevelRow> rows = TaskAssets.pick(
                TaskAssets.load(context, WORDS_FILE, 2), difficulty, count, random);
        List<WordScrambleView.Entry> entries = new ArrayList<>();
        for (TaskAssets.LevelRow row : rows) {
            String word = row.columns[0].replace(" ", "").toUpperCase(Locale.ROOT);
            if (word.length() >= 3) {
                entries.add(new WordScrambleView.Entry(word, row.columns[1]));
            }
        }
        if (entries.isEmpty()) {
            Log.e(TAG, "No words found in " + WORDS_FILE);
            return null;
        }
        return new WordScrambleView(context, entries, random);
    }

    private static BaseTaskView createMemory(Context context, Difficulty difficulty, Random random) {
        int pairs = byLevel(difficulty, 6, 8, 10);
        int columns = byLevel(difficulty, 4, 4, 5);
        int cardDp = byLevel(difficulty, 60, 54, 50);
        return new MemoryTaskView(context, pairs, columns, cardDp, random);
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