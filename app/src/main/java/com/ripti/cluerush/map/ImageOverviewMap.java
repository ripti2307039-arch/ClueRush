package com.ripti.cluerush.map;

import android.graphics.Bitmap;

import com.ripti.cluerush.model.GeoBounds;

import java.util.List;

public class ImageOverviewMap implements OverviewMapView {

    private final ImageMapView view;

    public ImageOverviewMap(ImageMapView view, Bitmap bitmap) {
        this.view = view;
        view.setMap(bitmap);
        view.setInteractive(true);
        view.setDrawPlayer(true);
        view.setFogEnabled(false);
    }

    @Override
    public void setBounds(GeoBounds bounds) {
    }

    @Override
    public void fit() {
        view.post(view::fitToView);
    }

    @Override
    public void setMarkers(List<MapMarker> markers) {
        view.setMarkerList(markers);
    }

    @Override
    public void setPlayer(double latitude, double longitude) {
        view.setPlayer(latitude, longitude);
    }

    @Override
    public void setRevealAll(boolean reveal) {
        view.setRevealAll(reveal);
    }

    @Override
    public void setPickEnabled(boolean enabled) {
        view.setPickEnabled(enabled);
    }

    @Override
    public void setPickListener(OverviewMap.OnPickListener listener) {
        view.setPickListener(listener);
    }

    @Override
    public void setPicked(double latitude, double longitude) {
        view.setPicked(latitude, longitude);
    }

    @Override
    public void clearPicked() {
        view.clearPicked();
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
    }
}