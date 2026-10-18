package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SlidingTaskView extends BaseTaskView {

    private final int size;
    private final int total;
    private final int[] values;
    private final TextView[] tiles;
    private int blank;

    public SlidingTaskView(Context context, int size, int shuffleMoves, Random random) {
        super(context);
        this.size = size;
        total = size * size;
        values = new int[total];
        tiles = new TextView[total];
        for (int i = 0; i < total - 1; i++) {
            values[i] = i + 1;
        }
        blank = total - 1;
        scramble(shuffleMoves, random);

        int tileSize = size == 3 ? 64 : 52;
        float textSp = size == 3 ? 24f : 20f;

        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < size; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < size; c++) {
                final int pos = r * size + c;
                TextView tile = label("", textSp, color(R.color.navy_950), true);
                tile.setOnClickListener(v -> onTile(pos));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(dp(tileSize), dp(tileSize));
                params.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(tile, params);
                tiles[pos] = tile;
            }
            grid.addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
        }
        addView(grid, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        refresh();
    }

    @Override
    public int getMistakes() {
        return 0;
    }

    private void scramble(int moves, Random random) {
        int previous = -1;
        for (int i = 0; i < moves || isSolved(); i++) {
            int row = blank / size;
            int col = blank % size;
            List<Integer> options = new ArrayList<>(4);
            if (row > 0) {
                options.add(blank - size);
            }
            if (row < size - 1) {
                options.add(blank + size);
            }
            if (col > 0) {
                options.add(blank - 1);
            }
            if (col < size - 1) {
                options.add(blank + 1);
            }
            options.remove(Integer.valueOf(previous));
            int target = options.get(random.nextInt(options.size()));
            values[blank] = values[target];
            values[target] = 0;
            previous = blank;
            blank = target;
        }
    }

    private void onTile(int pos) {
        if (isDone() || pos == blank) {
            return;
        }
        int blankRow = blank / size;
        int blankCol = blank % size;
        int row = pos / size;
        int col = pos % size;
        if (row != blankRow && col != blankCol) {
            return;
        }
        int step = row == blankRow ? (col > blankCol ? 1 : -1) : (row > blankRow ? size : -size);
        int current = blank;
        while (current != pos) {
            values[current] = values[current + step];
            current += step;
        }
        values[pos] = 0;
        blank = pos;
        refresh();
        if (isSolved()) {
            finishTask(true);
        }
    }

    private boolean isSolved() {
        for (int i = 0; i < total - 1; i++) {
            if (values[i] != i + 1) {
                return false;
            }
        }
        return true;
    }

    private void refresh() {
        int gold = color(R.color.treasure_gold);
        int mint = color(R.color.mint);
        for (int i = 0; i < total; i++) {
            TextView tile = tiles[i];
            int value = values[i];
            if (value == 0) {
                tile.setText("");
                tile.setBackground(box(color(R.color.hud_scrim), color(R.color.hud_stroke),
                        10, 1f));
            } else {
                int fill = value == i + 1 ? mint : gold;
                tile.setText(String.valueOf(value));
                tile.setBackground(box(fill, fill, 10, 0f));
            }
        }
    }
}