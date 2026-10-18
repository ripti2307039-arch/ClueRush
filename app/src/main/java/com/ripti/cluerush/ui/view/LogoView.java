package com.ripti.cluerush.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.ripti.cluerush.R;


public class LogoView extends View {

    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path headPath = new Path();
    private final Path tailPath = new Path();

    private final int blue;
    private final int gold;
    private final int mint;
    private final float density;

    private final ValueAnimator pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
    private float pulse = 0f;

    private float cx;
    private float cy;
    private float radius;

    public LogoView(Context context) {
        this(context, null);
    }

    public LogoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        blue = ContextCompat.getColor(context, R.color.rush_blue);
        gold = ContextCompat.getColor(context, R.color.treasure_gold);
        mint = ContextCompat.getColor(context, R.color.mint);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3f * density);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(2.5f * density);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);
        tickPaint.setColor(ColorUtils.setAlphaComponent(blue, 200));

        pinPaint.setStyle(Paint.Style.FILL);
        pinPaint.setColor(gold);

        pulseAnimator.setDuration(2200L);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulse = (float) animation.getAnimatedValue();
            invalidate();
        });

        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cx = w / 2f;
        cy = h / 2f;
        radius = Math.min(w, h) / 2f;

        glowPaint.setShader(new RadialGradient(cx, cy, radius,
                new int[]{ColorUtils.setAlphaComponent(blue, 150), ColorUtils.setAlphaComponent(blue, 0)},
                null, Shader.TileMode.CLAMP));

        ringPaint.setShader(new SweepGradient(cx, cy, new int[]{blue, mint, blue}, null));

        buildPin();
    }

    private void buildPin() {
        float headR = radius * 0.22f;
        float headY = cy - radius * 0.07f;

        headPath.reset();
        headPath.setFillType(Path.FillType.EVEN_ODD);
        headPath.addCircle(cx, headY, headR, Path.Direction.CW);
        headPath.addCircle(cx, headY, headR * 0.4f, Path.Direction.CW);

        double angle = Math.toRadians(40);
        float dx = (float) (headR * Math.cos(angle));
        float dy = (float) (headR * Math.sin(angle));
        tailPath.reset();
        tailPath.moveTo(cx - dx, headY + dy);
        tailPath.lineTo(cx + dx, headY + dy);
        tailPath.lineTo(cx, headY + headR * 2.2f);
        tailPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (radius <= 0f) {
            return;
        }


        float scale = 0.88f + 0.12f * pulse;
        glowPaint.setAlpha((int) (140 + 115 * pulse));
        canvas.save();
        canvas.scale(scale, scale, cx, cy);
        canvas.drawCircle(cx, cy, radius, glowPaint);
        canvas.restore();

        // Compass ring
        float ringR = radius * 0.72f;
        canvas.drawCircle(cx, cy, ringR, ringPaint);

        // N, E, S, W ticks
        float tickStart = ringR + radius * 0.07f;
        float tickEnd = ringR + radius * 0.16f;
        canvas.drawLine(cx, cy - tickStart, cx, cy - tickEnd, tickPaint);
        canvas.drawLine(cx, cy + tickStart, cx, cy + tickEnd, tickPaint);
        canvas.drawLine(cx - tickStart, cy, cx - tickEnd, cy, tickPaint);
        canvas.drawLine(cx + tickStart, cy, cx + tickEnd, cy, tickPaint);

        // Pin
        canvas.drawPath(headPath, pinPaint);
        canvas.drawPath(tailPath, pinPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        pulseAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        pulseAnimator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            pulseAnimator.resume();
        } else {
            pulseAnimator.pause();
        }
    }
}