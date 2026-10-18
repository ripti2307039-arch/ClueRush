package com.ripti.cluerush.task;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;
import com.ripti.cluerush.model.Difficulty;

import java.util.Locale;

public class TaskOverlayView extends FrameLayout {

    public interface Callback {
        void onTaskFinished(boolean solved, int mistakes, int secondsLeft);
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int LOW_TIME_COLOR = 0xFFFF6B6B;
    private static final int READY_SECONDS = 5;
    private static final long READY_TICK_MS = 1000L;
    private static final long TICK_MS = 100L;
    private static final long FINISH_DELAY_MS = 600L;

    private final float density;
    private final TextView titleText;
    private final TextView timerText;
    private final TextView hintText;
    private final View barFill;
    private final FrameLayout container;
    private final FrameLayout readyLayer;
    private final Runnable tick = this::onTick;
    private final Runnable readyTick = this::onReadyTick;

    private TextView readyTitle;
    private TextView readyRules;
    private TextView readyTime;
    private TextView readyButton;
    private BaseTaskView content;
    private Callback callback;
    private long limitMs;
    private long remainingMs;
    private long lastTickAt;
    private int readyLeft;
    private boolean readyActive;
    private boolean running;
    private boolean active;
    private boolean finishing;
    private boolean lowTime;

    public TaskOverlayView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        setBackgroundColor(color(R.color.navy_950));
        setClickable(true);
        setFocusable(true);
        setVisibility(GONE);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(12));

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        titleText = text(18, R.color.white);
        titleText.setGravity(Gravity.START);
        timerText = text(18, R.color.treasure_gold);
        header.addView(titleText, new LinearLayout.LayoutParams(0, WRAP, 1f));
        header.addView(timerText, new LinearLayout.LayoutParams(WRAP, WRAP));
        root.addView(header, new LinearLayout.LayoutParams(MATCH, WRAP));

        FrameLayout track = new FrameLayout(context);
        track.setBackgroundColor(color(R.color.hud_stroke));
        barFill = new View(context);
        barFill.setBackgroundColor(color(R.color.treasure_gold));
        barFill.setPivotX(0f);
        track.addView(barFill, new FrameLayout.LayoutParams(MATCH, MATCH));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(MATCH, dp(6));
        barParams.topMargin = dp(8);
        root.addView(track, barParams);

        hintText = text(13, R.color.hud_text_secondary);
        hintText.setTypeface(Typeface.DEFAULT);
        hintText.setGravity(Gravity.START);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(MATCH, WRAP);
        hintParams.topMargin = dp(8);
        root.addView(hintText, hintParams);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        container = new FrameLayout(context);
        scroll.addView(container, new ViewGroup.LayoutParams(MATCH, WRAP));

        FrameLayout body = new FrameLayout(context);
        body.addView(scroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        readyLayer = buildReadyLayer(context);
        body.addView(readyLayer, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        bodyParams.topMargin = dp(8);
        root.addView(body, bodyParams);

        addView(root, new FrameLayout.LayoutParams(MATCH, MATCH));
    }

    public boolean isActive() {
        return active;
    }

    public void begin(TaskType type, Difficulty difficulty, BaseTaskView view, Callback callback) {
        removeCallbacks(tick);
        removeCallbacks(readyTick);
        this.callback = callback;
        content = view;
        active = true;
        finishing = false;
        running = false;
        lowTime = false;

        int limitSeconds = type.timeLimitSeconds(difficulty);
        limitMs = limitSeconds * 1000L;
        remainingMs = limitMs;

        titleText.setText(type.getTitleRes());
        hintText.setText(type.getHintRes());
        readyTitle.setText(type.getTitleRes());
        readyRules.setText(type.getHintRes());
        readyTime.setText(getContext().getString(R.string.task_ready_time,
                formatTime(limitSeconds)));

        barFill.setBackgroundColor(color(R.color.treasure_gold));
        timerText.setTextColor(color(R.color.treasure_gold));

        container.removeAllViews();
        container.addView(view, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER));
        view.setListener((solved, mistakes) -> onContentFinished(solved, mistakes));
        updateTimerUi();
        showReady();

        animate().cancel();
        setAlpha(0f);
        setVisibility(VISIBLE);
        animate().alpha(1f).setDuration(200L).start();
    }

    public void pause() {
        running = false;
        removeCallbacks(tick);
        removeCallbacks(readyTick);
    }

    public void resume() {
        if (!active || finishing) {
            return;
        }
        if (readyActive) {
            removeCallbacks(readyTick);
            postDelayed(readyTick, READY_TICK_MS);
            return;
        }
        if (running) {
            return;
        }
        running = true;
        lastTickAt = SystemClock.uptimeMillis();
        postDelayed(tick, TICK_MS);
    }

    private void showReady() {
        readyActive = true;
        readyLeft = READY_SECONDS;
        updateReadyButton();
        readyLayer.setVisibility(VISIBLE);
        removeCallbacks(readyTick);
        postDelayed(readyTick, READY_TICK_MS);
    }

