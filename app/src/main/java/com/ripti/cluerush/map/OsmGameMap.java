package com.ripti.cluerush.map;

import android.annotation.SuppressLint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import com.ripti.cluerush.model.GeoBounds;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

import java.util.List;

public class OsmGameMap implements GameMap {

    private static final double MIN_ZOOM = 15.0;
    private static final double MAX_ZOOM = 19.0;
    private static final double SCROLL_PADDING_DEGREES = 0.012;

    private static final float[] NIGHT_MATRIX = {
            0.45f, 0f, 0f, 0f, 0f,
            0f, 0.55f, 0f, 0f, 0f,
            0f, 0f, 0.80f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
    };

    private final MapView mapView;
    private final TrailOverlay trailOverlay;
    private final ZoneOverlay zoneOverlay;
    private final LabelOverlay labelOverlay;

    @SuppressLint("ClickableViewAccessibility")
    public OsmGameMap(MapView mapView) {
        this.mapView = mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(false);
        mapView.setTilesScaledToDpi(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.setHorizontalMapRepetitionEnabled(false);
        mapView.setVerticalMapRepetitionEnabled(false);
        mapView.setMinZoomLevel(MIN_ZOOM);
        mapView.setMaxZoomLevel(MAX_ZOOM);
        mapView.setOnTouchListener((view, event) -> true);

        trailOverlay = new TrailOverlay(mapView.getContext(), mapView);
        zoneOverlay = new ZoneOverlay(mapView.getContext(), mapView);
        labelOverlay = new LabelOverlay(mapView.getContext());
        mapView.getOverlays().add(trailOverlay);
        mapView.getOverlays().add(zoneOverlay);
        mapView.getOverlays().add(labelOverlay);
    }

    @Override
    public void setBounds(GeoBounds movementBounds) {
        GeoBounds padded = movementBounds.expandedBy(SCROLL_PADDING_DEGREES);
        mapView.setScrollableAreaLimitDouble(new BoundingBox(
                padded.getNorth(), padded.getEast(), padded.getSouth(), padded.getWest()));
    }

    @Override
    public void setZoom(double zoom) {
        mapView.getController().setZoom(zoom);
    }

    @Override
    public void moveCamera(double latitude, double longitude) {
        mapView.getController().setCenter(new GeoPoint(latitude, longitude));
    }

    @Override
    public void setNightStyle(boolean night) {
        TilesOverlay tiles = mapView.getOverlayManager().getTilesOverlay();
        if (night) {
            tiles.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(NIGHT_MATRIX)));
        } else {
            tiles.setColorFilter(null);
        }
    }

    @Override
    public void setMarkers(List<MapMarker> markers) {
        labelOverlay.setMarkers(markers);
        mapView.invalidate();
    }

    @Override
    public void setLabelsVisible(boolean visible) {
        labelOverlay.setLabelsVisible(visible);
        mapView.invalidate();
    }

    @Override
    public void showZone(double latitude, double longitude, double radiusMeters) {
        zoneOverlay.show(latitude, longitude, radiusMeters);
    }

    @Override
    public void hideZone() {
        zoneOverlay.hide();
    }

    @Override
    public void playZoneRipple() {
        zoneOverlay.ripple();
    }

    @Override
    public void addTrailPoint(double latitude, double longitude) {
        trailOverlay.add(latitude, longitude);
    }

    @Override
    public void onResume() {
        mapView.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        mapView.onDetach();
    }
}