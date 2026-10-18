package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class WordScrambleView extends BaseTaskView {

    public static final class Entry {
        final String word;
        final String hint;

        public Entry(String word, String hint) {
            this.word = word;
            this.hint = hint;
        }
    }

    private static final long FEEDBACK_MS = 650L;
    private static final int PER_ROW = 6;

    private final List<Entry> entries;
    private final Random random;
    private final TextView progressText;
    private final TextView hintText;
    private final LinearLayout slotsBox;
    private final LinearLayout tilesBox;
    private final Runnable next = this::nextWord;
    private final Runnable reset = this::resetSlots;
    private TextView[] slots = new TextView[0];
    private TextView[] tiles = new TextView[0];
    private char[] letters = new char[0];
    private int[] slotTile = new int[0];
    private int index;
    private int mistakes;
    private boolean busy;

    public WordScrambleView(Context context, List<Entry> entries, Random random) {
        super(context);
        this.entries = new ArrayList<>(entries);
        this.random = random;

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        progressText = label("", 13, color(R.color.hud_text_secondary), true);
        hintText = label("", 18, color(R.color.treasure_gold), true);
        slotsBox = new LinearLayout(context);
        slotsBox.setOrientation(LinearLayout.VERTICAL);
        slotsBox.setGravity(Gravity.CENTER_HORIZONTAL);
        tilesBox = new LinearLayout(context);
        tilesBox.setOrientation(LinearLayout.VERTICAL);
        tilesBox.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        hintParams.topMargin = dp(6);
        hintParams.bottomMargin = dp(16);
        LinearLayout.LayoutParams tilesParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        tilesParams.topMargin = dp(20);

        column.addView(progressText, new LinearLayout.LayoutParams(WRAP, WRAP));
        column.addView(hintText, hintParams);
        column.addView(slotsBox, new LinearLayout.LayoutParams(WRAP, WRAP));
        column.addView(tilesBox, tilesParams);
        addView(column, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER));

        showWord();
    }

    @Override
    public int getMistakes() {
        return mistakes;
    }

    private void showWord() {
        Entry entry = entries.get(index);
        progressText.setText(getContext().getString(R.string.task_word_progress,
                index + 1, entries.size()));
        hintText.setText(getContext().getString(R.string.task_word_clue, entry.hint));

        int length = entry.word.length();
        letters = scramble(entry.word);
        slotTile = new int[length];
        Arrays.fill(slotTile, -1);
        slots = new TextView[length];
        tiles = new TextView[length];
        int size = length > 8 ? 32 : 38;

        for (int i = 0; i < length; i++) {
            final int slotIndex = i;
            TextView slot = label("", 20, color(R.color.white), true);
            slot.setBackground(box(color(R.color.hud_scrim), color(R.color.hud_stroke), 8, 1.5f));
            slot.setOnClickListener(v -> onSlot(slotIndex));
            slots[i] = slot;

            final int tileIndex = i;
            TextView tile = label(String.valueOf(letters[i]), 20, color(R.color.navy_950), true);
            tile.setBackground(box(color(R.color.treasure_gold), color(R.color.treasure_gold), 8, 0));
            tile.setOnClickListener(v -> onTile(tileIndex));
            tiles[i] = tile;
        }
        buildRows(slotsBox, slots, size);
        buildRows(tilesBox, tiles, size);
        busy = false;
    }

    private void buildRows(LinearLayout parent, TextView[] views, int sizeDp) {
        parent.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < views.length; i++) {
            if (i % PER_ROW == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER);
                parent.addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
            params.setMargins(dp(3), dp(3), dp(3), dp(3));
            row.addView(views[i], params);
        }
    }

    private char[] scramble(String word) {
        char[] original = word.toCharArray();
        char[] shuffled = original.clone();
        for (int attempt = 0; attempt < 20; attempt++) {
            for (int i = shuffled.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                char tmp = shuffled[i];
                shuffled[i] = shuffled[j];
                shuffled[j] = tmp;
            }
            if (!Arrays.equals(shuffled, original)) {
                break;
            }
        }
        return shuffled;
    }

    private void onTile(int tileIndex) {
        if (busy || isDone() || tiles[tileIndex].getVisibility() != View.VISIBLE) {
            return;
        }
        int slot = -1;
        for (int i = 0; i < slotTile.length; i++) {
            if (slotTile[i] < 0) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            return;
        }
        slotTile[slot] = tileIndex;
        slots[slot].setText(String.valueOf(letters[tileIndex]));
        tiles[tileIndex].setVisibility(View.INVISIBLE);

        for (int t : slotTile) {
            if (t < 0) {
                return;
            }
        }
        evaluate();
    }

    private void onSlot(int slot) {
        if (busy || isDone() || slotTile[slot] < 0) {
            return;
        }
        tiles[slotTile[slot]].setVisibility(View.VISIBLE);
        slotTile[slot] = -1;
        slots[slot].setText("");
    }

    private void evaluate() {
        StringBuilder guess = new StringBuilder();
        for (int t : slotTile) {
            guess.append(letters[t]);
        }
        busy = true;
        boolean right = guess.toString().equals(entries.get(index).word);
        if (right) {
            for (TextView slot : slots) {
                slot.setBackground(box(color(R.color.mint), color(R.color.mint), 8, 0));
                slot.setTextColor(color(R.color.navy_950));
            }
            postDelayed(next, FEEDBACK_MS);
        } else {
            mistakes++;
            for (TextView slot : slots) {
                slot.setBackground(box(ERROR_RED, ERROR_RED, 8, 0));
            }
            shake(slotsBox);
            postDelayed(reset, FEEDBACK_MS);
        }
    }

    private void resetSlots() {
        if (isDone()) {
            return;
        }
        for (int i = 0; i < slots.length; i++) {
            slots[i].setText("");
            slots[i].setTextColor(color(R.color.white));
            slots[i].setBackground(box(color(R.color.hud_scrim), color(R.color.hud_stroke), 8, 1.5f));
            tiles[i].setVisibility(View.VISIBLE);
        }
        Arrays.fill(slotTile, -1);
        busy = false;
    }

    private void nextWord() {
        if (isDone()) {
            return;
        }
        index++;
        if (index >= entries.size()) {
            finishTask(true);
            return;
        }
        showWord();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(next);
        removeCallbacks(reset);
        super.onDetachedFromWindow();
    }
}