    private void onReadyTick() {
        if (!readyActive) {
            return;
        }
        readyLeft--;
        if (readyLeft <= 0) {
            startPlaying();
            return;
        }
        updateReadyButton();
        postDelayed(readyTick, READY_TICK_MS);
    }

    private void startPlaying() {
        if (!readyActive) {
            return;
        }
        readyActive = false;
        removeCallbacks(readyTick);
        readyLayer.setVisibility(GONE);
        resume();
    }

    private void updateReadyButton() {
        readyButton.setText(getContext().getString(R.string.task_ready_start, readyLeft));
    }

    private FrameLayout buildReadyLayer(Context context) {
        FrameLayout layer = new FrameLayout(context);
        layer.setBackgroundColor(color(R.color.navy_950));
        layer.setClickable(true);
        layer.setVisibility(GONE);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView label = text(12, R.color.hud_text_secondary);
        label.setText(R.string.task_ready_label);
        label.setLetterSpacing(0.15f);
        label.setGravity(Gravity.CENTER);

        readyTitle = text(26, R.color.treasure_gold);
        readyTitle.setGravity(Gravity.CENTER);

        readyRules = text(15, R.color.white);
        readyRules.setTypeface(Typeface.DEFAULT);
        readyRules.setGravity(Gravity.CENTER);
        readyRules.setMaxWidth(dp(420));

        readyTime = text(14, R.color.hud_text_secondary);
        readyTime.setGravity(Gravity.CENTER);

        TextView note = text(12, R.color.hud_text_secondary);
        note.setTypeface(Typeface.DEFAULT);
        note.setText(R.string.task_ready_note);
        note.setGravity(Gravity.CENTER);

        readyButton = text(17, R.color.navy_950);
        readyButton.setGravity(Gravity.CENTER);
        readyButton.setPadding(dp(36), dp(12), dp(36), dp(12));
        readyButton.setBackground(rounded(color(R.color.treasure_gold), 14f));
        readyButton.setClickable(true);
        readyButton.setOnClickListener(v -> startPlaying());

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        titleParams.topMargin = dp(4);
        LinearLayout.LayoutParams rulesParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        rulesParams.topMargin = dp(10);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        timeParams.topMargin = dp(8);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        buttonParams.topMargin = dp(16);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        noteParams.topMargin = dp(8);

        column.addView(label, new LinearLayout.LayoutParams(WRAP, WRAP));
        column.addView(readyTitle, titleParams);
        column.addView(readyRules, rulesParams);
        column.addView(readyTime, timeParams);
        column.addView(readyButton, buttonParams);
        column.addView(note, noteParams);

        layer.addView(column, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        return layer;
    }

    private GradientDrawable rounded(int fill, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private void onTick() {
        if (!running) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        remainingMs -= now - lastTickAt;
        lastTickAt = now;
        if (remainingMs <= 0L) {
            remainingMs = 0L;
            running = false;
            updateTimerUi();
            timeUp();
            return;
        }
        updateTimerUi();
        postDelayed(tick, TICK_MS);
    }

    private void onContentFinished(boolean solved, int mistakes) {
        if (!active || finishing) {
            return;
        }
        finishing = true;
        pause();
        final int seconds = secondsLeft();
        postDelayed(() -> deliver(solved, mistakes, seconds), FINISH_DELAY_MS);
    }

    private void timeUp() {
        if (!active || finishing) {
            return;
        }
        finishing = true;
        int mistakes = 0;
        if (content != null) {
            content.lock();
            mistakes = content.getMistakes();
        }
        deliver(false, mistakes, 0);
    }

    private void deliver(boolean solved, int mistakes, int secondsLeft) {
        Callback cb = callback;
        callback = null;
        active = false;
        finishing = false;
        readyActive = false;
        removeCallbacks(readyTick);
        setVisibility(GONE);
        readyLayer.setVisibility(GONE);
        container.removeAllViews();
        content = null;
        if (cb != null) {
            cb.onTaskFinished(solved, mistakes, secondsLeft);
        }
    }

    private int secondsLeft() {
        return (int) Math.ceil(remainingMs / 1000.0);
    }

    private static String formatTime(int seconds) {
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    private void updateTimerUi() {
        int seconds = secondsLeft();
        timerText.setText(formatTime(seconds));
        float fraction = limitMs <= 0L ? 0f : Math.max(0f, Math.min(1f, remainingMs / (float) limitMs));
        barFill.setScaleX(fraction);
        boolean low = fraction < 0.25f;
        if (low != lowTime) {
            lowTime = low;
            int tint = low ? LOW_TIME_COLOR : color(R.color.treasure_gold);
            barFill.setBackgroundColor(tint);
            timerText.setTextColor(tint);
        }
    }

    private TextView text(float sp, @ColorRes int colorRes) {
        TextView view = new TextView(getContext());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color(colorRes));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private int color(@ColorRes int res) {
        return ContextCompat.getColor(getContext(), res);
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(tick);
        removeCallbacks(readyTick);
        super.onDetachedFromWindow();
    }
}