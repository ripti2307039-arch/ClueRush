package com.ripti.cluerush.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class LabelOverlay extends Overlay {

    private static final double MIN_ZOOM = 15.0;
    private static final double SCALE_PER_ZOOM = 0.125;
    private static final float MIN_SCALE = 0.65f;
    private static final float MAX_SCALE = 1.15f;
    private static final int DIMMED_TEXT_ALPHA = 150;

    private final float density;
    private final int pendingColor;
    private final int completedColor;
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics metrics = new Paint.FontMetrics();
    private final Point point = new Point();
    private final RectF pillRect = new RectF();

    private List<MapMarker> markers = Collections.emptyList();
    private GeoPoint[] geoPoints = new GeoPoint[0];
    private int[] xs = new int[0];
    private int[] ys = new int[0];
    private RectF[] placed = new RectF[0];
    private boolean labelsVisible = true;

    LabelOverlay(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        pendingColor = ContextCompat.getColor(context, R.color.treasure_gold);
        completedColor = ContextCompat.getColor(context, R.color.mint);

        dotPaint.setStyle(Paint.Style.FILL);

        dotStrokePaint.setStyle(Paint.Style.STROKE);
        dotStrokePaint.setColor(ContextCompat.getColor(context, R.color.white));

        pillPaint.setStyle(Paint.Style.FILL);
        pillPaint.setColor(ContextCompat.getColor(context, R.color.hud_scrim));

        pillStrokePaint.setStyle(Paint.Style.STROKE);
        pillStrokePaint.setStrokeWidth(density);
        pillStrokePaint.setColor(ContextCompat.getColor(context, R.color.hud_stroke));

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(ContextCompat.getColor(context, R.color.white));
    }

    void setMarkers(List<MapMarker> newMarkers) {
        markers = new ArrayList<>(newMarkers);
        int count = markers.size();
        geoPoints = new GeoPoint[count];
        xs = new int[count];
        ys = new int[count];
        placed = new RectF[count];
        for (int i = 0; i < count; i++) {
            MapMarker marker = markers.get(i);
            geoPoints[i] = new GeoPoint(marker.getLatitude(), marker.getLongitude());
            placed[i] = new RectF();
        }
    }

    void setLabelsVisible(boolean visible) {
        labelsVisible = visible;
    }

    @Override
    public void draw(Canvas canvas, Projection projection) {
        int count = markers.size();
        if (count == 0) {
            return;
        }

        Rect clip = canvas.getClipBounds();
        float margin = 120f * density;
        float scale = scaleFor(projection.getZoomLevel());
        float dotRadius = (labelsVisible ? 6f : 8f) * density * scale;
        float padH = 8f * density * scale;
        float padV = 3f * density * scale;
        float gap = 5f * density * scale;
        float corner = 8f * density * scale;

        dotStrokePaint.setStrokeWidth(2f * density * scale);
        textPaint.setTextSize(13f * density * scale);
        textPaint.getFontMetrics(metrics);
        float textHeight = metrics.descent - metrics.ascent;

        for (int i = 0; i < count; i++) {
            projection.toPixels(geoPoints[i], point);
            xs[i] = point.x;
            ys[i] = point.y;
        }

        for (int i = 0; i < count; i++) {
            if (!onScreen(xs[i], ys[i], clip, margin)) {
                continue;
            }
            dotPaint.setColor(markers.get(i).isCompleted() ? completedColor : pendingColor);
            canvas.drawCircle(xs[i], ys[i], dotRadius, dotPaint);
            canvas.drawCircle(xs[i], ys[i], dotRadius, dotStrokePaint);
        }

        if (!labelsVisible) {
            return;
        }

        int placedCount = 0;
        for (int i = 0; i < count; i++) {
            if (!onScreen(xs[i], ys[i], clip, margin)) {
                continue;
            }
            MapMarker marker = markers.get(i);
            String name = marker.getName();
            float width = textPaint.measureText(name) + padH * 2f;
            float height = textHeight + padV * 2f;
            float left = xs[i] - width / 2f;
            float bottom = ys[i] - dotRadius - gap;
            pillRect.set(left, bottom - height, left + width, bottom);

            if (collides(placedCount)) {
                continue;
            }
            placed[placedCount].set(pillRect);
            placedCount++;

            canvas.drawRoundRect(pillRect, corner, corner, pillPaint);
            canvas.drawRoundRect(pillRect, corner, corner, pillStrokePaint);
            textPaint.setAlpha(marker.isCompleted() ? DIMMED_TEXT_ALPHA : 255);
            float baseline = pillRect.centerY() - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(name, pillRect.centerX(), baseline, textPaint);
        }
    }

    private boolean collides(int placedCount) {
        for (int i = 0; i < placedCount; i++) {
            if (RectF.intersects(placed[i], pillRect)) {
                return true;
            }
        }
        return false;
    }

    private boolean onScreen(int x, int y, Rect clip, float margin) {
        return x >= clip.left - margin && x <= clip.right + margin
                && y >= clip.top - margin && y <= clip.bottom + margin;
    }

    private float scaleFor(double zoom) {
        float scale = (float) (MIN_SCALE + (zoom - MIN_ZOOM) * SCALE_PER_ZOOM);
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }
}