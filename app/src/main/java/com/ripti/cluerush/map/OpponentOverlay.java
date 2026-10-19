package com.ripti.cluerush.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

class OpponentOverlay extends Overlay {

    private final float density;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics metrics = new Paint.FontMetrics();
    private final Point pixel = new Point();
    private final GeoPoint point = new GeoPoint(0.0, 0.0);

    private boolean visible;
    private String label = "";

    OpponentOverlay(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(ContextCompat.getColor(context, R.color.heat_hot));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3f * density);
        strokePaint.setColor(ContextCompat.getColor(context, R.color.white));
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(13f * density);
        textPaint.setColor(ContextCompat.getColor(context, R.color.white));
    }

    void set(double latitude, double longitude, boolean visible, String label) {
        point.setCoords(latitude, longitude);
        this.visible = visible;
        this.label = label == null ? "" : label;
    }

    @Override
    public void draw(Canvas canvas, Projection projection) {
        if (!visible) {
            return;
        }
        projection.toPixels(point, pixel);
        float radius = 14f * density;
        canvas.drawCircle(pixel.x, pixel.y, radius, fillPaint);
        canvas.drawCircle(pixel.x, pixel.y, radius, strokePaint);
        textPaint.getFontMetrics(metrics);
        float baseline = pixel.y - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, pixel.x, baseline, textPaint);
    }
}