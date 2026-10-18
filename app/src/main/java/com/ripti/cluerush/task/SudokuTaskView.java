package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.Arrays;

public class SudokuTaskView extends BaseTaskView {

    private final int size;
    private final int boxRows;
    private final int boxCols;
    private final int[] solution;
    private final int[] values;
    private final boolean[] given;
    private final boolean[] wrong;
    private final TextView[] cells;
    private final LinearLayout grid;
    private int selected = -1;
    private int mistakes;

    public SudokuTaskView(Context context, SudokuGenerator.Puzzle puzzle) {
        super(context);
        size = puzzle.size;
        boxRows = puzzle.boxRows;
        boxCols = puzzle.boxCols;
        int total = size * size;
        solution = puzzle.solution.clone();
        values = puzzle.cells.clone();
        given = new boolean[total];
        wrong = new boolean[total];
        cells = new TextView[total];
        for (int i = 0; i < total; i++) {
            given[i] = puzzle.cells[i] != 0;
            if (selected < 0 && !given[i]) {
                selected = i;
            }
        }

        int cellSize = size == 4 ? 46 : 34;
        float textSp = size == 4 ? 22f : 18f;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);

        grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < size; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < size; c++) {
                final int pos = r * size + c;
                TextView cell = label("", textSp, color(R.color.white), true);
                cell.setOnClickListener(v -> select(pos));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(dp(cellSize), dp(cellSize));
                params.setMargins(dp(2), dp(2), dp(gap(c, boxCols)), dp(gap(r, boxRows)));
                row.addView(cell, params);
                cells[pos] = cell;
            }
            grid.addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
        }
        root.addView(grid, new LinearLayout.LayoutParams(WRAP, WRAP));

        LinearLayout.LayoutParams padParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        padParams.setMarginStart(dp(20));
        root.addView(buildPad(context), padParams);

        addView(root, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        refresh();
    }

    @Override
    public int getMistakes() {
        return mistakes;
    }

    private int gap(int index, int boxLength) {
        return (index + 1) % boxLength == 0 && index < size - 1 ? 8 : 2;
    }

    private LinearLayout buildPad(Context context) {
        int columns = size == 4 ? 2 : 3;
        int rows = size / columns;
        int button = size == 4 ? 46 : 40;

        LinearLayout pad = new LinearLayout(context);
        pad.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < columns; c++) {
                final int number = r * columns + c + 1;
                TextView key = label(String.valueOf(number), 22,
                        color(R.color.treasure_gold), true);
                key.setBackground(box(color(R.color.navy_900), color(R.color.treasure_gold),
                        12, 1.5f));
                key.setOnClickListener(v -> place(number));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(dp(button), dp(button));
                params.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(key, params);
            }
            pad.addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
        }

        TextView erase = label(context.getString(R.string.task_erase), 14,
                color(R.color.white), true);
        erase.setBackground(box(color(R.color.navy_900), color(R.color.hud_stroke), 12, 1.5f));
        erase.setOnClickListener(v -> place(0));
        LinearLayout.LayoutParams eraseParams = new LinearLayout.LayoutParams(
                dp((button + 6) * columns - 6), dp(button));
        eraseParams.setMargins(dp(3), dp(3), dp(3), dp(3));
        pad.addView(erase, eraseParams);
        return pad;
    }

    private void select(int pos) {
        if (isDone() || given[pos]) {
            return;
        }
        selected = pos;
        refresh();
    }

    private void place(int number) {
        if (isDone() || selected < 0 || given[selected]) {
            return;
        }
        values[selected] = number;
        Arrays.fill(wrong, false);
        refresh();
        checkComplete();
    }

    private void checkComplete() {
        for (int value : values) {
            if (value == 0) {
                return;
            }
        }
        boolean allRight = true;
        for (int i = 0; i < values.length; i++) {
            wrong[i] = values[i] != solution[i];
            if (wrong[i]) {
                allRight = false;
            }
        }
        if (allRight) {
            selected = -1;
            refresh();
            finishTask(true);
        } else {
            mistakes++;
            shake(grid);
            refresh();
        }
    }

    private void refresh() {
        int gold = color(R.color.treasure_gold);
        for (int i = 0; i < values.length; i++) {
            TextView cell = cells[i];
            cell.setText(values[i] == 0 ? "" : String.valueOf(values[i]));
            boolean isSelected = i == selected;
            int textColor = given[i] ? color(R.color.white) : (wrong[i] ? ERROR_RED : gold);
            int fill = given[i] ? color(R.color.navy_900) : color(R.color.hud_scrim);
            int stroke = isSelected ? gold : color(R.color.hud_stroke);
            cell.setTextColor(textColor);
            cell.setBackground(box(fill, stroke, 8, isSelected ? 2.5f : 1f));
        }
    }
}