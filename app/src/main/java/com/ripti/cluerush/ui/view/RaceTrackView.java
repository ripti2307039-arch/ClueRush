package com.ripti.cluerush.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

import java.util.Locale;

public class RaceTrackView extends View {

    private static final long FILL_MS = 600L;
    private static final long PULSE_MS = 650L;

    private final float density;
    private final TextPaint namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF segment = new RectF();
    private final int meColor;
    private final int oppColor;

    private int total = 5;

    private String meName = "";
    private int meRounds;
    private int meScore;

    private String oppName = "";
    private float oppShownRounds;
    private int oppTargetRounds;
    private float oppShownScore;
    private int oppTargetScore;
    private boolean oppSolving;
    private boolean oppKnown;

    private ValueAnimator fillAnimator;
    private ValueAnimator pulseAnimator;
    private float pulse;

    public RaceTrackView(Context context) {
        this(context, null);
    }

    public RaceTrackView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        meColor = ContextCompat.getColor(context, R.color.player_me);
        oppColor = ContextCompat.getColor(context, R.color.alert_red);

        namePaint.setColor(ContextCompat.getColor(context, R.color.hud_text_secondary));
        namePaint.setTextSize(sp(12f));
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(sp(14f));
        scorePaint.setTextAlign(Paint.Align.RIGHT);
        scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        emptyPaint.setStyle(Paint.Style.FILL);
        emptyPaint.setColor(Color.WHITE);
        emptyPaint.setAlpha(34);

        fillPaint.setStyle(Paint.Style.FILL);
        activePaint.setStyle(Paint.Style.FILL);
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }

    public void setTotal(int rounds) {
        int safe = Math.max(1, rounds);
        if (safe != total) {
            total = safe;
            invalidate();
        }
    }

    public void setMe(String name, int rounds, int score) {
        meName = name == null ? "" : name;
        meRounds = Math.max(0, Math.min(total, rounds));
        meScore = score;
        invalidate();
    }

    public void setOpponent(String name, int rounds, int score, boolean solving) {
        oppName = name == null ? "" : name;
        int safeRounds = Math.max(0, Math.min(total, rounds));
        boolean first = !oppKnown;
        oppKnown = true;
        boolean changed = safeRounds != oppTargetRounds || score != oppTargetScore;
        oppTargetRounds = safeRounds;
        oppTargetScore = score;

        if (solving != oppSolving) {
            oppSolving = solving;
        }
        if (first) {
            oppShownRounds = safeRounds;
            oppShownScore = score;
        } else if (changed) {
            animateOpponent();
        }
        updatePulse();
        invalidate();
    }

    private void animateOpponent() {
        if (fillAnimator != null) {
            fillAnimator.cancel();
        }
        final float fromRounds = oppShownRounds;
        final float fromScore = oppShownScore;
        fillAnimator = ValueAnimator.ofFloat(0f, 1f);
        fillAnimator.setDuration(FILL_MS);
        fillAnimator.setInterpolator(new DecelerateInterpolator());
        fillAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            oppShownRounds = fromRounds + (oppTargetRounds - fromRounds) * t;
            oppShownScore = fromScore + (oppTargetScore - fromScore) * t;
            invalidate();
        });
        fillAnimator.start();
    }

    private void updatePulse() {
        boolean needed = oppKnown && oppTargetRounds < total && isAttachedToWindow();
        if (needed) {
            if (pulseAnimator == null) {
                pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
                pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
                pulseAnimator.addUpdateListener(animation -> {
                    pulse = (float) animation.getAnimatedValue();
                    invalidate();
                });
            }
            pulseAnimator.setDuration(oppSolving ? PULSE_MS / 2 : PULSE_MS);
            if (!pulseAnimator.isRunning()) {
                pulseAnimator.start();
            }
        } else if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
            pulse = 0f;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePulse();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (fillAnimator != null) {
            fillAnimator.cancel();
            fillAnimator = null;
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int fallbackWidth = (int) (300f * density);
        int fallbackHeight = (int) (68f * density);
        setMeasuredDimension(resolveSize(fallbackWidth, widthMeasureSpec),
                resolveSize(fallbackHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float laneHeight = h / 2f;

        drawLane(canvas, 0f, w, laneHeight, meName, meRounds, meScore, meColor, false);
        drawLane(canvas, laneHeight, w, laneHeight, oppName, oppShownRounds,
                Math.round(oppShownScore), oppColor, true);
    }

    private void drawLane(Canvas canvas, float top, float w, float laneHeight, String name,
                          float rounds, int score, int color, boolean opponent) {
        float nameWidth = 58f * density;
        float scoreWidth = 48f * density;
        float gap = 4f * density;
        float barHeight = 12f * density;
        float centerY = top + laneHeight / 2f;

        CharSequence label = TextUtils.ellipsize(name, namePaint,
                nameWidth - 6f * density, TextUtils.TruncateAt.END);
        float textBaseline = centerY - (namePaint.ascent() + namePaint.descent()) / 2f;
        canvas.drawText(label, 0, label.length(), 0f, textBaseline, namePaint);

        String scoreText = opponent && !oppKnown ? "--" : String.format(Locale.US, "%d", score);
        float scoreBaseline = centerY - (scorePaint.ascent() + scorePaint.descent()) / 2f;
        canvas.drawText(scoreText, w, scoreBaseline, scorePaint);

        float barLeft = nameWidth;
        float barRight = w - scoreWidth;
        float areaWidth = barRight - barLeft;
        if (areaWidth <= 0f) {
            return;
        }
        float segmentWidth = (areaWidth - gap * (total - 1)) / total;
        float radius = barHeight / 2f;

        fillPaint.setColor(color);
        activePaint.setColor(color);

        for (int i = 0; i < total; i++) {
            float left = barLeft + i * (segmentWidth + gap);
            segment.set(left, centerY - barHeight / 2f, left + segmentWidth,
                    centerY + barHeight / 2f);
            canvas.drawRoundRect(segment, radius, radius, emptyPaint);

            float fill = Math.max(0f, Math.min(1f, rounds - i));
            if (fill > 0f) {
                segment.set(left, centerY - barHeight / 2f, left + segmentWidth * fill,
                        centerY + barHeight / 2f);
                canvas.drawRoundRect(segment, radius, radius, fillPaint);
            } else if (opponent && oppKnown && i == oppTargetRounds && oppTargetRounds < total) {
                activePaint.setAlpha((int) (60 + 120 * pulse));
                segment.set(left, centerY - barHeight / 2f, left + segmentWidth,
                        centerY + barHeight / 2f);
                canvas.drawRoundRect(segment, radius, radius, activePaint);
            }
        }
    }
}