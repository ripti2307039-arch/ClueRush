package com.ripti.cluerush.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;
import com.ripti.cluerush.map.MapMarker;
import com.ripti.cluerush.model.GeoBounds;
import com.ripti.cluerush.util.GeoMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MinimapView extends View {

    private final float density;
    private final int pendingColor;
    private final int completedColor;
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint visionFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint visionStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panelRect = new RectF();
    private final RectF borderRect = new RectF();
    private final RectF areaRect = new RectF();

    private GeoBounds bounds;
    private List<MapMarker> markers = Collections.emptyList();
    private double playerLat = Double.NaN;
    private double playerLng = Double.NaN;
    private double visionMeters;

    public MinimapView(Context context) {
        this(context, null);
    }

    public MinimapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        pendingColor = ContextCompat.getColor(context, R.color.treasure_gold);
        completedColor = ContextCompat.getColor(context, R.color.mint);
        int white = ContextCompat.getColor(context, R.color.white);

        panelPaint.setStyle(Paint.Style.FILL);
        panelPaint.setColor(ContextCompat.getColor(context, R.color.hud_scrim));

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(density);
        borderPaint.setColor(ContextCompat.getColor(context, R.color.hud_stroke));

        areaPaint.setStyle(Paint.Style.FILL);
        areaPaint.setColor(white);
        areaPaint.setAlpha(24);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(density);
        gridPaint.setColor(white);
        gridPaint.setAlpha(28);

        dotPaint.setStyle(Paint.Style.FILL);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1.5f * density);
        ringPaint.setColor(white);

        visionFillPaint.setStyle(Paint.Style.FILL);
        visionFillPaint.setColor(white);
        visionFillPaint.setAlpha(26);

        visionStrokePaint.setStyle(Paint.Style.STROKE);
        visionStrokePaint.setStrokeWidth(density);
        visionStrokePaint.setColor(white);
        visionStrokePaint.setAlpha(90);

        playerPaint.setStyle(Paint.Style.FILL);
        playerPaint.setColor(white);

        playerStrokePaint.setStyle(Paint.Style.STROKE);
        playerStrokePaint.setStrokeWidth(2f * density);
        playerStrokePaint.setColor(ContextCompat.getColor(context, R.color.rush_blue));
    }

    public void setBounds(GeoBounds newBounds) {
        bounds = newBounds;
        invalidate();
    }

    public void setMarkers(List<MapMarker> newMarkers) {
        markers = new ArrayList<>(newMarkers);
        invalidate();
    }

    public void setPlayer(double latitude, double longitude) {
        playerLat = latitude;
        playerLng = longitude;
        invalidate();
    }

    public void setVisionMeters(double meters) {
        visionMeters = meters;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float corner = 12f * density;

        panelRect.set(0f, 0f, w, h);
        canvas.drawRoundRect(panelRect, corner, corner, panelPaint);

        if (bounds != null) {
            drawArena(canvas, w, h);
        }

        float half = density / 2f;
        borderRect.set(half, half, w - half, h - half);
        canvas.drawRoundRect(borderRect, corner, corner, borderPaint);
    }

    private void drawArena(Canvas canvas, float w, float h) {
        double west = bounds.getWest();
        double north = bounds.getNorth();
        double cosLat = Math.cos(Math.toRadians((north + bounds.getSouth()) / 2.0));
        double spanX = (bounds.getEast() - west) * cosLat;
        double spanY = north - bounds.getSouth();
        if (spanX <= 0.0 || spanY <= 0.0) {
            return;
        }

        float pad = 8f * density;
        float scale = (float) Math.min((w - 2f * pad) / spanX, (h - 2f * pad) / spanY);
        float mapW = (float) (spanX * scale);
        float mapH = (float) (spanY * scale);
        float left = (w - mapW) / 2f;
        float top = (h - mapH) / 2f;

        areaRect.set(left, top, left + mapW, top + mapH);
        canvas.drawRoundRect(areaRect, 6f * density, 6f * density, areaPaint);
        for (int i = 1; i <= 2; i++) {
            float x = left + mapW * i / 3f;
            float y = top + mapH * i / 3f;
            canvas.drawLine(x, top, x, top + mapH, gridPaint);
            canvas.drawLine(left, y, left + mapW, y, gridPaint);
        }

        for (MapMarker marker : markers) {
            float x = left + (float) ((marker.getLongitude() - west) * cosLat * scale);
            float y = top + (float) ((north - marker.getLatitude()) * scale);
            if (marker.isDiscovered() || marker.isCompleted()) {
                canvas.drawCircle(x, y, 6.5f * density, ringPaint);
            }
            dotPaint.setColor(marker.isCompleted() ? completedColor : pendingColor);
            canvas.drawCircle(x, y, 3.5f * density, dotPaint);
        }

        if (!Double.isNaN(playerLat) && !Double.isNaN(playerLng)) {
            float px = left + (float) ((playerLng - west) * cosLat * scale);
            float py = top + (float) ((north - playerLat) * scale);
            px = Math.max(left, Math.min(left + mapW, px));
            py = Math.max(top, Math.min(top + mapH, py));
            if (visionMeters > 0.0) {
                float vision = (float) (visionMeters / GeoMath.METERS_PER_DEGREE_LATITUDE * scale);
                canvas.drawCircle(px, py, vision, visionFillPaint);
                canvas.drawCircle(px, py, vision, visionStrokePaint);
            }
            canvas.drawCircle(px, py, 4.5f * density, playerPaint);
            canvas.drawCircle(px, py, 4.5f * density, playerStrokePaint);
        }
    }
}