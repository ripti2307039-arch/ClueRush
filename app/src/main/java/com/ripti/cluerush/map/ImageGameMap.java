package com.ripti.cluerush.map;

import android.graphics.Bitmap;

import com.ripti.cluerush.model.GeoBounds;

import java.util.List;

public class ImageGameMap implements GameMap {

    private final ImageMapView view;

    public ImageGameMap(ImageMapView view, Bitmap bitmap) {
        this.view = view;
        view.setMap(bitmap);
        view.setFogEnabled(true);
        view.setDrawPlayer(false);
        view.setInteractive(false);
    }

    @Override
    public void setBounds(GeoBounds movementBounds) {
    }

    @Override
    public void setZoom(double zoom) {
        view.setZoomLevel(zoom);
    }

    @Override
    public void moveCamera(double latitude, double longitude) {
        view.setCenterLatLng(latitude, longitude);
    }

    @Override
    public void setNightStyle(boolean night) {
        view.setNight(night);
    }

    @Override
    public void setMarkers(List<MapMarker> markers) {
        view.setMarkerList(markers);
    }

    @Override
    public void setPlayerPosition(double latitude, double longitude) {
        view.setPlayer(latitude, longitude);
    }

    @Override
    public void setVisionRadius(double meters) {
        view.setVisionMeters(meters);
    }

    @Override
    public void setOpponent(double latitude, double longitude, boolean visible, String label) {
        view.setOpponent(latitude, longitude, visible, label);
    }

    @Override
    public void showZone(double latitude, double longitude, double radiusMeters) {
        view.showZone(latitude, longitude, radiusMeters);
    }

    @Override
    public void hideZone() {
        view.hideZone();
    }

    @Override
    public void playZoneRipple() {
        view.ripple();
    }

    @Override
    public void addTrailPoint(double latitude, double longitude) {
        view.addTrailPoint(latitude, longitude);
    }

    @Override
    public void onResume() {
        view.invalidate();
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
    }
}