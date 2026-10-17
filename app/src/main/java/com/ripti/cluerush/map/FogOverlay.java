package com.ripti.cluerush.map;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.ripti.cluerush.util.GeoMath;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

class FogOverlay extends Overlay {

    private static final int FOG_RGB = 0x0B1220;
    private static final int FOG_ALPHA = 0xE6;
    private static final float CLEAR_UNTIL = 0.55f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RadialGradient shader;
    private final Matrix matrix = new Matrix();
    private final Point centerPixel = new Point();
    private final Point edgePixel = new Point();
    private final GeoPoint center = new GeoPoint(0.0, 0.0);
    private final GeoPoint edge = new GeoPoint(0.0, 0.0);

    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private double radiusMeters;

    FogOverlay() {
        int clear = FOG_RGB;
        int dense = (FOG_ALPHA << 24) | FOG_RGB;
        shader = new RadialGradient(0f, 0f, 1f,
                new int[]{clear, clear, dense},
                new float[]{0f, CLEAR_UNTIL, 1f},
                Shader.TileMode.CLAMP);
    }

    void setPlayer(double lat, double lng) {
        latitude = lat;
        longitude = lng;
    }

    void setRadiusMeters(double meters) {
        radiusMeters = meters;
    }

    @Override
    public void draw(Canvas canvas, Projection projection) {
        if (radiusMeters <= 0.0 || Double.isNaN(latitude) || Double.isNaN(longitude)) {
            return;
        }
        center.setCoords(latitude, longitude);
        edge.setCoords(latitude,
                longitude + GeoMath.metersToLongitudeDegrees(radiusMeters, latitude));
        projection.toPixels(center, centerPixel);
        projection.toPixels(edge, edgePixel);

        float radius = Math.max(1f, Math.abs((float) edgePixel.x - centerPixel.x));
        matrix.setScale(radius, radius);
        matrix.postTranslate(centerPixel.x, centerPixel.y);
        shader.setLocalMatrix(matrix);
        paint.setShader(shader);
        canvas.drawRect(canvas.getClipBounds(), paint);
    }
}