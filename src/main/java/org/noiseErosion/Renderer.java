package org.noiseErosion;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.noiseErosion.lib.World;

import java.util.ArrayList;
import java.util.Arrays;

public class Renderer {
    public float maxHeight;
    public Camera cam;
    private GraphicsContext gc;

    private float[] projectedX;
    private float[] projectedY;
    private float[] projectedZ;
    private boolean[] projectedToRender;

    private double[] triangleX;
    private double[] triangleY;


    private final boolean debug = true;

    public Renderer(Camera cam){
        this.cam = cam;
    }

    //hollow models
    public boolean renderHollows(ArrayList<Model> models){
        if (models == null)
            return false;
        for (Model model : models){
            renderHollowModel(model);
        }
        return true;
    }

    public void renderHollowModel(Model model){
        int[][] triangles = model.getTriangles();
        Vector3[] vertices = model.getVertices();
        boolean[] trianglesToRender = getTrianglesToRender(triangles, vertices);

        drawTriangles(triangles, trianglesToRender, vertices, null);
    }

    //solid model

    public boolean renderSolids(ArrayList<SolidModel> models){
        if (models == null)
            return false;
        for (SolidModel model : models){
            renderSolidModel(model);
        }
        return true;
    }

    public void renderSolidModel(SolidModel model){

        long start = System.nanoTime();

        if (model.dirty){

            int width = model.units;
            Vector3 offset = model.offset;
            ArrayList<Vector3> verts = new ArrayList<>();
            ArrayList<int[]> tris = new ArrayList<>();

            float half = model.units / 2f;
            half = 0f;

            for(int i = 0; i < width; i++){
                for (int j = 0; j < width; j++) {
                    for (int k = 0; k < width; k++) {

                        if(!model.isSolid(i, j, k)) continue;

                        float wx = offset.x + (i - half) * model.voxelSize;
                        float wy = offset.y + (j - half) * model.voxelSize;
                        float wz = offset.z + (k - half) * model.voxelSize;

                        if (!model.isSolid(i, j, k - 1))
                            addFace(verts, tris,  // -k face — reversed
                                    new Vector3(wx,   wy,   wz),
                                    new Vector3(wx,   wy+model.voxelSize, wz),
                                    new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz),
                                    new Vector3(wx+model.voxelSize, wy,   wz)
                            );

                        if (!model.isSolid(i, j, k + 1))
                            addFace(verts, tris, // +k face
                                    new Vector3(wx,   wy,   wz+model.voxelSize),
                                    new Vector3(wx+model.voxelSize, wy,   wz+model.voxelSize),
                                    new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz+model.voxelSize),
                                    new Vector3(wx,   wy+model.voxelSize, wz+model.voxelSize)
                            );

                        if (!model.isSolid(i - 1, j, k))
                            addFace(verts, tris, // -i face
                                    new Vector3(wx, wy,   wz),
                                    new Vector3(wx, wy,   wz+model.voxelSize),
                                    new Vector3(wx, wy+model.voxelSize, wz+model.voxelSize),
                                    new Vector3(wx, wy+model.voxelSize, wz)
                            );

                        if (!model.isSolid(i + 1, j, k))
                            addFace(verts, tris, // +i face
                                    new Vector3(wx+model.voxelSize, wy,   wz),
                                    new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz),
                                    new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz+model.voxelSize),
                                    new Vector3(wx+model.voxelSize, wy,   wz+model.voxelSize)
                            );

                        if (!model.isSolid(i, j - 1, k))
                            addFace(verts, tris, // -j face
                                    new Vector3(wx,   wy, wz),
                                    new Vector3(wx+model.voxelSize, wy, wz),
                                    new Vector3(wx+model.voxelSize, wy, wz+model.voxelSize),
                                    new Vector3(wx,   wy, wz+model.voxelSize)
                            );

                        if (!model.isSolid(i, j + 1, k))
                            addFace(verts, tris, // +j face
                                    new Vector3(wx,   wy+model.voxelSize, wz),
                                    new Vector3(wx,   wy+model.voxelSize, wz+model.voxelSize),
                                    new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz+model.voxelSize),
                                    new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz)
                            );
                    }
                }
            }
            model.cachedVerts = verts.toArray(new Vector3[0]);
            tris.sort((t1, t2) -> {
                float d1 = getAverageZ(t1, model.cachedVerts);
                float d2 = getAverageZ(t2, model.cachedVerts);
                return Float.compare(d2, d1);
            });
            model.cachedTris = tris.toArray(new int[0][]);

            model.cachedTrisToRender = getTrianglesToRender(model.cachedTris, model.cachedVerts);

            model.dirty = false;
        }

        if (debug)
            postDirtyRenderWithDebug(model);
        else
            postDirtyRender(model);

        long end = System.nanoTime();
        if (debug)
            System.out.println("render solid model function: " + (end - start) / 1_000_000f + "ms");
    }

    private void postDirtyRender(SolidModel model){
        Vector3[] vertices = model.cachedVerts;

        //sorting the triangles
        int[][] triangles = backfaceCullTriangles(model.cachedTris, model.cachedVerts);
        triangles = sortTriangles(triangles, model.cachedVerts);

        //getting the triangles to render
        boolean[] trianglesToRender = getTrianglesToRender(triangles, vertices);

        //drawing the triangles
        drawTriangles(triangles, trianglesToRender, vertices, model.colour);
    }

    private void postDirtyRenderWithDebug(SolidModel model){
        Vector3[] vertices = model.cachedVerts;

        long start3 = System.nanoTime();

        //sorting the triangles
        int[][] triangles = backfaceCullTriangles(model.cachedTris, model.cachedVerts);
        triangles = sortTriangles(triangles, model.cachedVerts);

        long end3 = System.nanoTime();
        System.out.println("sort triangles: " + (end3 - start3) / 1_000_000f + "ms");


        //getting the triangles to render
        long start2 = System.nanoTime();

        boolean[] trianglesToRender = getTrianglesToRender(triangles, vertices);

        long end2 = System.nanoTime();
        System.out.println("triangles to render: " + (end2 - start2) / 1_000_000f + "ms");


        //drawing the triangles
        long start4 = System.nanoTime();

        drawTriangles(triangles, trianglesToRender, vertices, model.colour);

        long end4 = System.nanoTime();
        System.out.println("draw triangles: " + (end4 - start4) / 1_000_000f + "ms");
    }

    private int[][] backfaceCullTriangles(int[][] triangles, Vector3[] vertices){
        ArrayList<int[]> visible = new ArrayList<>();
        for (int[] tri : triangles){
            Vector3 a = vertices[tri[0]];
            Vector3 b = vertices[tri[1]];
            Vector3 c = vertices[tri[2]];

            if (isLookingAt(a, b, c) > 0)
                visible.add(tri);
        }
        return visible.toArray(new int[0][]);
    }

    private void addFace(ArrayList<Vector3> verts, ArrayList<int[]> tris, Vector3 a, Vector3 b, Vector3 c, Vector3 d){
        int index = verts.size();
        verts.add(a);
        verts.add(b);
        verts.add(c);
        verts.add(d);

        tris.add(new int[] { index, index + 1, index + 2 });
        tris.add(new int[] { index, index + 2, index + 3 });
    }

    //world

    public boolean renderWorld(World world){
        if (world == null)
            return false;

        ArrayList<SolidModel> models = new ArrayList<>();

        for (SolidModel[][] plane : world.chunks) {
            for (SolidModel[] row : plane) {
                for (SolidModel model : row) {
                    if (model != null) {
                        models.add(model);
                    }
                }
            }
        }

        maxHeight = world.getWorldHeight() * world.getChunkWidth() * models.getFirst().voxelSize;
        renderSolids(models);
        return true;
    }

    //needed for both versions
    private boolean[] getVerticesToRender(Vector3[] vertices){
        boolean[] verticesToRender = new boolean[vertices.length];

        for (int i = 0; i < vertices.length; i++){
            verticesToRender[i] = cam.isVertexInFrame(vertices[i]);
        }
        return verticesToRender;
    }

    private void getProjectedPoints(Vector3[] vertices){
        int length = vertices.length;
        if (projectedX == null || projectedX.length < length) {
            projectedX = new float[length];
            projectedY = new float[length];
            projectedZ = new float[length];
            projectedToRender = new boolean[length];

            triangleX = new double[3];
            triangleY = new double[3];
        }

        Vector3 position = cam.getPosition();
        Vector3 forward = cam.getForward();
        forward.normalise();
        Vector3 right = cam.getRight();
        right.normalise();
        Vector3 trueUp = right.cross(forward);

        float focalLength = 500f;

        for (int i = 0; i < length; i++) {
            Vector3 v = vertices[i];

            float x = v.x - position.x;
            float y = v.y - position.y;
            float z = v.z - position.z;


            float cameraX = x * right.x + y * right.y + z * right.z;
            float cameraY = x * trueUp.x + y * trueUp.y + z * trueUp.z;
            float cameraZ = x * forward.x + y * forward.y + z * forward.z;

            if (cameraZ < 0){
                projectedToRender[i] = false;
                continue;
            }

            projectedX[i] = cameraX * focalLength / cameraZ;
            projectedY[i] = cameraY * focalLength / cameraZ;
            projectedZ[i] = cameraZ;

            projectedToRender[i] = true;
        }
    }

    private boolean[] getTrianglesToRender(int[][] triangles, Vector3[] vertices){
        boolean[] verticesToRender = getVerticesToRender(vertices);
        boolean[] trianglesToRender = new boolean[triangles.length];
        for (int i = 0; i < triangles.length; i++){
            trianglesToRender[i] = cam.isTriangleInFrame(i, triangles, verticesToRender);
        }
        return trianglesToRender;
    }

    private void drawTriangles(int[][] triangles, boolean[] trianglesToRender, Vector3[] vertices, Color colour){
//        for (int i = 0; i < triangles.length; i++){
//            if (!trianglesToRender[i])
//                continue;
//
//            drawTriangle(triangles[i], vertices, colour);

            getProjectedPoints(vertices);
            int centreX = Config.SCREEN_WIDTH / 2;
            int centreY = Config.SCREEN_HEIGHT / 2;

            for (int j = 0; j < triangles.length; j++) {

                int[] triangle = triangles[j];

                int i0 = triangle[0];
                int i1 = triangle[1];
                int i2 = triangle[2];

                if (!projectedToRender[i0] &&
                    !projectedToRender[i1] &&
                    !projectedToRender[i2]){
                    continue;
                }

                triangleX[0] = projectedX[i0] + centreX;
                triangleX[1] = projectedX[i1] + centreX;
                triangleX[2] = projectedX[i2] + centreX;

                triangleY[0] = projectedY[i0] + centreY;
                triangleY[1] = projectedY[i1] + centreY;
                triangleY[2] = projectedY[i2] + centreY;


                gc.setFill(ColourMap.getColour(vertices[i0].y, maxHeight, ColourMap.rainbowCMAP));

                gc.fillPolygon(triangleX, triangleY, 3);

            }
        //}
    }

    public void drawTriangle(int[] triangle, Vector3[] vertices, Color colour){
        if (gc == null)
            return;

        int centreX = Config.SCREEN_WIDTH / 2;
        int centreY = Config.SCREEN_HEIGHT / 2;

        Vector3 v1 = vertices[triangle[0]];
        Vector3 v2 = vertices[triangle[1]];
        Vector3 v3 = vertices[triangle[2]];

        //get screen coordinates
        Vector3 a = cam.project(v1);
        Vector3 b = cam.project(v2);
        Vector3 c = cam.project(v3);

        if (a == null || b == null || c == null)
            return;

        float facingVal = isLookingAt(v1, v2, v3);

        float maxColour = 0.6f;
        float offset = 0.2f;

        if (facingVal <= 0){
            return;
        } else {
            float intensity = offset + facingVal * maxColour;
            gc.setFill(Color.color(intensity * colour.getRed(), intensity * colour.getGreen(), intensity * colour.getBlue()));
            gc.setFill(ColourMap.getColour(v1.y, maxHeight, ColourMap.rainbowCMAP));
        }

        gc.fillPolygon(
                new double[]{
                        a.x + centreX,
                        b.x + centreX,
                        c.x + centreX
                },
                new double[]{
                        a.y + centreY,
                        b.y + centreY,
                        c.y + centreY
                },
                3);

    }

    public Vector3 getNormal(Vector3 a, Vector3 b, Vector3 c){
        Vector3 ab = Vector3.subtract(b, a);
        Vector3 ac = Vector3.subtract(c, a);

        Vector3 normal = ab.cross(ac);
        normal.normalise();
        return normal;
    }

    public float isLookingAt(Vector3 a, Vector3 b, Vector3 c){
        Vector3 centre = getCentre(a, b, c);

        Vector3 normal = getNormal(a, b, c);

        Vector3 toCam = Vector3.subtract(cam.getPosition(), centre);
        toCam.normalise();

        return Vector3.dot(toCam, normal);
    }

    public float getAverageZ(int[] tris, Vector3[] vertices){
        Vector3 a = vertices[tris[0]];
        Vector3 b = vertices[tris[1]];
        Vector3 c = vertices[tris[2]];

        float cx = (a.x + b.x + c.x) / 3f - cam.getPosition().x;
        float cy = (a.y + b.y + c.y) / 3f - cam.getPosition().y;
        float cz = (a.z + b.z + c.z) / 3f - cam.getPosition().z;

        Vector3 forward = cam.getForward();
        forward.normalise();

        return cx * forward.x + cy * forward.y + cz * forward.z;
    }

    public Vector3 getCentre(Vector3 a, Vector3 b, Vector3 c){
        return new Vector3(
                (a.x + b.x + c.x) / 3f,
                (a.y + b.y + c.y) / 3f,
                (a.z + b.z + c.z) / 3f
        );
    }

    private int[][] sortTriangles(int[][] triangles, Vector3[] vertices) {
        Vector3 forward = cam.getForward();
        forward.normalise();

        ArrayList<int[]> tris = new ArrayList<>(Arrays.asList(triangles));
        tris.sort((t1, t2) -> {
            float d1 = getAverageZ(t1, vertices);
            float d2 = getAverageZ(t2, vertices);
            return Float.compare(d2, d1);
        });
        return tris.toArray(new int[0][]);
    }

    public void clearScreen(){
        gc.clearRect(0, 0, Config.SCREEN_WIDTH, Config.SCREEN_HEIGHT);
    }

    public void setGraphicsContext(GraphicsContext gc){
        this.gc = gc;
    }
}
