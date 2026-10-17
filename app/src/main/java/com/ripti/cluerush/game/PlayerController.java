package com.ripti.cluerush.game;

import com.ripti.cluerush.model.GeoBounds;
import com.ripti.cluerush.util.GeoMath;

public class PlayerController {

    public static final double DEFAULT_SPEED_MPS = 45.0;

    private static final float RESPONSE_RATE = 8f;
    private static final float STOP_THRESHOLD = 0.02f;
    private static final float FACING_THRESHOLD = 0.08f;

    private final GeoBounds bounds;
    private double latitude;
    private double longitude;
    private double speedMetersPerSecond = DEFAULT_SPEED_MPS;
    private float inputX;
    private float inputY;
    private float velocityX;
    private float velocityY;
    private boolean facingRight = true;
    private boolean moving;

    public PlayerController(double latitude, double longitude, GeoBounds bounds) {
        this.bounds = bounds;
        this.latitude = bounds.clampLatitude(latitude);
        this.longitude = bounds.clampLongitude(longitude);
    }

    public void setInput(float x, float y) {
        inputX = x;
        inputY = y;
    }

    public void setSpeedMetersPerSecond(double speedMetersPerSecond) {
        this.speedMetersPerSecond = speedMetersPerSecond;
    }

    public boolean update(float deltaSeconds) {
        float blend = Math.min(1f, deltaSeconds * RESPONSE_RATE);
        velocityX += (inputX - velocityX) * blend;
        velocityY += (inputY - velocityY) * blend;

        float magnitude = (float) Math.hypot(velocityX, velocityY);
        boolean noInput = inputX == 0f && inputY == 0f;
        if (magnitude < STOP_THRESHOLD && noInput) {
            velocityX = 0f;
            velocityY = 0f;
            moving = false;
            return false;
        }

        if (Math.abs(velocityX) > FACING_THRESHOLD) {
            facingRight = velocityX > 0f;
        }
        if (deltaSeconds <= 0f) {
            return false;
        }

        double metersEast = velocityX * speedMetersPerSecond * deltaSeconds;
        double metersNorth = velocityY * speedMetersPerSecond * deltaSeconds;
        double newLatitude = bounds.clampLatitude(
                latitude + GeoMath.metersToLatitudeDegrees(metersNorth));
        double newLongitude = bounds.clampLongitude(
                longitude + GeoMath.metersToLongitudeDegrees(metersEast, latitude));

        boolean changed = newLatitude != latitude || newLongitude != longitude;
        latitude = newLatitude;
        longitude = newLongitude;
        moving = changed && magnitude >= STOP_THRESHOLD;
        return changed;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public float getSpeedFraction() {
        return Math.min(1f, (float) Math.hypot(velocityX, velocityY));
    }
}