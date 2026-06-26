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
    private int[] visibleTriangleIndexes;
    private float[] triangleDepths;
    private float[] chunkDepths;

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

        getChunkDepths(models);
        sortChunks(models.size(), models);

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

    public void getChunkDepths(ArrayList<SolidModel> models){
        int len = models.size();
        if (chunkDepths == null ||
            chunkDepths.length < len)
            chunkDepths = new float[len];

        Vector3 forward = cam.getForward();
        forward.normalise();
        Vector3 pos = cam.getPosition();
        for (int i = 0; i < len; i++) {
            chunkDepths[i] = getChunkDepth(models.get(i), forward, pos);
        }

    }

    public float getChunkDepth(SolidModel model, Vector3 forward, Vector3 pos){
        Vector3 offset = model.offset;
        float x = offset.x - pos.x;
        float y = offset.y - pos.y;
        float z = offset.z - pos.z;

        return x * forward.x + y * forward.y + z * forward.z;
    }

    public void sortChunks(int count, ArrayList<SolidModel> models){
        for (int i = 1; i < count; i++) {
            float depth = chunkDepths[i];
            SolidModel model = models.get(i);

            int j = i - 1;
            while(j >= 0 && chunkDepths[j] < depth){
                chunkDepths[j + 1] = chunkDepths[j];
                models.set(j + 1, models.get(j));
                j--;
            }
            models.set(j + 1, model);
            chunkDepths[j + 1] = depth;
        }
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
        int count = backfaceCullTriangles(model.cachedTris, model.cachedVerts);
        sortTriangles(count);

        //getting the triangles to render

        //drawing the triangles
        drawTriangles(model.cachedTris, vertices, model.colour, count);
    }

    private void postDirtyRenderWithDebug(SolidModel model){
        Vector3[] vertices = model.cachedVerts;

        long start = System.nanoTime();
        //sorting the triangles
        int count = backfaceCullTriangles(model.cachedTris, model.cachedVerts);
        long end = System.nanoTime();
        cullTime += (end - start) / 1_000_000f;

        long start3 = System.nanoTime();
        sortTriangles(count);

        long end3 = System.nanoTime();
        sortTime += (end3 - start3) / 1_000_000f;
        //System.out.println("sort triangles: " + (end3 - start3) / 1_000_000f + "ms");

        //drawing the triangles
        long start4 = System.nanoTime();

        drawTriangles(model.cachedTris, vertices, model.colour, count);

        long end4 = System.nanoTime();
        drawTime += (end4 - start4) / 1_000_000f;
        //System.out.println("draw triangles: " + (end4 - start4) / 1_000_000f + "ms");
    }

    private int backfaceCullTriangles(int[][] triangles, Vector3[] vertices){
        if (visibleTriangleIndexes == null ||
            visibleTriangleIndexes.length < triangles.length){
            visibleTriangleIndexes = new int[triangles.length];
            triangleDepths = new float[triangles.length];
        }


        Vector3 camPos = cam.getPosition();
        Vector3 forward = cam.getForward();
        forward.normalise();

        int count = 0;
        for (int i = 0; i < triangles.length; i++) {
            Vector3 a = vertices[triangles[i][0]];
            Vector3 b = vertices[triangles[i][1]];
            Vector3 c = vertices[triangles[i][2]];

            if (isLookingAt(a, b, c)) {
                visibleTriangleIndexes[count] = i;
                triangleDepths[count++] = getAverageZ(triangles[i], vertices, camPos, forward);
            }
        }
        return count;
    }

    private float getAverageZ(int[] triangle, Vector3[] vertices, Vector3 camPos, Vector3 forward){
        Vector3 a = vertices[triangle[0]];
        Vector3 b = vertices[triangle[1]];
        Vector3 c = vertices[triangle[2]];

        float x = (a.x + b.x + c.x) / 3f - camPos.x;
        float y = (a.y + b.y + c.y) / 3f - camPos.y;
        float z = (a.z + b.z + c.z) / 3f - camPos.z;

        return x * forward.x + y * forward.y + z * forward.z;
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

    private void drawTriangles(int[][] triangles, Vector3[] vertices, Color colour, int count){
        //not a bottleneck
        getProjectedPoints(vertices);

        int centreX = Config.SCREEN_WIDTH / 2;
        int centreY = Config.SCREEN_HEIGHT / 2;

        int lastIndex = -1;
        for (int j = 0; j < count; j++) {
            //not a bottleneck
            int[] triangle = triangles[visibleTriangleIndexes[j]];

            int i0 = triangle[0];
            int i1 = triangle[1];
            int i2 = triangle[2];

            if (!projectedToRender[i0] ||
                !projectedToRender[i1] ||
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

    public boolean isLookingAt(Vector3 a, Vector3 b, Vector3 c){
        //centre
        float cx = (a.x + b.x + c.x) / 3f;
        float cy = (a.y + b.y + c.y) / 3f;
        float cz = (a.z + b.z + c.z) / 3f;

        //vector ab = b - a
        float abx = b.x - a.x;
        float aby = b.y - a.y;
        float abz = b.z - a.z;

        //vector ac = c - a
        float acx = c.x - a.x;
        float acy = c.y - a.y;
        float acz = c.z - a.z;

        //normal
        float nx = aby * acz - abz * acy;
        float ny = abz * acx - abx * acz;
        float nz = abx * acy - aby * acx;

        //toCam
        float camX = cam.getPosition().x - cx;
        float camY = cam.getPosition().y - cy;
        float camZ = cam.getPosition().z - cz;

        //toCam dot normal
        return camX * nx + camY * ny + camZ * nz > 0f;
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

    //insertion sort
    private void sortTriangles(int count){
        for (int i = 1; i < count; i++) {
            int index = visibleTriangleIndexes[i];
            float depth = triangleDepths[i];

            int j = i - 1;
            while(j >= 0 && triangleDepths[j] < depth){
                visibleTriangleIndexes[j + 1] = visibleTriangleIndexes[j];
                triangleDepths[j + 1] = triangleDepths[j];
                j--;
            }

            visibleTriangleIndexes[j + 1] = index;
            triangleDepths[j + 1] = depth;
        }
    }


    public void clearScreen(){
        gc.clearRect(0, 0, Config.SCREEN_WIDTH, Config.SCREEN_HEIGHT);
    }

    public void setGraphicsContext(GraphicsContext gc){
        this.gc = gc;
    }
}
