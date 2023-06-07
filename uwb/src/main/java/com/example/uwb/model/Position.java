package com.example.uwb.model;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 03:35
 */
public class Position {
    private float distance;
    private float azimuth;
    private float elevation;

    public Position(float distance, float azimuth) {
        this.distance = distance;
        this.azimuth = azimuth;
    }

    public Position (float distance, float azimuth, float elevation) {
        this.distance = distance;
        this.azimuth = azimuth;
        this.elevation = elevation;
    }

    public float getDistance() {
        return distance;
    }

    public float getAzimuth() {
        return azimuth;
    }

    public float getElevation() {
        return elevation;
    }
}
