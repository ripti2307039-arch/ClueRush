package com.ripti.cluerush.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.ripti.cluerush.R;

/**
 * Animated "Neon Night-Map" decoration drawn on top of the window background.
 * Draws two slowly drifting glow orbs, a dashed treasure route between two pins,
 * and a gold dot travelling along the route.
 * Everything is cheap Canvas drawing, no bitmaps, no allocations per frame.
 */
public class NeonMapBackgroundView extends View implements Choreographer.FrameCallback {

    private static final float ROUTE_PERIOD_SECONDS = 10f;
    private static final float PIN_PULSE_SECONDS = 2.4f;

    private final Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path route = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final float[] pos = new float[2];

    private final int blue;
    private final int gold;
    private final int mint;
    private final float density;

    private RadialGradient blueOrb;
    private RadialGradient goldOrb;
    private float orbRadius;
    private float routeLength;
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    private float animTime = 2f;
    private long lastFrameNanos = 0L;
    private boolean running = false;
    private boolean reduceEffects = false;

    public NeonMapBackgroundView(Context context) {
        this(context, null);
    }

    public NeonMapBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        blue = ContextCompat.getColor(context, R.color.rush_blue);
        gold = ContextCompat.getColor(context, R.color.treasure_gold);
        mint = ContextCompat.getColor(context, R.color.mint);

        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeWidth(2f * density);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setColor(ColorUtils.setAlphaComponent(blue, 120));
        routePaint.setPathEffect(new DashPathEffect(new float[]{10f * density, 9f * density}, 0f));

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1.5f * density);

        dotPaint.setColor(gold);
        dotGlowPaint.setColor(ColorUtils.setAlphaComponent(gold, 70));

        setWillNotDraw(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Call with true to stop all animation (future "Reduce effects" setting). */
    public void setReduceEffects(boolean reduce) {
        reduceEffects = reduce;
        if (reduce) {
            stop();
            animTime = 2f;
            invalidate();
        } else if (isAttachedToWindow() && getWindowVisibility() == VISIBLE) {
            start();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w == 0 || h == 0) {
            return;
        }
        orbRadius = Math.max(w, h) * 0.55f;
        blueOrb = new RadialGradient(0f, 0f, orbRadius,
                new int[]{ColorUtils.setAlphaComponent(blue, 85), ColorUtils.setAlphaComponent(blue, 0)},
                null, Shader.TileMode.CLAMP);
        goldOrb = new RadialGradient(0f, 0f, orbRadius,
                new int[]{ColorUtils.setAlphaComponent(gold, 55), ColorUtils.setAlphaComponent(gold, 0)},
                null, Shader.TileMode.CLAMP);

        route.reset();
        route.moveTo(w * 0.10f, h * 0.88f);
        route.cubicTo(w * 0.40f, h * 0.98f, w * 0.30f, h * 0.64f, w * 0.58f, h * 0.60f);
        route.cubicTo(w * 0.86f, h * 0.56f, w * 0.62f, h * 0.30f, w * 0.90f, h * 0.14f);

        measure.setPath(route, false);
        routeLength = measure.getLength();
        measure.getPosTan(0f, pos, null);
        startX = pos[0];
        startY = pos[1];
        measure.getPosTan(routeLength, pos, null);
        endX = pos[0];
        endY = pos[1];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (blueOrb == null || goldOrb == null || routeLength <= 0f) {
            return;
        }
        float w = getWidth();
        float h = getHeight();
        float t = animTime;

        // Glow orbs drifting slowly
        float bx = w * 0.15f + (float) Math.sin(t * 0.25f) * w * 0.08f;
        float by = h * 0.12f + (float) Math.cos(t * 0.21f) * h * 0.04f;
        canvas.save();
        canvas.translate(bx, by);
        orbPaint.setShader(blueOrb);
        canvas.drawCircle(0f, 0f, orbRadius, orbPaint);
        canvas.restore();

        float gx = w * 0.90f + (float) Math.cos(t * 0.20f) * w * 0.06f;
        float gy = h * 0.90f + (float) Math.sin(t * 0.23f) * h * 0.03f;
        canvas.save();
        canvas.translate(gx, gy);
        orbPaint.setShader(goldOrb);
        canvas.drawCircle(0f, 0f, orbRadius * 0.85f, orbPaint);
        canvas.restore();

        // Dashed treasure route
        canvas.drawPath(route, routePaint);

        // Pins with pulse rings
        drawPin(canvas, startX, startY, blue, t, 0f);
        drawPin(canvas, endX, endY, mint, t, PIN_PULSE_SECONDS / 2f);

        // Travelling dot
        float progress = (t % ROUTE_PERIOD_SECONDS) / ROUTE_PERIOD_SECONDS;
        measure.getPosTan(progress * routeLength, pos, null);
        canvas.drawCircle(pos[0], pos[1], 11f * density, dotGlowPaint);
        canvas.drawCircle(pos[0], pos[1], 4f * density, dotPaint);
    }

    private void drawPin(Canvas canvas, float x, float y, int color, float t, float offset) {
        float pulse = ((t + offset) % PIN_PULSE_SECONDS) / PIN_PULSE_SECONDS;
        ringPaint.setColor(color);
        ringPaint.setAlpha((int) (130f * (1f - pulse)));
        canvas.drawCircle(x, y, (6f + pulse * 16f) * density, ringPaint);
        pinPaint.setColor(color);
        canvas.drawCircle(x, y, 5f * density, pinPaint);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }
        if (lastFrameNanos != 0L) {
            animTime += (frameTimeNanos - lastFrameNanos) / 1_000_000_000f;
        }
        lastFrameNanos = frameTimeNanos;
        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void start() {
        if (running || reduceEffects) {
            return;
        }
        running = true;
        lastFrameNanos = 0L;
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void stop() {
        running = false;
        lastFrameNanos = 0L;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getWindowVisibility() == VISIBLE) {
            start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            start();
        } else {
            stop();
        }
    }
}