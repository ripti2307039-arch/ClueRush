package com.ripti.cluerush.map;

import com.ripti.cluerush.model.GeoBounds;

import java.util.List;

public interface OverviewMapView {

    void setBounds(GeoBounds bounds);

    void fit();

    void setMarkers(List<MapMarker> markers);

    void setPlayer(double latitude, double longitude);

    void setRevealAll(boolean reveal);

    void setPickEnabled(boolean enabled);

    void setPickListener(OverviewMap.OnPickListener listener);

    void setPicked(double latitude, double longitude);

    void clearPicked();

    void onResume();

    void onPause();

    void onDestroy();
}