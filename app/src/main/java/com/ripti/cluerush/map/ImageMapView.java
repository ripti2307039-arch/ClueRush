package com.ripti.cluerush.map;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ImageMapView extends View {

    private static final double VISIBLE_METERS = 220.0;
    private static final double REFERENCE_ZOOM = 18.0;
    private static final double MAX_ZOOM = 19.5;
    private static final double METERS = IslandSpace.SIZE_METERS;
    private static final int OCEAN = 0xFF0B2A4A;
    private static final int MAX_TRAIL = 160;
    private static final float[] NIGHT_MATRIX = {
            0.45f, 0f, 0f, 0f, 0f,
            0f, 0.55f, 0f, 0f, 0f,
            0f, 0f, 0.80f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
    };

    private final float density;
    private final int pendingColor;
    private final int completedColor;
    private final int white;
    private final int blue;
    private final int hot;
    private final Paint mapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint fogPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dest = new RectF();
    private final List<double[]> trail = new ArrayList<>();
    private final GestureDetector gestures;
    private final ScaleGestureDetector scaler;

    private Bitmap map;
    private double centerX;
    private double centerY;
    private double zoom = REFERENCE_ZOOM;
    private double fitZoom = REFERENCE_ZOOM - 1.5;
    private List<MapMarker> markers = Collections.emptyList();
    private double playerX = Double.NaN;
    private double playerY = Double.NaN;
    private double visionMeters;
    private double oppX = Double.NaN;
    private double oppY = Double.NaN;
    private boolean oppVisible;
    private String oppLabel = "";
    private boolean zoneOn;
    private double zoneX;
    private double zoneY;
    private double zoneR;
    private float rippleT = -1f;
    private double pickX = Double.NaN;
    private double pickY = Double.NaN;
    private boolean fogEnabled;
    private boolean revealAll;
    private boolean showAllPins;
    private boolean drawPlayer;
    private boolean interactive;
    private boolean pickEnabled;
    private OverviewMap.OnPickListener pickListener;
    private ValueAnimator rippleAnimator;
    private float viewW;
    private float viewH;
    private double ppm = 1.0;
    private float fogRadius = -1f;

    public ImageMapView(Context context) {
        this(context, null);
    }

    public ImageMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        pendingColor = ContextCompat.getColor(context, R.color.treasure_gold);
        completedColor = ContextCompat.getColor(context, R.color.mint);
        white = ContextCompat.getColor(context, R.color.white);
        blue = ContextCompat.getColor(context, R.color.rush_blue);
        hot = ContextCompat.getColor(context, R.color.heat_hot);

        stroke.setStyle(Paint.Style.STROKE);
        fill.setStyle(Paint.Style.FILL);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(11f * density);
        text.setFakeBoldText(true);
        text.setShadowLayer(3f * density, 0f, 0f, 0xFF000000);

        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                centerX = clampCenter(centerX + dx / ppm);
                centerY = clampCenter(centerY - dy / ppm);
                invalidate();
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (!pickEnabled || pickListener == null) {
                    return false;
                }
                double x = centerX + (e.getX() - viewW / 2f) / ppm;
                double y = centerY - (e.getY() - viewH / 2f) / ppm;
                pickListener.onPick(IslandSpace.latitudeOf(y), IslandSpace.longitudeOf(x));
                return true;
            }
        });
        scaler = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        double next = zoom + Math.log(detector.getScaleFactor()) / Math.log(2.0);
                        zoom = Math.max(fitZoom - 0.3, Math.min(MAX_ZOOM, next));
                        invalidate();
                        return true;
                    }
                });
    }

    // ---- configuration ----

    public void setMap(Bitmap bitmap) {
        map = bitmap;
        invalidate();
    }

    public void setInteractive(boolean value) {
        interactive = value;
    }

    public void setFogEnabled(boolean value) {
        fogEnabled = value;
        invalidate();
    }

    public void setRevealAll(boolean value) {
        revealAll = value;
        invalidate();
    }

    public void setShowAllPins(boolean value) {
        showAllPins = value;
        invalidate();
    }

    public void setDrawPlayer(boolean value) {
        drawPlayer = value;
        invalidate();
    }

    public void setPickEnabled(boolean value) {
        pickEnabled = value;
    }

    public void setPickListener(OverviewMap.OnPickListener listener) {
        pickListener = listener;
    }

    public void setNight(boolean night) {
        mapPaint.setColorFilter(night
                ? new ColorMatrixColorFilter(new ColorMatrix(NIGHT_MATRIX)) : null);
        invalidate();
    }

    // ---- camera ----

    public void setZoomLevel(double value) {
        zoom = value;
        invalidate();
    }

    public void setCenterLatLng(double latitude, double longitude) {
        centerX = IslandSpace.xMeters(longitude);
        centerY = IslandSpace.yMeters(latitude);
        invalidate();
    }

    public void fitToView() {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        double basePpm = getHeight() / VISIBLE_METERS;
        double fitPpm = Math.min(getWidth(), getHeight()) / METERS * 0.96;
        fitZoom = REFERENCE_ZOOM + Math.log(fitPpm / basePpm) / Math.log(2.0);
        zoom = fitZoom;
        centerX = 0.0;
        centerY = 0.0;
        invalidate();
    }

    private static double clampCenter(double value) {
        double half = METERS / 2.0;
        return Math.max(-half, Math.min(half, value));
    }

    // ---- content ----

    public void setMarkerList(List<MapMarker> list) {
        markers = new ArrayList<>(list);
        invalidate();
    }

    public void setPlayer(double latitude, double longitude) {
        playerX = IslandSpace.xMeters(longitude);
        playerY = IslandSpace.yMeters(latitude);
        invalidate();
    }

    public void setVisionMeters(double meters) {
        visionMeters = meters;
        fogRadius = -1f;
        invalidate();
    }

    public void setOpponent(double latitude, double longitude, boolean visible, String label) {
        oppX = IslandSpace.xMeters(longitude);
        oppY = IslandSpace.yMeters(latitude);
        oppVisible = visible;
        oppLabel = label == null ? "" : label;
        invalidate();
    }

    public void showZone(double latitude, double longitude, double radiusMeters) {
        zoneX = IslandSpace.xMeters(longitude);
        zoneY = IslandSpace.yMeters(latitude);
        zoneR = radiusMeters;
        zoneOn = true;
        invalidate();
    }

    public void hideZone() {
        zoneOn = false;
        invalidate();
    }

    public void ripple() {
        if (rippleAnimator != null) {
            rippleAnimator.cancel();
        }
        rippleAnimator = ValueAnimator.ofFloat(0f, 1f);
        rippleAnimator.setDuration(700L);
        rippleAnimator.addUpdateListener(animator -> {
            rippleT = (float) animator.getAnimatedValue();
            invalidate();
        });
        rippleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rippleT = -1f;
                invalidate();
            }
        });
        rippleAnimator.start();
    }

    public void addTrailPoint(double latitude, double longitude) {
        trail.add(new double[]{IslandSpace.xMeters(longitude), IslandSpace.yMeters(latitude)});
        if (trail.size() > MAX_TRAIL) {
            trail.remove(0);
        }
    }

    public void setPicked(double latitude, double longitude) {
        pickX = IslandSpace.xMeters(longitude);
        pickY = IslandSpace.yMeters(latitude);
        invalidate();
    }

    public void clearPicked() {
        pickX = Double.NaN;
        pickY = Double.NaN;
        invalidate();
    }

    // ---- touch ----

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactive) {
            return super.onTouchEvent(event);
        }
        scaler.onTouchEvent(event);
        if (!scaler.isInProgress()) {
            gestures.onTouchEvent(event);
        }
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (rippleAnimator != null) {
            rippleAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }

    // ---- drawing ----

    private float sx(double xMeters) {
        return (float) (viewW / 2f + (xMeters - centerX) * ppm);
    }

    private float sy(double yMeters) {
        return (float) (viewH / 2f - (yMeters - centerY) * ppm);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        viewW = getWidth();
        viewH = getHeight();
        canvas.drawColor(OCEAN);
        if (map == null || viewH <= 0f) {
            return;
        }
        ppm = (viewH / VISIBLE_METERS) * Math.pow(2.0, zoom - REFERENCE_ZOOM);

        double half = METERS / 2.0;
        dest.set(sx(-half), sy(half), sx(half), sy(-half));
        canvas.drawBitmap(map, null, dest, mapPaint);

        drawTrail(canvas);
        if (fogEnabled) {
            drawFog(canvas);
        }
        drawZone(canvas);
        drawMarkers(canvas);
        drawOpponent(canvas);
        drawPicked(canvas);
        if (drawPlayer) {
            drawPlayerDot(canvas);
        }
    }

    private void drawTrail(Canvas canvas) {
        fill.setColor(white);
        int size = trail.size();
        for (int i = 0; i < size; i++) {
            double[] point = trail.get(i);
            fill.setAlpha(20 + (int) (90f * i / Math.max(1, size)));
            canvas.drawCircle(sx(point[0]), sy(point[1]), 2.5f * density, fill);
        }
        fill.setAlpha(255);
    }

    private void drawFog(Canvas canvas) {
        if (Double.isNaN(playerX) || visionMeters <= 0.0) {
            return;
        }
        float radius = (float) (visionMeters * ppm);
        if (fogPaint.getShader() == null || Math.abs(radius - fogRadius) > 0.5f) {
            fogRadius = radius;
            fogPaint.setShader(new RadialGradient(0f, 0f, radius,
                    new int[]{0x00000000, 0x00000000, 0xB004101C},
                    new float[]{0f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        }
        float px = sx(playerX);
        float py = sy(playerY);
        canvas.save();
        canvas.translate(px, py);
        canvas.drawRect(-px, -py, viewW - px, viewH - py, fogPaint);
        canvas.restore();

        stroke.setStrokeWidth(density);
        stroke.setColor(white);
        stroke.setAlpha(70);
        canvas.drawCircle(px, py, radius, stroke);
        stroke.setAlpha(255);
    }

    private void drawZone(Canvas canvas) {
        if (!zoneOn) {
            return;
        }
        float x = sx(zoneX);
        float y = sy(zoneY);
        float radius = (float) (zoneR * ppm);
        fill.setColor(pendingColor);
        fill.setAlpha(45);
        canvas.drawCircle(x, y, radius, fill);
        fill.setAlpha(255);
        stroke.setStrokeWidth(2f * density);
        stroke.setColor(pendingColor);
        canvas.drawCircle(x, y, radius, stroke);
        if (rippleT >= 0f) {
            stroke.setAlpha((int) (200f * (1f - rippleT)));
            canvas.drawCircle(x, y, radius * (0.5f + 0.8f * rippleT), stroke);
            stroke.setAlpha(255);
        }
    }

    private void drawMarkers(Canvas canvas) {
        boolean playerKnown = !Double.isNaN(playerX);
        for (MapMarker marker : markers) {
            double mx = IslandSpace.xMeters(marker.getLongitude());
            double my = IslandSpace.yMeters(marker.getLatitude());
            boolean known = marker.isDiscovered() || marker.isCompleted();
            boolean near = playerKnown && Math.hypot(mx - playerX, my - playerY) <= visionMeters;
            if (!revealAll && !showAllPins && !known && !near) {
                continue;
            }
            float x = sx(mx);
            float y = sy(my);
            float pin = 7f * density;
            if (known) {
                stroke.setStrokeWidth(1.5f * density);
                stroke.setColor(white);
                canvas.drawCircle(x, y, pin + 4f * density, stroke);
            }
            fill.setColor(marker.isCompleted() ? completedColor : pendingColor);
            canvas.drawCircle(x, y, pin, fill);
            stroke.setStrokeWidth(2f * density);
            stroke.setColor(0xFF0B1B2B);
            canvas.drawCircle(x, y, pin, stroke);
            if (known || revealAll) {
                text.setColor(white);
                canvas.drawText(marker.getName(), x, y + pin + 14f * density, text);
            }
        }
    }

    private void drawOpponent(Canvas canvas) {
        if (!oppVisible || Double.isNaN(oppX)) {
            return;
        }
        float x = sx(oppX);
        float y = sy(oppY);
        fill.setColor(hot);
        canvas.drawCircle(x, y, 9f * density, fill);
        stroke.setStrokeWidth(2f * density);
        stroke.setColor(white);
        canvas.drawCircle(x, y, 9f * density, stroke);
        text.setColor(white);
        canvas.drawText(oppLabel.toUpperCase(Locale.ROOT), x, y + 4f * density, text);
    }

    private void drawPicked(Canvas canvas) {
        if (Double.isNaN(pickX)) {
            return;
        }
        float x = sx(pickX);
        float y = sy(pickY);
        stroke.setStrokeWidth(2f * density);
        stroke.setColor(0xFFFF3B30);
        canvas.drawCircle(x, y, 8f * density, stroke);
        canvas.drawLine(x - 12f * density, y, x + 12f * density, y, stroke);
        canvas.drawLine(x, y - 12f * density, x, y + 12f * density, stroke);
    }

    private void drawPlayerDot(Canvas canvas) {
        if (Double.isNaN(playerX)) {
            return;
        }
        float x = sx(playerX);
        float y = sy(playerY);
        fill.setColor(white);
        canvas.drawCircle(x, y, 6f * density, fill);
        stroke.setStrokeWidth(2f * density);
        stroke.setColor(blue);
        canvas.drawCircle(x, y, 6f * density, stroke);
    }
}