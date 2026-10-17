package com.ripti.cluerush.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

public class VignetteView extends View {

    private final Paint paint = new Paint();
    private final int edgeColor;

    public VignetteView(Context context) {
        this(context, null);
    }

    public VignetteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        edgeColor = ContextCompat.getColor(context, R.color.vignette_edge);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        RadialGradient gradient = new RadialGradient(w / 2f, h / 2f, w * 0.62f,
                new int[]{Color.TRANSPARENT, Color.TRANSPARENT, edgeColor},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        matrix.setScale(1f, (float) h / w, w / 2f, h / 2f);
        gradient.setLocalMatrix(matrix);
        paint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
    }
}