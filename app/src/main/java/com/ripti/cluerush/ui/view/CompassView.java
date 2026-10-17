package com.ripti.cluerush.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

public class CompassView extends View {

    private static final float SMOOTHING = 0.3f;
    private static final float MIN_STEP_DEGREES = 0.25f;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();

    private float centerX;
    private float centerY;
    private float displayedBearing;

    public CompassView(Context context) {
        this(context, null);
    }

    public CompassView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(ContextCompat.getColor(context, R.color.treasure_gold));

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2f * density);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setColor(ContextCompat.getColor(context, R.color.navy_950));
    }

    public void setBearing(float bearingDegrees) {
        float delta = ((bearingDegrees - displayedBearing + 540f) % 360f) - 180f;
        if (Math.abs(delta) < MIN_STEP_DEGREES) {
            return;
        }
        displayedBearing = (displayedBearing + delta * SMOOTHING + 360f) % 360f;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        float radius = Math.min(w, h) * 0.39f;
        float size = Math.min(w, h) * 0.09f;

        arrow.reset();
        arrow.moveTo(centerX, centerY - radius - size);
        arrow.lineTo(centerX + size * 0.75f, centerY - radius + size * 0.55f);
        arrow.lineTo(centerX, centerY - radius + size * 0.2f);
        arrow.lineTo(centerX - size * 0.75f, centerY - radius + size * 0.55f);
        arrow.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.rotate(displayedBearing, centerX, centerY);
        canvas.drawPath(arrow, fillPaint);
        canvas.drawPath(arrow, strokePaint);
        canvas.restore();
    }
}