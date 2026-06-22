package org.noiseErosion;

import java.awt.*;

public class Triangle {
    public Vector3[] vertices;
    public Vector3 normal;//add in later
    public Color colour;

    public Triangle(Vector3[] vertices){
        this.vertices = vertices;
    }
}
