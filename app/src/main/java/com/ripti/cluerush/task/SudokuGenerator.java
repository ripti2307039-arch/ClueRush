package com.ripti.cluerush.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class SudokuGenerator {

    private static final int CELLS = 16;

    public static final class Puzzle {
        public final int[] cells;
        public final int[] solution;

        Puzzle(int[] cells, int[] solution) {
            this.cells = cells;
            this.solution = solution;
        }
    }

    private SudokuGenerator() {
    }

    public static Puzzle generate(Random random, int blanks) {
        int[] solution = new int[CELLS];
        fill(solution, 0, random);

        int[] puzzle = solution.clone();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < CELLS; i++) {
            order.add(i);
        }
        Collections.shuffle(order, random);

        int removed = 0;
        for (int index : order) {
            if (removed >= blanks) {
                break;
            }
            int backup = puzzle[index];
            puzzle[index] = 0;
            if (countSolutions(puzzle.clone(), 2) == 1) {
                removed++;
            } else {
                puzzle[index] = backup;
            }
        }
        return new Puzzle(puzzle, solution);
    }

    private static boolean fill(int[] grid, int pos, Random random) {
        if (pos == CELLS) {
            return true;
        }
        List<Integer> numbers = new ArrayList<>();
        for (int n = 1; n <= 4; n++) {
            numbers.add(n);
        }
        Collections.shuffle(numbers, random);
        for (int n : numbers) {
            if (canPlace(grid, pos, n)) {
                grid[pos] = n;
                if (fill(grid, pos + 1, random)) {
                    return true;
                }
                grid[pos] = 0;
            }
        }
        return false;
    }

    private static int countSolutions(int[] grid, int limit) {
        int pos = -1;
        for (int i = 0; i < CELLS; i++) {
            if (grid[i] == 0) {
                pos = i;
                break;
            }
        }
        if (pos < 0) {
            return 1;
        }
        int count = 0;
        for (int n = 1; n <= 4; n++) {
            if (canPlace(grid, pos, n)) {
                grid[pos] = n;
                count += countSolutions(grid, limit - count);
                grid[pos] = 0;
                if (count >= limit) {
                    return count;
                }
            }
        }
        return count;
    }

    private static boolean canPlace(int[] grid, int pos, int n) {
        int row = pos / 4;
        int col = pos % 4;
        for (int i = 0; i < 4; i++) {
            if (grid[row * 4 + i] == n || grid[i * 4 + col] == n) {
                return false;
            }
        }
        int boxRow = (row / 2) * 2;
        int boxCol = (col / 2) * 2;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                if (grid[(boxRow + r) * 4 + boxCol + c] == n) {
                    return false;
                }
            }
        }
        return true;
    }
}