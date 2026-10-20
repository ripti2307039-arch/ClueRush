package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.Locale;
import java.util.Random;

public class ColorClashTaskView extends BaseTaskView {

    private static final int[] NAMES = {
            R.string.clash_red, R.string.clash_blue, R.string.clash_green,
            R.string.clash_yellow, R.string.clash_purple
    };
    private static final int[] INKS = {
            0xFFFF5A5F, 0xFF4C8DFF, 0xFF3DD68C, 0xFFFFD60A, 0xFFB07CFF
    };
    private static final long FEEDBACK_MS = 320L;

    private final int colorCount;
    private final int maxMistakes;
    private final int[] wordOf;
    private final int[] inkOf;
    private final boolean[] byInk;
    private final TextView progressText;
    private final TextView ruleText;
    private final TextView wordText;
    private final Runnable next = this::nextRound;
    private int index;
    private int mistakes;
    private boolean busy;

    public ColorClashTaskView(Context context, int rounds, int colorCount, int maxMistakes,
                              int wordRulePercent, Random random) {
        super(context);
        this.colorCount = colorCount;
        this.maxMistakes = maxMistakes;
        wordOf = new int[rounds];
        inkOf = new int[rounds];
        byInk = new boolean[rounds];
        for (int i = 0; i < rounds; i++) {
            wordOf[i] = random.nextInt(colorCount);
            int ink;
            do {
                ink = random.nextInt(colorCount);
            } while (ink == wordOf[i]);
            inkOf[i] = ink;
            byInk[i] = random.nextInt(100) >= wordRulePercent;
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        progressText = label("", 13, color(R.color.hud_text_secondary), true);
        root.addView(progressText, new LinearLayout.LayoutParams(WRAP, WRAP));

        ruleText = label("", 14, color(R.color.white), true);
        LinearLayout.LayoutParams ruleParams = new LinearLayout.LayoutParams(dp(260), dp(30));
        ruleParams.topMargin = dp(6);
        root.addView(ruleText, ruleParams);

        wordText = label("", 40, color(R.color.white), true);
        LinearLayout.LayoutParams wordParams = new LinearLayout.LayoutParams(dp(260), dp(72));
        wordParams.topMargin = dp(8);
        root.addView(wordText, wordParams);

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < colorCount; i++) {
            final int choice = i;
            TextView button = label(context.getString(NAMES[i]).toUpperCase(Locale.ROOT), 14,
                    color(R.color.white), true);
            button.setBackground(box(color(R.color.navy_900), color(R.color.hud_stroke), 10, 1.5f));
            button.setOnClickListener(v -> answer(choice));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(84), dp(44));
            params.setMargins(dp(4), 0, dp(4), 0);
            buttons.addView(button, params);
        }
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        buttonsParams.topMargin = dp(10);
        root.addView(buttons, buttonsParams);

        addView(root, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        showRound();
    }

    @Override
    public int getMistakes() {
        return mistakes;
    }

    private void showRound() {
        progressText.setText(getContext().getString(R.string.task_clash_progress,
                index + 1, wordOf.length));
        if (byInk[index]) {
            ruleText.setText(R.string.task_clash_rule_ink);
            ruleText.setBackground(box(color(R.color.navy_900), color(R.color.treasure_gold),
                    10, 1.5f));
        } else {
            ruleText.setText(R.string.task_clash_rule_word);
            ruleText.setBackground(box(color(R.color.navy_900), color(R.color.mint), 10, 1.5f));
        }
        wordText.setText(getContext().getString(NAMES[wordOf[index]]).toUpperCase(Locale.ROOT));
        wordText.setTextColor(INKS[inkOf[index]]);
        wordText.setBackground(box(color(R.color.hud_scrim), color(R.color.hud_stroke), 12, 1.5f));
        busy = false;
    }

    private void answer(int choice) {
        if (busy || isDone()) {
            return;
        }
        int target = byInk[index] ? inkOf[index] : wordOf[index];
        busy = true;
        if (choice == target) {
            wordText.setBackground(box(color(R.color.hud_scrim), color(R.color.mint), 12, 2f));
        } else {
            mistakes++;
            wordText.setBackground(box(color(R.color.hud_scrim), ERROR_RED, 12, 2f));
            shake(wordText);
            if (mistakes > maxMistakes) {
                finishTask(false);
                return;
            }
        }
        postDelayed(next, FEEDBACK_MS);
    }

    private void nextRound() {
        if (isDone()) {
            return;
        }
        index++;
        if (index >= wordOf.length) {
            finishTask(true);
            return;
        }
        showRound();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(next);
        super.onDetachedFromWindow();
    }
}