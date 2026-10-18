package com.ripti.cluerush.map;

import com.ripti.cluerush.model.GeoBounds;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;

import java.util.List;

public class OverviewMap {

    public interface OnPickListener {
        void onPick(double latitude, double longitude);
    }

    private static final double MIN_ZOOM = 14.0;
    private static final double MAX_ZOOM = 19.0;
    private static final double SCROLL_PADDING_DEGREES = 0.004;
    private static final float FIT_BORDER_DP = 24f;

    private final MapView mapView;
    private final OverviewOverlay overlay;
    private BoundingBox fitBox;
    private OnPickListener pickListener;
    private boolean pickEnabled;

    public OverviewMap(MapView mapView) {
        this.mapView = mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.setHorizontalMapRepetitionEnabled(false);
        mapView.setVerticalMapRepetitionEnabled(false);
        mapView.setMinZoomLevel(MIN_ZOOM);
        mapView.setMaxZoomLevel(MAX_ZOOM);

        overlay = new OverviewOverlay(mapView.getContext());
        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (!pickEnabled || pickListener == null) {
                    return false;
                }
                pickListener.onPick(p.getLatitude(), p.getLongitude());
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        mapView.getOverlays().add(new MapEventsOverlay(receiver));
        mapView.getOverlays().add(overlay);
    }

    public void setBounds(GeoBounds bounds) {
        fitBox = new BoundingBox(bounds.getNorth(), bounds.getEast(),
                bounds.getSouth(), bounds.getWest());
        GeoBounds padded = bounds.expandedBy(SCROLL_PADDING_DEGREES);
        mapView.setScrollableAreaLimitDouble(new BoundingBox(
                padded.getNorth(), padded.getEast(), padded.getSouth(), padded.getWest()));
    }


    public void fit() {
        if (fitBox == null) {
            return;
        }
        int border = (int) (FIT_BORDER_DP * mapView.getResources().getDisplayMetrics().density);
        mapView.post(() -> mapView.zoomToBoundingBox(fitBox, false, border));
    }

    public void setMarkers(List<MapMarker> markers) {
        overlay.setMarkers(markers);
        mapView.invalidate();
    }

    public void setPlayer(double latitude, double longitude) {
        overlay.setPlayer(latitude, longitude);
        mapView.invalidate();
    }

    public void setRevealAll(boolean reveal) {
        overlay.setRevealAll(reveal);
        mapView.invalidate();
    }

    public void setPickEnabled(boolean enabled) {
        pickEnabled = enabled;
    }

    public void setPickListener(OnPickListener listener) {
        pickListener = listener;
    }

    public void setPicked(double latitude, double longitude) {
        overlay.setPicked(new GeoPoint(latitude, longitude));
        mapView.invalidate();
    }

    public void clearPicked() {
        overlay.setPicked(null);
        mapView.invalidate();
    }

    public void onResume() {
        mapView.onResume();
    }

    public void onPause() {
        mapView.onPause();
    }

    public void onDestroy() {
        mapView.onDetach();
    }
}