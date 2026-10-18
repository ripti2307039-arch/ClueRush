package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.Arrays;

public class SudokuTaskView extends BaseTaskView {

    private static final int CELLS = 16;

    private final int[] solution;
    private final int[] values;
    private final boolean[] given = new boolean[CELLS];
    private final boolean[] wrong = new boolean[CELLS];
    private final TextView[] cells = new TextView[CELLS];
    private final LinearLayout grid;
    private int selected = -1;
    private int mistakes;

    public SudokuTaskView(Context context, int[] puzzle, int[] solution) {
        super(context);
        this.solution = solution.clone();
        this.values = puzzle.clone();
        for (int i = 0; i < CELLS; i++) {
            given[i] = puzzle[i] != 0;
            if (selected < 0 && !given[i]) {
                selected = i;
            }
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);

        grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        int cellSize = dp(46);
        for (int r = 0; r < 4; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 4; c++) {
                final int pos = r * 4 + c;
                TextView cell = label("", 22, color(R.color.white), true);
                cell.setOnClickListener(v -> select(pos));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(cellSize, cellSize);
                params.setMargins(dp(2), dp(2), dp(c == 1 ? 8 : 2), dp(r == 1 ? 8 : 2));
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

    private LinearLayout buildPad(Context context) {
        LinearLayout pad = new LinearLayout(context);
        pad.setOrientation(LinearLayout.VERTICAL);
        int size = dp(46);
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 2; col++) {
                final int number = rowIndex * 2 + col + 1;
                TextView button = label(String.valueOf(number), 22, color(R.color.treasure_gold), true);
                button.setBackground(box(color(R.color.navy_900), color(R.color.treasure_gold), 12, 1.5f));
                button.setOnClickListener(v -> place(number));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(button, params);
            }
            pad.addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
        }
        TextView erase = label(context.getString(R.string.task_erase), 14, color(R.color.white), true);
        erase.setBackground(box(color(R.color.navy_900), color(R.color.hud_stroke), 12, 1.5f));
        erase.setOnClickListener(v -> place(0));
        LinearLayout.LayoutParams eraseParams = new LinearLayout.LayoutParams(size * 2 + dp(6), size);
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
        for (int i = 0; i < CELLS; i++) {
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
        for (int i = 0; i < CELLS; i++) {
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