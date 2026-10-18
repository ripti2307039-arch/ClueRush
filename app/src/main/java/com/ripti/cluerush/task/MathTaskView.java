package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.ArrayList;
import java.util.List;

public class MathTaskView extends BaseTaskView {

    private static final long FEEDBACK_MS = 450L;
    private static final int MAX_DIGITS = 6;

    private final List<MathGenerator.Problem> problems;
    private final TextView progressText;
    private final TextView expressionText;
    private final TextView answerText;
    private final StringBuilder input = new StringBuilder();
    private final Runnable next = this::nextProblem;
    private int index;
    private int mistakes;
    private boolean busy;

    public MathTaskView(Context context, List<MathGenerator.Problem> problems) {
        super(context);
        this.problems = new ArrayList<>(problems);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);

        LinearLayout left = new LinearLayout(context);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL);

        progressText = label("", 13, color(R.color.hud_text_secondary), true);
        expressionText = label("", 30, color(R.color.white), true);
        answerText = label("", 26, color(R.color.treasure_gold), true);

        LinearLayout.LayoutParams expressionParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        expressionParams.topMargin = dp(12);
        expressionParams.bottomMargin = dp(16);

        left.addView(progressText, new LinearLayout.LayoutParams(WRAP, WRAP));
        left.addView(expressionText, expressionParams);
        left.addView(answerText, new LinearLayout.LayoutParams(dp(180), dp(54)));
        root.addView(left, new LinearLayout.LayoutParams(dp(230), WRAP));

        LinearLayout.LayoutParams padParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        padParams.setMarginStart(dp(16));
        root.addView(buildPad(context), padParams);

        addView(root, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        showProblem();
    }

    @Override
    public int getMistakes() {
        return mistakes;
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
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(dp(46), dp(40));
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

    private void showProblem() {
        MathGenerator.Problem problem = problems.get(index);
        progressText.setText(getContext().getString(R.string.task_math_progress,
                index + 1, problems.size()));
        expressionText.setText(problem.text + " = ?");
        input.setLength(0);
        answerText.setText("");
        styleAnswer(color(R.color.hud_scrim), color(R.color.hud_stroke));
        busy = false;
    }

    private void addDigit(int digit) {
        if (busy || isDone() || input.length() >= MAX_DIGITS) {
            return;
        }
        input.append(digit);
        answerText.setText(input.toString());
        styleAnswer(color(R.color.hud_scrim), color(R.color.hud_stroke));
    }

    private void deleteDigit() {
        if (busy || isDone() || input.length() == 0) {
            return;
        }
        input.setLength(input.length() - 1);
        answerText.setText(input.toString());
    }

    private void submit() {
        if (busy || isDone() || input.length() == 0) {
            return;
        }
        long value = Long.parseLong(input.toString());
        if (value == problems.get(index).answer) {
            busy = true;
            styleAnswer(color(R.color.mint), color(R.color.mint));
            answerText.setTextColor(color(R.color.navy_950));
            postDelayed(next, FEEDBACK_MS);
        } else {
            mistakes++;
            styleAnswer(ERROR_RED, ERROR_RED);
            shake(answerText);
            input.setLength(0);
        }
    }

    private void nextProblem() {
        if (isDone()) {
            return;
        }
        answerText.setTextColor(color(R.color.treasure_gold));
        index++;
        if (index >= problems.size()) {
            finishTask(true);
            return;
        }
        showProblem();
    }

    private void styleAnswer(int fill, int stroke) {
        answerText.setBackground(box(fill, stroke, 12, 1.5f));
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(next);
        super.onDetachedFromWindow();
    }
}