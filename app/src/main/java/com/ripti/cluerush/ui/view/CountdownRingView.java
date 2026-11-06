package com.ripti.cluerush.ui.view;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

public class CountdownRingView extends View {

    private static final long FRACTION_MS = 420L;
    private static final long PULSE_MS = 480L;

    private final float density;
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();
    private final ArgbEvaluator evaluator = new ArgbEvaluator();

    private final int colorSafe;
    private final int colorWarn;
    private final int colorDanger;
    private final int colorMuted;

    private float shownFraction = 1f;
    private float targetFraction = 1f;
    private String timeText = "--";
    private String labelText = "";
    private boolean urgent;
    private boolean offline;
    private float pulse;

    private ValueAnimator fractionAnimator;
    private ValueAnimator pulseAnimator;

    public CountdownRingView(Context context) {
        this(context, null);
    }

    public CountdownRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        colorSafe = ContextCompat.getColor(context, R.color.mint);
        colorWarn = ContextCompat.getColor(context, R.color.treasure_gold);
        colorDanger = ContextCompat.getColor(context, R.color.alert_red);
        colorMuted = ContextCompat.getColor(context, R.color.hud_text_secondary);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(8f * density);
        trackPaint.setColor(Color.WHITE);
        trackPaint.setAlpha(34);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setStrokeWidth(8f * density);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeWidth(8f * density);

        timePaint.setTextAlign(Paint.Align.CENTER);
        timePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        timePaint.setColor(Color.WHITE);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(colorMuted);
        labelPaint.setTextSize(11f * density);
    }

    public void setState(float fraction, String time, String label, boolean urgent,
                         boolean offline) {
        float clamped = Math.max(0f, Math.min(1f, fraction));
        timeText = time == null ? "" : time;
        labelText = label == null ? "" : label;
        setContentDescription(timeText + " " + labelText);
        this.offline = offline;

        if (Math.abs(clamped - targetFraction) > 0.002f) {
            targetFraction = clamped;
            animateFraction();
        }
        if (this.urgent != urgent) {
            this.urgent = urgent;
            updatePulse();
        }
        invalidate();
    }

    private void animateFraction() {
        if (fractionAnimator != null) {
            fractionAnimator.cancel();
        }
        fractionAnimator = ValueAnimator.ofFloat(shownFraction, targetFraction);
        fractionAnimator.setDuration(FRACTION_MS);
        fractionAnimator.setInterpolator(new LinearInterpolator());
        fractionAnimator.addUpdateListener(animation -> {
            shownFraction = (float) animation.getAnimatedValue();
            invalidate();
        });
        fractionAnimator.start();
    }

    private void updatePulse() {
        if (urgent && isAttachedToWindow()) {
            if (pulseAnimator == null) {
                pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
                pulseAnimator.setDuration(PULSE_MS);
                pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
                pulseAnimator.addUpdateListener(animation -> {
                    pulse = (float) animation.getAnimatedValue();
                    invalidate();
                });
            }
            if (!pulseAnimator.isRunning()) {
                pulseAnimator.start();
            }
        } else {
            if (pulseAnimator != null) {
                pulseAnimator.cancel();
                pulseAnimator = null;
            }
            pulse = 0f;
        }
    }

    private int ringColor() {
        if (offline) {
            return colorMuted;
        }
        float f = shownFraction;
        if (f >= 0.5f) {
            return colorSafe;
        }
        if (f >= 0.25f) {
            float t = (0.5f - f) / 0.25f;
            return (int) evaluator.evaluate(t, colorSafe, colorWarn);
        }
        if (f >= 0.1f) {
            float t = (0.25f - f) / 0.15f;
            return (int) evaluator.evaluate(t, colorWarn, colorDanger);
        }
        return colorDanger;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePulse();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (fractionAnimator != null) {
            fractionAnimator.cancel();
            fractionAnimator = null;
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int fallback = (int) (104f * density);
        setMeasuredDimension(resolveSize(fallback, widthMeasureSpec),
                resolveSize(fallback, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float pad = 10f * density;
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = size / 2f - pad;
        if (radius <= 0f) {
            return;
        }
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        canvas.drawCircle(cx, cy, radius, trackPaint);

        int color = ringColor();
        float sweep = 360f * shownFraction;
        if (sweep > 0.5f) {
            glowPaint.setColor(color);
            glowPaint.setStrokeWidth((8f + 7f * pulse) * density);
            glowPaint.setAlpha((int) (46 + 70 * pulse));
            canvas.drawArc(arcRect, -90f, sweep, false, glowPaint);

            arcPaint.setColor(color);
            canvas.drawArc(arcRect, -90f, sweep, false, arcPaint);
        }

        float textScale = 1f + 0.07f * pulse;
        boolean longText = timeText.length() > 5;
        timePaint.setTextSize((longText ? 15f : 26f) * density * textScale);
        float timeY = cy + 0.35f * timePaint.getTextSize()
                - (labelText.isEmpty() ? 0f : 8f * density);
        canvas.drawText(timeText, cx, timeY, timePaint);

        if (!labelText.isEmpty()) {
            canvas.drawText(labelText, cx, timeY + 15f * density, labelPaint);
        }
    }
}