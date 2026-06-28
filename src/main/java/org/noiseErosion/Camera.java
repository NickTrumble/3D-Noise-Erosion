package org.noiseErosion;

public class Camera {
    private static final float FOCAL_LENGTH = 500f;

    private Vector3 position;
    private Vector3 lookat;
    private Vector3 up;

    private float angle = 0f;

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

    public boolean project(Vector3 v, Vector3 projected){
        worldToCamera(v, projected);

        if (projected.z <= 0)
            return false;

        projected.x = (projected.x * FOCAL_LENGTH) / projected.z;
        projected.y = (projected.y * FOCAL_LENGTH) / projected.z;
        return true;
    }

    public void worldToCamera(Vector3 v, Vector3 cameraSpace){
        float x = v.x - position.x;
        float y = v.y - position.y;
        float z = v.z - position.z;

        Vector3 forward = getForwardDirection();
        Vector3 right = getRightDirection();
        Vector3 trueUp = getTrueUpDirection();

        float cx = x * right.x   + y * right.y   + z * right.z;
        float cy = x * trueUp.x + y * trueUp.y + z * trueUp.z;
        float cz = x * forward.x + y * forward.y + z * forward.z;

        cameraSpace.x = cx;
        cameraSpace.y = cy;
        cameraSpace.z = cz;
    }

    public float getDepth(Vector3 point){
        Vector3 forward = getForwardDirection();
        float x = point.x - position.x;
        float y = point.y - position.y;
        float z = point.z - position.z;

        return x * forward.x + y * forward.y + z * forward.z;
    }

    public float getTriangleDepth(int[] triangle, Vector3[] vertices){
        Vector3 a = vertices[triangle[0]];
        Vector3 b = vertices[triangle[1]];
        Vector3 c = vertices[triangle[2]];

        return getDepth(getCentre(a, b, c));
    }

    public boolean isTriangleFacingCamera(Vector3 a, Vector3 b, Vector3 c){
        Vector3 centre = getCentre(a, b, c);

        float abx = b.x - a.x;
        float aby = b.y - a.y;
        float abz = b.z - a.z;

        float acx = c.x - a.x;
        float acy = c.y - a.y;
        float acz = c.z - a.z;

        float nx = aby * acz - abz * acy;
        float ny = abz * acx - abx * acz;
        float nz = abx * acy - aby * acx;

        float camX = position.x - centre.x;
        float camY = position.y - centre.y;
        float camZ = position.z - centre.z;

        return camX * nx + camY * ny + camZ * nz > 0f;
    }

    public Vector3 getCentre(Vector3 a, Vector3 b, Vector3 c){
        return new Vector3(
                (a.x + b.x + c.x) / 3f,
                (a.y + b.y + c.y) / 3f,
                (a.z + b.z + c.z) / 3f
        );
    }

    public void rotateCam(float angleIncrement, float orbitRadius){
        angle += angleIncrement;
        Vector3 lookat = getLookat();

        float camX = lookat.x + (float) Math.cos(angle) * orbitRadius;
        float camZ = lookat.z + (float) Math.sin(angle) * orbitRadius;
        float camY = this.position.y;

        setPosition(new Vector3(camX, camY, camZ));
    }

    public void moveCamera(Vector3 change){
        lookat.add(change);
        position.add(change);
    }

    public void resetCamera(){
        lookat = new Vector3(Config.DEFAULT_LOOKAT);
        position = new Vector3(Config.DEFAULT_POSITION);
        angle = (float) (- Math.PI / 2f);
    }

    //getters and setters
    public void setPosition(Vector3 position) { this.position = position; }
    public void setLookat(Vector3 lookat) { this.lookat = lookat; }

    public Vector3 getPosition() { return position; }
    public Vector3 getLookat() { return lookat; }
    public Vector3 getForward() {
        return new Vector3(
                lookat.x - position.x,
                lookat.y - position.y,
                lookat.z - position.z
        );
    }
    public Vector3 getForwardDirection(){
        Vector3 forward = getForward();
        forward.normalise();
        return forward;
    }
    public Vector3 getRight(){
        Vector3 forward = getForward();
        return up.cross(forward);
    }
    public Vector3 getRightDirection(){
        Vector3 right = getRight();
        right.normalise();
        return right;
    }
    public Vector3 getTrueUpDirection(){
        return getRightDirection().cross(getForwardDirection());
    }

//override methods
    @Override
    public String toString(){
        return String.format("Camera: \nPosition: %s, \nLookat: %s, \nUp: %s",
                position.toString(), lookat.toString(), up.toString());
    }
}
