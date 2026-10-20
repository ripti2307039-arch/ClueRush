package com.ripti.cluerush.task;

import android.content.Context;
import android.util.Log;

import com.ripti.cluerush.R;
import com.ripti.cluerush.model.Difficulty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class TaskFactory {

    private static final String TAG = "TaskFactory";
    private static final String WORDS_FILE = "tasks/words.txt";
    private static final String ODD_FILE = "tasks/odd.txt";
    private static final int GENERATION_GUARD = 300;

    private interface QuestionMaker {
        QuizTaskView.Question make();
    }

    private TaskFactory() {
    }

    public static BaseTaskView create(Context context, TaskSpec spec) {
        return create(context, spec, true);
    }

    public static BaseTaskView create(Context context, TaskSpec spec, boolean avoidRepeats) {
        Random random = new Random(spec.getSeed());
        Difficulty difficulty = spec.getDifficulty();
        switch (spec.getType()) {
            case SUDOKU:
                return createSudoku(context, difficulty, random);
            case WORD_SCRAMBLE:
                return createWords(context, difficulty, random, avoidRepeats);
            case MEMORY:
                return createMemory(context, difficulty, random);
            case SLIDING:
                return createSliding(context, difficulty, random);
            case MATH:
                return createMath(context, difficulty, random);
            case SEQUENCE:
                return createSequence(context, difficulty, random);
            case ODD_ONE_OUT:
                return createOdd(context, difficulty, random, avoidRepeats);
            case PATTERN_LOCK:
                return createPattern(context, difficulty, random);
            case CODE_BREAKER:
                return createCode(context, difficulty, random);
            case LIGHTS_OUT:
                return createLights(context, difficulty, random);
            case COLOR_CLASH:
                return createClash(context, difficulty, random);
            default:
                return null;
        }
    }

    private static BaseTaskView createSequence(Context context, Difficulty difficulty,
                                               Random random) {
        int count = byLevel(difficulty, 3, 4, 5);
        int pass = byLevel(difficulty, 2, 3, 4);
        List<QuizTaskView.Question> questions = new ArrayList<>();
        fill(questions, new HashSet<String>(), count,
                () -> SequenceGenerator.generate(difficulty, random));
        return new QuizTaskView(context, questions, Math.min(pass, questions.size()),
                R.string.task_puzzle_progress);
    }

    private static BaseTaskView createOdd(Context context, Difficulty difficulty,
                                          Random random, boolean avoidRepeats) {
        int count = byLevel(difficulty, 3, 4, 5);
        int pass = byLevel(difficulty, 2, 3, 4);
        int generatedCount = count / 2;
        String questionText = context.getString(R.string.task_odd_question);

        List<TaskAssets.LevelRow> rows = TaskAssets.pick(context, "odd",
                TaskAssets.load(context, ODD_FILE, 4), difficulty,
                count - generatedCount, random, avoidRepeats);
        List<QuizTaskView.Question> questions = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (TaskAssets.LevelRow row : rows) {
            List<String> options = new ArrayList<>(Arrays.asList(row.columns).subList(0, 4));
            String odd = options.get(0);
            Collections.shuffle(options, random);
            addUnique(questions, keys,
                    new QuizTaskView.Question(questionText, options, options.indexOf(odd)));
        }
        fill(questions, keys, count,
                () -> OddGenerator.generate(questionText, difficulty, random));
        Collections.shuffle(questions, random);
        return new QuizTaskView(context, questions, Math.min(pass, questions.size()),
                R.string.task_puzzle_progress);
    }

    private static BaseTaskView createSudoku(Context context, Difficulty difficulty,
                                             Random random) {
        SudokuGenerator.Puzzle puzzle;
        switch (difficulty) {
            case EASY:
                puzzle = SudokuGenerator.generate(random, 4, 2, 2, 6);
                break;
            case MEDIUM:
                puzzle = SudokuGenerator.generate(random, 6, 2, 3, 14);
                break;
            default:
                puzzle = SudokuGenerator.generate(random, 6, 2, 3, 20);
                break;
        }
        return new SudokuTaskView(context, puzzle);
    }

    private static BaseTaskView createWords(Context context, Difficulty difficulty,
                                            Random random, boolean avoidRepeats) {
        int count = byLevel(difficulty, 3, 4, 5);
        List<TaskAssets.LevelRow> rows = TaskAssets.pick(context, "words",
                TaskAssets.load(context, WORDS_FILE, 2), difficulty, count, random,
                avoidRepeats);
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

    private static BaseTaskView createMemory(Context context, Difficulty difficulty,
                                             Random random) {
        int pairs = byLevel(difficulty, 6, 8, 10);
        int columns = byLevel(difficulty, 4, 4, 5);
        int cardDp = byLevel(difficulty, 60, 54, 50);
        return new MemoryTaskView(context, pairs, columns, cardDp, random);
    }

    private static BaseTaskView createSliding(Context context, Difficulty difficulty,
                                              Random random) {
        int size = difficulty == Difficulty.HARD ? 4 : 3;
        int moves = byLevel(difficulty, 40, 90, 150);
        return new SlidingTaskView(context, size, moves, random);
    }

    private static BaseTaskView createPattern(Context context, Difficulty difficulty,
                                              Random random) {
        int columns = difficulty == Difficulty.HARD ? 4 : 3;
        int length = byLevel(difficulty, 4, 6, 8);
        int cellDp = columns == 4 ? 46 : 56;
        return new PatternLockTaskView(context, columns, length, cellDp, random);
    }

    private static BaseTaskView createCode(Context context, Difficulty difficulty,
                                           Random random) {
        int length = difficulty == Difficulty.EASY ? 3 : 4;
        int attempts = byLevel(difficulty, 8, 8, 7);
        return new CodeBreakerTaskView(context, length, attempts, random);
    }

    private static BaseTaskView createLights(Context context, Difficulty difficulty,
                                             Random random) {
        int size = byLevel(difficulty, 3, 4, 5);
        int cellDp = byLevel(difficulty, 64, 52, 42);
        int presses = byLevel(difficulty, 3, 5, 8);
        return new LightsOutTaskView(context, size, cellDp, presses, random);
    }

    private static BaseTaskView createClash(Context context, Difficulty difficulty,
                                            Random random) {
        int rounds = byLevel(difficulty, 6, 8, 10);
        int colors = byLevel(difficulty, 3, 4, 5);
        int allowed = byLevel(difficulty, 2, 2, 3);
        int wordRule = byLevel(difficulty, 0, 25, 40);
        return new ColorClashTaskView(context, rounds, colors, allowed, wordRule, random);
    }

    private static BaseTaskView createMath(Context context, Difficulty difficulty,
                                           Random random) {
        int count = byLevel(difficulty, 5, 6, 8);
        List<MathGenerator.Problem> problems = new ArrayList<>();
        Set<String> texts = new HashSet<>();
        int guard = 0;
        while (problems.size() < count && guard++ < GENERATION_GUARD) {
            MathGenerator.Problem problem = MathGenerator.generate(difficulty, random);
            if (texts.add(problem.getText())) {
                problems.add(problem);
            }
        }
        return new MathTaskView(context, problems);
    }

    private static void addUnique(List<QuizTaskView.Question> questions, Set<String> keys,
                                  QuizTaskView.Question question) {
        if (keys.add(keyOf(question))) {
            questions.add(question);
        }
    }

    private static void fill(List<QuizTaskView.Question> questions, Set<String> keys,
                             int count, QuestionMaker maker) {
        int guard = 0;
        while (questions.size() < count && guard++ < GENERATION_GUARD) {
            addUnique(questions, keys, maker.make());
        }
    }

    private static String keyOf(QuizTaskView.Question question) {
        List<String> sorted = new ArrayList<>(question.options);
        Collections.sort(sorted);
        return question.text + "#" + sorted;
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