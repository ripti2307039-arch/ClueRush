package com.ripti.cluerush.task;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.Random;

public class CodeBreakerTaskView extends BaseTaskView {

    private static final int FREE_GUESSES = 3;

    private final int[] code;
    private final int maxAttempts;
    private final TextView[] slots;
    private final TextView statusText;
    private final LinearLayout history;
    private final StringBuilder guess = new StringBuilder();
    private int attempts;

    public CodeBreakerTaskView(Context context, int length, int maxAttempts, Random random) {
        super(context);
        this.maxAttempts = maxAttempts;
        code = CodeGenerator.generate(random, length);
        slots = new TextView[length];

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);

        LinearLayout left = new LinearLayout(context);
        left.setOrientation(LinearLayout.VERTICAL);
        statusText = label("", 13, color(R.color.hud_text_secondary), true);
        statusText.setGravity(Gravity.START);
        left.addView(statusText, new LinearLayout.LayoutParams(MATCH, WRAP));
        history = new LinearLayout(context);
        history.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(MATCH, WRAP);
        historyParams.topMargin = dp(6);
        left.addView(history, historyParams);
        root.addView(left, new LinearLayout.LayoutParams(dp(190), WRAP));

        LinearLayout right = new LinearLayout(context);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout slotRow = new LinearLayout(context);
        slotRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < length; i++) {
            TextView slot = label("", 22, color(R.color.treasure_gold), true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(44));
            params.setMargins(dp(3), 0, dp(3), 0);
            slotRow.addView(slot, params);
            slots[i] = slot;
        }
        right.addView(slotRow, new LinearLayout.LayoutParams(WRAP, WRAP));
        LinearLayout.LayoutParams padParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        padParams.topMargin = dp(8);
        right.addView(buildPad(context), padParams);

        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        rightParams.setMarginStart(dp(16));
        root.addView(right, rightParams);

        addView(root, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        refreshSlots();
        refreshStatus();
    }

    @Override
    public int getMistakes() {
        return Math.max(0, attempts - 1 - FREE_GUESSES);
    }

    private LinearLayout buildPad(Context context) {
        LinearLayout pad = new LinearLayout(context);
        pad.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < 4; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 3; c++) {
                int slot = r * 3 + c;
                TextView key;
                if (slot == 9) {
                    key = key(context.getString(R.string.task_erase), 13f,
                            color(R.color.navy_900), color(R.color.hud_stroke),
                            color(R.color.white), v -> deleteDigit());
                } else if (slot == 11) {
                    key = key(context.getString(R.string.task_ok), 15f,
                            color(R.color.treasure_gold), color(R.color.treasure_gold),
                            color(R.color.navy_950), v -> submit());
                } else {
                    final int digit = slot == 10 ? 0 : slot + 1;
                    key = key(String.valueOf(digit), 20f, color(R.color.navy_900),
                            color(R.color.treasure_gold), color(R.color.treasure_gold),
                            v -> addDigit(digit));
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(40));
                params.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(key, params);
            }
            pad.addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
        }
        return pad;
    }

    private TextView key(String text, float sp, int fill, int stroke, int textColor,
                         View.OnClickListener listener) {
        TextView key = label(text, sp, textColor, true);
        key.setBackground(box(fill, stroke, 10, 1.5f));
        key.setOnClickListener(listener);
        return key;
    }

    private void addDigit(int digit) {
        if (isDone() || guess.length() >= code.length) {
            return;
        }
        if (guess.indexOf(String.valueOf(digit)) >= 0) {
            shake(slots[guess.length() - 1]);
            return;
        }
        guess.append(digit);
        refreshSlots();
    }

    private void deleteDigit() {
        if (isDone() || guess.length() == 0) {
            return;
        }
        guess.setLength(guess.length() - 1);
        refreshSlots();
    }

    private void submit() {
        if (isDone()) {
            return;
        }
        if (guess.length() < code.length) {
            shake(slots[0]);
            return;
        }
        int[] attempt = new int[code.length];
        for (int i = 0; i < code.length; i++) {
            attempt[i] = guess.charAt(i) - '0';
        }
        int exact = CodeGenerator.exact(code, attempt);
        int misplaced = CodeGenerator.misplaced(code, attempt);
        attempts++;
        addHistoryRow(guess.toString(), exact, misplaced);
        guess.setLength(0);
        refreshSlots();
        refreshStatus();
        if (exact == code.length) {
            finishTask(true);
        } else if (attempts >= maxAttempts) {
            finishTask(false);
        }
    }

    private void addHistoryRow(String digits, int exact, int misplaced) {
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < exact; i++) {
            marks.append('\u25CF');
        }
        for (int i = 0; i < misplaced; i++) {
            marks.append('\u25CB');
        }
        if (marks.length() == 0) {
            marks.append('\u2013');
        }
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView number = label(attempts + ".", 13, color(R.color.hud_text_secondary), false);
        TextView digitsText = label(spaced(digits), 16, color(R.color.white), true);
        digitsText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        TextView markText = label(marks.toString(), 16, color(R.color.treasure_gold), true);

        row.addView(number, new LinearLayout.LayoutParams(dp(24), WRAP));
        row.addView(digitsText, new LinearLayout.LayoutParams(dp(84), WRAP));
        row.addView(markText, new LinearLayout.LayoutParams(WRAP, WRAP));
        history.addView(row, new LinearLayout.LayoutParams(MATCH, dp(22)));
    }

    private String spaced(String digits) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    private void refreshSlots() {
        for (int i = 0; i < slots.length; i++) {
            boolean filled = i < guess.length();
            slots[i].setText(filled ? String.valueOf(guess.charAt(i)) : "");
            slots[i].setBackground(box(color(R.color.hud_scrim),
                    filled ? color(R.color.treasure_gold) : color(R.color.hud_stroke), 10, 1.5f));
        }
    }

    private void refreshStatus() {
        statusText.setText(getContext().getString(R.string.task_code_tries,
                maxAttempts - attempts));
    }
}