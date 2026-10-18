package com.ripti.cluerush.map;

import com.ripti.cluerush.model.GeoBounds;

import java.util.List;

public interface GameMap {

    void setBounds(GeoBounds movementBounds);

    void setZoom(double zoom);

    void moveCamera(double latitude, double longitude);

    void setNightStyle(boolean night);

    void setMarkers(List<MapMarker> markers);

    void setPlayerPosition(double latitude, double longitude);

    void setVisionRadius(double meters);

    void showZone(double latitude, double longitude, double radiusMeters);

    void hideZone();

    void playZoneRipple();

    void addTrailPoint(double latitude, double longitude);

    void onResume();

    void onPause();

    void onDestroy();
}