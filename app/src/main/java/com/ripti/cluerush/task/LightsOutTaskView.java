package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.Random;

public class LightsOutTaskView extends BaseTaskView {

    private final int size;
    private final boolean[] lit;
    private final TextView[] cells;
    private final TextView litText;
    private final TextView movesText;
    private int moves;

    public LightsOutTaskView(Context context, int size, int cellDp, int presses, Random random) {
        super(context);
        this.size = size;
        lit = LightsGenerator.generate(random, size, presses);
        cells = new TextView[size * size];

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);

        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < size; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < size; c++) {
                final int pos = r * size + c;
                TextView cell = label("", 14, color(R.color.navy_950), true);
                cell.setOnClickListener(v -> onCell(pos));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(dp(cellDp), dp(cellDp));
                params.setMargins(dp(2), dp(2), dp(2), dp(2));
                row.addView(cell, params);
                cells[pos] = cell;
            }
            grid.addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
        }
        root.addView(grid, new LinearLayout.LayoutParams(WRAP, WRAP));

        LinearLayout side = new LinearLayout(context);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setGravity(Gravity.CENTER_HORIZONTAL);
        litText = label("", 20, color(R.color.treasure_gold), true);
        movesText = label("", 14, color(R.color.hud_text_secondary), true);
        side.addView(litText, new LinearLayout.LayoutParams(dp(150), WRAP));
        LinearLayout.LayoutParams movesParams = new LinearLayout.LayoutParams(dp(150), WRAP);
        movesParams.topMargin = dp(6);
        side.addView(movesText, movesParams);

        LinearLayout.LayoutParams sideParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        sideParams.setMarginStart(dp(24));
        root.addView(side, sideParams);

        addView(root, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        refresh();
    }

    @Override
    public int getMistakes() {
        return 0;
    }

    private void onCell(int pos) {
        if (isDone()) {
            return;
        }
        LightsGenerator.press(lit, size, pos);
        moves++;
        refresh();
        if (LightsGenerator.countLit(lit) == 0) {
            finishTask(true);
        }
    }

    private void refresh() {
        for (int i = 0; i < cells.length; i++) {
            if (lit[i]) {
                cells[i].setBackground(box(color(R.color.treasure_gold),
                        color(R.color.treasure_gold), 8, 0));
            } else {
                cells[i].setBackground(box(color(R.color.navy_900),
                        color(R.color.hud_stroke), 8, 1.5f));
            }
        }
        litText.setText(getContext().getString(R.string.task_lights_on,
                LightsGenerator.countLit(lit)));
        movesText.setText(getContext().getString(R.string.task_lights_moves, moves));
    }
}