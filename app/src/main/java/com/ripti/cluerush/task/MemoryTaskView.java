package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MemoryTaskView extends BaseTaskView {

    private static final String[] SYMBOLS = {
            "\u2605", "\u2665", "\u2660", "\u2663", "\u2666",
            "\u25CF", "\u25B2", "\u25A0", "\u2726", "\u266B"
    };
    private static final int[] COLORS = {
            0xFFF5B301, 0xFFE5484D, 0xFF3E63DD, 0xFF30A46C, 0xFF8E4EC6,
            0xFF12A594, 0xFFF76B15, 0xFFD6409F, 0xFF0091FF, 0xFF7CB518
    };
    private static final long MISMATCH_MS = 700L;

    private final int[] symbolOf;
    private final TextView[] cards;
    private final boolean[] faceUp;
    private final boolean[] matched;
    private final Runnable hide = this::hidePending;
    private int pairsLeft;
    private int first = -1;
    private int pendingA = -1;
    private int pendingB = -1;
    private int mistakes;
    private boolean busy;

    public MemoryTaskView(Context context, int pairs, int columns, int cardDp, Random random) {
        super(context);
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < SYMBOLS.length; i++) {
            ids.add(i);
        }
        Collections.shuffle(ids, random);

        List<Integer> deck = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            deck.add(ids.get(i));
            deck.add(ids.get(i));
        }
        Collections.shuffle(deck, random);

        int total = deck.size();
        symbolOf = new int[total];
        cards = new TextView[total];
        faceUp = new boolean[total];
        matched = new boolean[total];
        pairsLeft = pairs;

        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        int rows = (total + columns - 1) / columns;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < columns; c++) {
                int pos = r * columns + c;
                if (pos >= total) {
                    break;
                }
                symbolOf[pos] = deck.get(pos);
                final int cardIndex = pos;
                TextView card = label("?", 24, color(R.color.treasure_gold), true);
                card.setOnClickListener(v -> onCard(cardIndex));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(dp(cardDp), dp(cardDp));
                params.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(card, params);
                cards[pos] = card;
                showFace(pos, false);
            }
            grid.addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
        }
        addView(grid, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
    }

    @Override
    public int getMistakes() {
        return mistakes;
    }

    private void showFace(int pos, boolean up) {
        faceUp[pos] = up;
        TextView card = cards[pos];
        if (up) {
            int tint = COLORS[symbolOf[pos]];
            card.setText(SYMBOLS[symbolOf[pos]]);
            card.setTextColor(color(R.color.white));
            card.setBackground(box(tint, tint, 10, 0));
        } else {
            card.setText("?");
            card.setTextColor(color(R.color.treasure_gold));
            card.setBackground(box(color(R.color.navy_900), color(R.color.treasure_gold), 10, 1.5f));
        }
    }

    private void onCard(int pos) {
        if (busy || isDone() || matched[pos] || faceUp[pos]) {
            return;
        }
        showFace(pos, true);
        if (first < 0) {
            first = pos;
            return;
        }
        int second = pos;
        if (symbolOf[first] == symbolOf[second]) {
            matched[first] = true;
            matched[second] = true;
            cards[first].setAlpha(0.6f);
            cards[second].setAlpha(0.6f);
            first = -1;
            pairsLeft--;
            if (pairsLeft == 0) {
                finishTask(true);
            }
        } else {
            mistakes++;
            busy = true;
            pendingA = first;
            pendingB = second;
            first = -1;
            postDelayed(hide, MISMATCH_MS);
        }
    }

    private void hidePending() {
        if (pendingA >= 0) {
            showFace(pendingA, false);
        }
        if (pendingB >= 0) {
            showFace(pendingB, false);
        }
        pendingA = -1;
        pendingB = -1;
        busy = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(hide);
        super.onDetachedFromWindow();
    }
}