package com.ripti.cluerush.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

public class JoystickView extends View {

    public interface Listener {
        void onMove(float x, float y);
    }

    private static final float DEAD_ZONE = 0.12f;
    private static final int NO_POINTER = -1;

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Listener listener;
    private int activePointerId = NO_POINTER;
    private float centerX;
    private float centerY;
    private float baseRadius;
    private float knobRadius;
    private float knobX;
    private float knobY;

    public JoystickView(Context context) {
        this(context, null);
    }

    public JoystickView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;

        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setColor(ContextCompat.getColor(context, R.color.hud_scrim));

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f * density);
        ringPaint.setColor(ContextCompat.getColor(context, R.color.hud_stroke));

        innerRingPaint.setStyle(Paint.Style.STROKE);
        innerRingPaint.setStrokeWidth(density);
        innerRingPaint.setColor(ContextCompat.getColor(context, R.color.hud_stroke));
        innerRingPaint.setAlpha(70);

        knobPaint.setStyle(Paint.Style.FILL);
        knobPaint.setColor(ContextCompat.getColor(context, R.color.rush_blue));

        knobStrokePaint.setStyle(Paint.Style.STROKE);
        knobStrokePaint.setStrokeWidth(3f * density);
        knobStrokePaint.setColor(ContextCompat.getColor(context, R.color.white));
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void reset() {
        release();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        baseRadius = Math.min(w, h) / 2f - ringPaint.getStrokeWidth();
        knobRadius = baseRadius * 0.38f;
        knobX = centerX;
        knobY = centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(centerX, centerY, baseRadius, ringPaint);
        canvas.drawCircle(centerX, centerY, baseRadius * 0.5f, innerRingPaint);
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint);
        canvas.drawCircle(knobX, knobY, knobRadius, knobStrokePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                updateKnob(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE: {
                int index = event.findPointerIndex(activePointerId);
                if (index >= 0) {
                    updateKnob(event.getX(index), event.getY(index));
                }
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int index = event.getActionIndex();
                if (event.getPointerId(index) == activePointerId) {
                    release();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                release();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void updateKnob(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        float distance = (float) Math.hypot(dx, dy);
        float clamped = Math.min(distance, baseRadius);

        if (distance > 0f) {
            knobX = centerX + dx / distance * clamped;
            knobY = centerY + dy / distance * clamped;
        } else {
            knobX = centerX;
            knobY = centerY;
        }

        float magnitude = baseRadius > 0f ? clamped / baseRadius : 0f;
        float outX = 0f;
        float outY = 0f;
        if (magnitude > DEAD_ZONE && distance > 0f) {
            float scaled = (magnitude - DEAD_ZONE) / (1f - DEAD_ZONE);
            outX = dx / distance * scaled;
            outY = -dy / distance * scaled;
        }

        invalidate();
        dispatchMove(outX, outY);
    }

    private void release() {
        activePointerId = NO_POINTER;
        knobX = centerX;
        knobY = centerY;
        invalidate();
        dispatchMove(0f, 0f);
    }

    private void dispatchMove(float x, float y) {
        if (listener != null) {
            listener.onMove(x, y);
        }
    }
}