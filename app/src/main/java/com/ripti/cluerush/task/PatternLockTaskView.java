package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.Random;

public class PatternLockTaskView extends BaseTaskView {

    private static final long LIT_MS = 480L;
    private static final long GAP_MS = 220L;
    private static final long WRONG_MS = 750L;
    private static final long FIRST_SHOW_MS = 500L;

    private final int[] sequence;
    private final TextView[] cells;
    private final TextView statusText;
    private final Runnable stepper = this::playStep;
    private final Runnable restart = () -> startShow(300L);
    private int playIndex;
    private int progress;
    private int mistakes;
    private boolean lit;
    private boolean locked;

    public PatternLockTaskView(Context context, int columns, int length, int cellDp,
                               Random random) {
        super(context);
        int total = columns * columns;
        sequence = PatternGenerator.generate(random, total, length);
        cells = new TextView[total];

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);

        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < columns; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < columns; c++) {
                final int pos = r * columns + c;
                TextView cell = label("", 20, color(R.color.navy_950), true);
                cell.setOnClickListener(v -> onCell(pos));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(dp(cellDp), dp(cellDp));
                params.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(cell, params);
                cells[pos] = cell;
            }
            grid.addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
        }
        root.addView(grid, new LinearLayout.LayoutParams(WRAP, WRAP));

        LinearLayout side = new LinearLayout(context);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setGravity(Gravity.CENTER_HORIZONTAL);

        statusText = label("", 15, color(R.color.white), true);
        side.addView(statusText, new LinearLayout.LayoutParams(dp(170), WRAP));

        TextView again = label(context.getString(R.string.task_pattern_again), 13,
                color(R.color.treasure_gold), true);
        again.setBackground(box(color(R.color.navy_900), color(R.color.treasure_gold), 10, 1.5f));
        again.setOnClickListener(v -> showAgain());
        LinearLayout.LayoutParams againParams = new LinearLayout.LayoutParams(dp(120), dp(38));
        againParams.topMargin = dp(14);
        side.addView(again, againParams);

        LinearLayout.LayoutParams sideParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        sideParams.setMarginStart(dp(24));
        root.addView(side, sideParams);

        addView(root, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        startShow(FIRST_SHOW_MS);
    }

    @Override
    public int getMistakes() {
        return mistakes;
    }

    private void startShow(long delay) {
        removeCallbacks(stepper);
        removeCallbacks(restart);
        locked = true;
        progress = 0;
        playIndex = 0;
        lit = false;
        for (int i = 0; i < cells.length; i++) {
            paintIdle(i);
        }
        statusText.setText(R.string.task_pattern_watch);
        postDelayed(stepper, delay);
    }

    private void playStep() {
        if (isDone()) {
            return;
        }
        if (lit) {
            paintIdle(sequence[playIndex]);
            lit = false;
            playIndex++;
            if (playIndex >= sequence.length) {
                locked = false;
                showTurn();
                return;
            }
            postDelayed(stepper, GAP_MS);
        } else {
            TextView cell = cells[sequence[playIndex]];
            cell.setText(String.valueOf(playIndex + 1));
            cell.setBackground(box(color(R.color.treasure_gold), color(R.color.treasure_gold),
                    10, 0));
            lit = true;
            postDelayed(stepper, LIT_MS);
        }
    }

    private void showTurn() {
        statusText.setText(getContext().getString(R.string.task_pattern_turn,
                progress + 1, sequence.length));
    }

    private void onCell(int pos) {
        if (locked || isDone()) {
            return;
        }
        if (pos == sequence[progress]) {
            TextView cell = cells[pos];
            cell.setText(String.valueOf(progress + 1));
            cell.setBackground(box(color(R.color.mint), color(R.color.mint), 10, 0));
            progress++;
            if (progress >= sequence.length) {
                locked = true;
                finishTask(true);
                return;
            }
            showTurn();
        } else {
            mistakes++;
            locked = true;
            cells[pos].setBackground(box(ERROR_RED, ERROR_RED, 10, 0));
            shake(cells[pos]);
            statusText.setText(R.string.task_pattern_wrong);
            postDelayed(restart, WRONG_MS);
        }
    }

    private void showAgain() {
        if (locked || isDone()) {
            return;
        }
        mistakes++;
        startShow(200L);
    }

    private void paintIdle(int pos) {
        TextView cell = cells[pos];
        cell.setText("");
        cell.setBackground(box(color(R.color.navy_900), color(R.color.hud_stroke), 10, 1.5f));
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(stepper);
        removeCallbacks(restart);
        super.onDetachedFromWindow();
    }
}