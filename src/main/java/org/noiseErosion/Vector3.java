package org.noiseErosion;

//LEFT/RIGHT, UP/DOWN, FORWARDS/BACKWARDS
public class Vector3 {
    public float x, y, z;
    public Vector3(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    //operator functions
    public void add(Vector3 v){
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
    }

    public void multiply(float c){
        this.x *= c;
        this.y *= c;
        this.z *= c;
    }

    public static float dot(Vector3 v1, Vector3 v2){
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    @Override
    public String toString(){
        return String.format("%.2f, %.2f, %.2f", x, y, z);
    }
}
