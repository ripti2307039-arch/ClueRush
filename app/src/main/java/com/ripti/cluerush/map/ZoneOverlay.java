package com.ripti.cluerush.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.SystemClock;

import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;
import com.ripti.cluerush.util.GeoMath;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

class ZoneOverlay extends Overlay {

    private static final long FADE_MS = 220L;
    private static final long RIPPLE_MS = 800L;
    private static final long NO_RIPPLE = -1L;

    private final MapView mapView;
    private final float density;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Point centerPixel = new Point();
    private final Point edgePixel = new Point();

    private GeoPoint center;
    private GeoPoint edge;
    private boolean visible;
    private long shownAt;
    private long rippleStartedAt = NO_RIPPLE;

    ZoneOverlay(Context context, MapView mapView) {
        this.mapView = mapView;
        density = context.getResources().getDisplayMetrics().density;
        int gold = ContextCompat.getColor(context, R.color.treasure_gold);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(gold);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(10f * density);
        glowPaint.setColor(gold);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3f * density);
        ringPaint.setColor(gold);

        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(3f * density);
        ripplePaint.setColor(ContextCompat.getColor(context, R.color.white));
    }

    void show(double latitude, double longitude, double radiusMeters) {
        center = new GeoPoint(latitude, longitude);
        edge = new GeoPoint(latitude,
                longitude + GeoMath.metersToLongitudeDegrees(radiusMeters, latitude));
        if (!visible) {
            visible = true;
            shownAt = SystemClock.uptimeMillis();
        }
        mapView.invalidate();
    }

    void hide() {
        visible = false;
        rippleStartedAt = NO_RIPPLE;
        mapView.invalidate();
    }

    void ripple() {
        rippleStartedAt = SystemClock.uptimeMillis();
        mapView.invalidate();
    }

    @Override
    public void draw(Canvas canvas, Projection projection) {
        if (!visible || center == null || edge == null) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        float fade = Math.min(1f, (now - shownAt) / (float) FADE_MS);

        projection.toPixels(center, centerPixel);
        projection.toPixels(edge, edgePixel);
        float cx = centerPixel.x;
        float cy = centerPixel.y;
        float radius = Math.max(8f * density, Math.abs((float) edgePixel.x - cx));

        fillPaint.setAlpha((int) (40f * fade));
        canvas.drawCircle(cx, cy, radius, fillPaint);
        glowPaint.setAlpha((int) (70f * fade));
        canvas.drawCircle(cx, cy, radius, glowPaint);
        ringPaint.setAlpha((int) (255f * fade));
        canvas.drawCircle(cx, cy, radius, ringPaint);

        boolean animating = fade < 1f;
        if (rippleStartedAt != NO_RIPPLE) {
            float t = (now - rippleStartedAt) / (float) RIPPLE_MS;
            if (t >= 1f) {
                rippleStartedAt = NO_RIPPLE;
            } else {
                ripplePaint.setAlpha((int) (200f * (1f - t)));
                canvas.drawCircle(cx, cy, radius * (0.4f + 1.1f * t), ripplePaint);
                animating = true;
            }
        }
        if (animating) {
            mapView.postInvalidateOnAnimation();
        }
    }
}