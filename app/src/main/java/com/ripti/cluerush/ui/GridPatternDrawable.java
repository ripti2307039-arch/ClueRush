package com.ripti.cluerush.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

public class GridPatternDrawable extends Drawable {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float cellSize;

    public GridPatternDrawable(@NonNull Context context) {
        linePaint.setColor(ContextCompat.getColor(context, R.color.cr_grid_line));
        linePaint.setStrokeWidth(context.getResources().getDisplayMetrics().density);
        cellSize = context.getResources().getDimension(R.dimen.grid_cell);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        for (float x = bounds.left; x <= bounds.right; x += cellSize) {
            canvas.drawLine(x, bounds.top, x, bounds.bottom, linePaint);
        }
        for (float y = bounds.top; y <= bounds.bottom; y += cellSize) {
            canvas.drawLine(bounds.left, y, bounds.right, y, linePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        linePaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        linePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}