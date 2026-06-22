package org.noiseErosion;

public class Camera {
    private Vector3 position;
    private Vector3 lookat;
    private Vector3 up;

    public Camera(){
        position = new Vector3(0, 0, -5); //5 back
        lookat = new Vector3(0, 0, 0);//origin
        up = new Vector3(0, 1, 0);//directly up
    }

    public Camera(Vector3 position, Vector3 lookat, Vector3 up){
        this.position = position;
        this.lookat = lookat;
        this.up = up;
    }


    public boolean isTriangleInFrame(int index, int[][] triangles, boolean[] verticesToRender){
        int[] indexes = triangles[index];
        boolean result = false;
        //return true if any vertex is visible
        for (int vertex : indexes){
            if (verticesToRender[vertex]){
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean isVertexInFrame(Vector3 vertex){

        Vector3 projected = project(vertex);

        if (projected == null)
            return false;

        int screenWidth = 800;
        int screenHeight = 600;

        return (projected.x >= ((float) -screenWidth / 2)) &&
                (projected.x <= ((float) screenWidth / 2)) &&
                (projected.y >= ((float) -screenHeight / 2)) &&
                (projected.y <= ((float) screenHeight / 2));
    }

    public Vector3 project(Vector3 v){
        float x = v.x - position.x;
        float y = v.y - position.y;
        float z = v.z - position.z;

        if (z <= 0)
            return null;

        float fov = 500;

        float screenX = (x * fov) / z;
        float screenY = (y * fov) / z;

        return new Vector3(screenX, screenY, z);
    }

//getters and setters
    public void setPosition(Vector3 position) { this.position = position; }
    public void setLookat(Vector3 lookat) { this.lookat = lookat; }
    public void setUp(Vector3 up) { this.up = up;}

    public Vector3 getPosition() { return position; }
    public Vector3 getLookat() { return lookat; }
    public Vector3 getUp() { return up; }

//override methods
    @Override
    public String toString(){
        return String.format("Camera: \nPosition: %s, \nLookat: %s, \nUp: %s",
                position.toString(), lookat.toString(), up.toString());
    }
}
