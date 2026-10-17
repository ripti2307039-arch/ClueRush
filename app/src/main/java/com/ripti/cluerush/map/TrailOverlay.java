package com.ripti.cluerush.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.SystemClock;

import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

class TrailOverlay extends Overlay {

    private static final int CAPACITY = 24;
    private static final long LIFE_MS = 1200L;

    private final MapView mapView;
    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GeoPoint[] points = new GeoPoint[CAPACITY];
    private final long[] times = new long[CAPACITY];
    private final Point pixel = new Point();
    private int head;
    private int count;

    TrailOverlay(Context context, MapView mapView) {
        this.mapView = mapView;
        density = context.getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(context, R.color.rush_blue));
    }

    void add(double latitude, double longitude) {
        points[head] = new GeoPoint(latitude, longitude);
        times[head] = SystemClock.uptimeMillis();
        head = (head + 1) % CAPACITY;
        count = Math.min(count + 1, CAPACITY);
        mapView.postInvalidateOnAnimation();
    }

    @Override
    public void draw(Canvas canvas, Projection projection) {
        if (count == 0) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        int alive = 0;
        for (int i = 0; i < count; i++) {
            int index = (head - 1 - i + CAPACITY) % CAPACITY;
            long age = now - times[index];
            if (age >= LIFE_MS) {
                break;
            }
            float life = 1f - age / (float) LIFE_MS;
            projection.toPixels(points[index], pixel);
            float side = index % 2 == 0 ? -1f : 1f;
            paint.setAlpha((int) (150f * life));
            canvas.drawCircle(pixel.x + side * 3f * density, pixel.y,
                    (2.5f + 1.5f * life) * density, paint);
            alive++;
        }

        count = alive;
        if (alive > 0) {
            mapView.postInvalidateOnAnimation();
        }
    }
}