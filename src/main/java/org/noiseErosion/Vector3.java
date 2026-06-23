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

    public void subtract(Vector3 v){
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
    }

    public static Vector3 subtract(Vector3 v1, Vector3 v2){
        return new Vector3(
                v1.x - v2.x,
                v1.y - v2.y,
                v1.z - v2.z
        );
    }

    public static float dot(Vector3 v1, Vector3 v2){
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    public void normalise(){
        float invSqrt = (float) (1f / Math.sqrt(x*x + y*y + z*z));
        multiply(invSqrt);
    }

    public Vector3 cross(Vector3 v){
        return new Vector3(
                y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x
        );
    }
    @Override
    public String toString(){
        return String.format("%.2f, %.2f, %.2f", x, y, z);
    }
}
