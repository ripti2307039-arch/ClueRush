package com.ripti.cluerush.model;

import androidx.annotation.ColorInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Arena {

    private final String id;
    private final String name;
    private final String subtitle;
    private final int colorStart;
    private final int colorEnd;
    private final double centerLatitude;
    private final double centerLongitude;
    private final double zoom;
    private final GeoBounds bounds;
    private final int sortOrder;
    private final List<CampusLocation> locations;

    public Arena(String id, String name, String subtitle,
                 @ColorInt int colorStart, @ColorInt int colorEnd,
                 double centerLatitude, double centerLongitude, double zoom,
                 GeoBounds bounds, int sortOrder, List<CampusLocation> locations) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.zoom = zoom;
        this.bounds = bounds;
        this.sortOrder = sortOrder;
        this.locations = Collections.unmodifiableList(new ArrayList<>(locations));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    @ColorInt
    public int getColorStart() {
        return colorStart;
    }

    @ColorInt
    public int getColorEnd() {
        return colorEnd;
    }

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public double getZoom() {
        return zoom;
    }

    public GeoBounds getBounds() {
        return bounds;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public List<CampusLocation> getLocations() {
        return locations;
    }
}