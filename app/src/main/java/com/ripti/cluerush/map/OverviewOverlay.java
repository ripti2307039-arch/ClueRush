package com.ripti.cluerush.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class OverviewOverlay extends Overlay {

    private static final int DIMMED_TEXT_ALPHA = 150;

    private final float density;
    private final int pendingColor;
    private final int completedColor;
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics metrics = new Paint.FontMetrics();
    private final Point point = new Point();
    private final RectF pillRect = new RectF();

    private List<MapMarker> markers = Collections.emptyList();
    private GeoPoint[] geoPoints = new GeoPoint[0];
    private int[] xs = new int[0];
    private int[] ys = new int[0];
    private RectF[] placed = new RectF[0];
    private GeoPoint player;
    private GeoPoint picked;
    private boolean revealAll;

    OverviewOverlay(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        pendingColor = ContextCompat.getColor(context, R.color.treasure_gold);
        completedColor = ContextCompat.getColor(context, R.color.mint);

        dotPaint.setStyle(Paint.Style.FILL);

        dotStrokePaint.setStyle(Paint.Style.STROKE);
        dotStrokePaint.setStrokeWidth(2f * density);
        dotStrokePaint.setColor(ContextCompat.getColor(context, R.color.white));

        pillPaint.setStyle(Paint.Style.FILL);
        pillPaint.setColor(ContextCompat.getColor(context, R.color.hud_scrim));

        pillStrokePaint.setStyle(Paint.Style.STROKE);
        pillStrokePaint.setStrokeWidth(density);
        pillStrokePaint.setColor(ContextCompat.getColor(context, R.color.hud_stroke));

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(13f * density);
        textPaint.setColor(ContextCompat.getColor(context, R.color.white));

        playerPaint.setStyle(Paint.Style.FILL);
        playerPaint.setColor(ContextCompat.getColor(context, R.color.rush_blue));

        playerStrokePaint.setStyle(Paint.Style.STROKE);
        playerStrokePaint.setStrokeWidth(3f * density);
        playerStrokePaint.setColor(ContextCompat.getColor(context, R.color.white));

        pickPaint.setStyle(Paint.Style.STROKE);
        pickPaint.setStrokeWidth(3f * density);
        pickPaint.setColor(ContextCompat.getColor(context, R.color.heat_hot));
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

    void setPlayer(double latitude, double longitude) {
        player = new GeoPoint(latitude, longitude);
    }

    void setPicked(GeoPoint point) {
        picked = point;
    }

    void setRevealAll(boolean reveal) {
        revealAll = reveal;
    }

    @Override
    public void draw(Canvas canvas, Projection projection) {
        int count = markers.size();
        float dotRadius = 7f * density;
        float padH = 8f * density;
        float padV = 3f * density;
        float gap = 5f * density;
        float corner = 8f * density;

        textPaint.getFontMetrics(metrics);
        float textHeight = metrics.descent - metrics.ascent;

        for (int i = 0; i < count; i++) {
            projection.toPixels(geoPoints[i], point);
            xs[i] = point.x;
            ys[i] = point.y;
            dotPaint.setColor(markers.get(i).isCompleted() ? completedColor : pendingColor);
            canvas.drawCircle(xs[i], ys[i], dotRadius, dotPaint);
            canvas.drawCircle(xs[i], ys[i], dotRadius, dotStrokePaint);
        }

        int placedCount = 0;
        for (int i = 0; i < count; i++) {
            MapMarker marker = markers.get(i);
            if (!revealAll && !marker.isDiscovered() && !marker.isCompleted()) {
                continue;
            }
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

        if (player != null) {
            projection.toPixels(player, point);
            canvas.drawCircle(point.x, point.y, 9f * density, playerPaint);
            canvas.drawCircle(point.x, point.y, 9f * density, playerStrokePaint);
        }

        if (picked != null) {
            projection.toPixels(picked, point);
            float ring = 12f * density;
            float arm = 22f * density;
            canvas.drawCircle(point.x, point.y, ring, pickPaint);
            canvas.drawLine(point.x - arm, point.y, point.x + arm, point.y, pickPaint);
            canvas.drawLine(point.x, point.y - arm, point.x, point.y + arm, pickPaint);
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
}