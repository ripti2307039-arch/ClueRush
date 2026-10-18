package com.ripti.cluerush.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class SudokuGenerator {

    public static final class Puzzle {
        public final int size;
        public final int boxRows;
        public final int boxCols;
        public final int[] cells;
        public final int[] solution;

        Puzzle(int size, int boxRows, int boxCols, int[] cells, int[] solution) {
            this.size = size;
            this.boxRows = boxRows;
            this.boxCols = boxCols;
            this.cells = cells;
            this.solution = solution;
        }
    }

    private SudokuGenerator() {
    }

    public static Puzzle generate(Random random, int size, int boxRows, int boxCols, int blanks) {
        int total = size * size;
        int[] solution = new int[total];
        fill(solution, 0, size, boxRows, boxCols, random);

        int[] puzzle = solution.clone();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < total; i++) {
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
            if (countSolutions(puzzle.clone(), 2, size, boxRows, boxCols) == 1) {
                removed++;
            } else {
                puzzle[index] = backup;
            }
        }
        return new Puzzle(size, boxRows, boxCols, puzzle, solution);
    }

    private static boolean fill(int[] grid, int pos, int size, int boxRows, int boxCols,
                                Random random) {
        if (pos == grid.length) {
            return true;
        }
        List<Integer> numbers = new ArrayList<>();
        for (int n = 1; n <= size; n++) {
            numbers.add(n);
        }
        Collections.shuffle(numbers, random);
        for (int n : numbers) {
            if (canPlace(grid, pos, n, size, boxRows, boxCols)) {
                grid[pos] = n;
                if (fill(grid, pos + 1, size, boxRows, boxCols, random)) {
                    return true;
                }
                grid[pos] = 0;
            }
        }
        return false;
    }

    private static int countSolutions(int[] grid, int limit, int size, int boxRows,
                                      int boxCols) {
        int best = -1;
        int bestCount = size + 1;
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != 0) {
                continue;
            }
            int options = 0;
            for (int n = 1; n <= size; n++) {
                if (canPlace(grid, i, n, size, boxRows, boxCols)) {
                    options++;
                }
            }
            if (options == 0) {
                return 0;
            }
            if (options < bestCount) {
                bestCount = options;
                best = i;
            }
        }
        if (best < 0) {
            return 1;
        }
        int count = 0;
        for (int n = 1; n <= size; n++) {
            if (canPlace(grid, best, n, size, boxRows, boxCols)) {
                grid[best] = n;
                count += countSolutions(grid, limit - count, size, boxRows, boxCols);
                grid[best] = 0;
                if (count >= limit) {
                    return count;
                }
            }
        }
        return count;
    }

    private static boolean canPlace(int[] grid, int pos, int n, int size, int boxRows,
                                    int boxCols) {
        int row = pos / size;
        int col = pos % size;
        for (int i = 0; i < size; i++) {
            if (grid[row * size + i] == n || grid[i * size + col] == n) {
                return false;
            }
        }
        int startRow = (row / boxRows) * boxRows;
        int startCol = (col / boxCols) * boxCols;
        for (int r = 0; r < boxRows; r++) {
            for (int c = 0; c < boxCols; c++) {
                if (grid[(startRow + r) * size + startCol + c] == n) {
                    return false;
                }
            }
        }
        return true;
    }
}