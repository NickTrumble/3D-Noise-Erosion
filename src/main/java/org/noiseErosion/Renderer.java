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

    private Color[] cachedColours;

    private double[] triangleX;
    private double[] triangleY;


    private final boolean debug = false;
    private float drawTime = 0f;
    private float sortTime = 0f;
    private float cullTime = 0f;
    private float renderTime = 0f;
    private float colourTime = 0f;
    private float setFillTime = 0f;
    private float indexTime = 0f;

    private Vector3 modelOffset;
    private float modelVoxelsize;
    private int modelChunks;
    private int modelWidth;

    public Renderer(Camera cam){
        this.cam = cam;
    }
    //solid model

    public boolean renderSolids(ArrayList<SolidModel> models){
        if (models == null)
            return false;

        SolidModel model1 = models.getFirst();
        modelOffset = model1.offset;
        modelVoxelsize = model1.voxelSize;
        modelWidth = model1.width;
        if (modelChunks == 0)
            modelChunks = 1;


        sortTime = 0f;
        drawTime = 0f;
        cullTime = 0f;
        renderTime = 0f;
        colourTime = 0f;
        setFillTime = 0f;
        indexTime = 0f;
        for (SolidModel model : models){
            renderSolidModel(model);
        }
        if (debug){
            System.out.println("Sort time: " + sortTime + "ms");
            System.out.println("Cull time: " + cullTime + "ms");
            System.out.println("Draw time: " + drawTime + "ms");
            System.out.println("Render time: " + renderTime + "ms, %" + 100 * renderTime / drawTime);
            System.out.println("Colour time: " + colourTime + "ms, %" + 100 * colourTime / drawTime);
            System.out.println("index time: " + indexTime + "ms, %" + 100 * indexTime / colourTime);
            System.out.println("set fill time: " + setFillTime + "ms, %" + 100 * setFillTime / colourTime);

        }
        return true;
    }

    public void renderSolidModel(SolidModel model){
        if (model.dirty){

            int width = model.units;
            Vector3 offset = model.offset;
            ArrayList<Vector3> verts = new ArrayList<>();
            ArrayList<int[]> tris = new ArrayList<>();

            float half = model.units / 2f;

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

            model.dirty = false;
        }

        if (debug)
            postDirtyRenderWithDebug(model);
        else
            postDirtyRender(model);
    }

    private void postDirtyRender(SolidModel model){
        Vector3[] vertices = model.cachedVerts;

        //sorting the triangles
        int[][] triangles = backfaceCullTriangles(model.cachedTris, model.cachedVerts);
        triangles = sortTriangles(triangles, model.cachedVerts);

        //getting the triangles to render

        //drawing the triangles
        drawTriangles(triangles, vertices, model.colour);
    }

    private void postDirtyRenderWithDebug(SolidModel model){
        Vector3[] vertices = model.cachedVerts;



        long start = System.nanoTime();
        //sorting the triangles
        int[][] triangles = backfaceCullTriangles(model.cachedTris, model.cachedVerts);
        long end = System.nanoTime();
        cullTime += (end - start) / 1_000_000f;

        long start3 = System.nanoTime();
        triangles = sortTriangles(triangles, model.cachedVerts);

        long end3 = System.nanoTime();
        sortTime += (end3 - start3) / 1_000_000f;
        //System.out.println("sort triangles: " + (end3 - start3) / 1_000_000f + "ms");

        //drawing the triangles
        long start4 = System.nanoTime();

        drawTriangles(triangles, vertices, model.colour);

        long end4 = System.nanoTime();
        drawTime += (end4 - start4) / 1_000_000f;
        //System.out.println("draw triangles: " + (end4 - start4) / 1_000_000f + "ms");
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

        SolidModel model1 = models.getFirst();
        modelOffset = model1.offset;
        modelVoxelsize = model1.voxelSize;
        modelWidth = model1.width;
        modelChunks = world.getWorldHeight();
        maxHeight = world.getWorldHeight() * world.getChunkWidth() * models.getFirst().voxelSize;
        buildColourCache();
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

    private void buildColourCache(){
        int length = (int) (modelChunks * modelWidth / modelVoxelsize);
        if (cachedColours != null &&
        length == cachedColours.length)
            return;

        System.out.println("building colour cache");
        cachedColours = new Color[length];
        for (int i = 0; i < length; i++) {
            cachedColours[i] = ColourMap.getColour(
                i,
                length,
                ColourMap.rainbowCMAP
            );
        }
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

    private void drawTriangles(int[][] triangles, Vector3[] vertices, Color colour){
        //not a bottleneck
        getProjectedPoints(vertices);

        int centreX = Config.SCREEN_WIDTH / 2;
        int centreY = Config.SCREEN_HEIGHT / 2;

        int lastIndex = -1;
        for (int j = 0; j < triangles.length; j++) {
            //not a bottleneck
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
//end

            //gc.setFill(ColourMap.getColour(vertices[i0].y, maxHeight, ColourMap.rainbowCMAP));
            int index = (int)((vertices[i0].y - modelOffset.y + 2) / modelVoxelsize);
            Color c = cachedColours[index];


            if (lastIndex != index) {
                gc.setFill(c);
                lastIndex = index;
            }

            gc.fillPolygon(triangleX, triangleY, 3);
        }
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
            float d1 = vertices[t1[0]].z;
            float d2 = vertices[t2[0]].z;
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
