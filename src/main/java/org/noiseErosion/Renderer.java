package org.noiseErosion;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.noiseErosion.lib.World;

import java.util.ArrayList;

public class Renderer {
    public Camera cam;
    private GraphicsContext gc;

    private float[] projectedX;
    private float[] projectedY;
    private boolean[] projectedToRender;

    private Color[] cachedColours;
    private Color[] colourMap = ColourMap.viridisCMAP;
    private int[] visibleTriangleIndexes;
    private float[] triangleDepths;
    private float[] chunkDepths;

    private double[] triangleX;
    private double[] triangleY;
    private final Vector3 projectedPoint = new Vector3(0, 0, 0);

    private boolean debugEnabled = false;

    private Vector3 modelOffset;
    private float modelVoxelsize;
    private float colourMinY;
    private int modelChunks;
    private int modelWidth;



    public Renderer(Camera cam){
        this.cam = cam;
    }

    public void setDebugEnabled(boolean debugEnabled){
        this.debugEnabled = debugEnabled;
    }

    public void setColourMap(Color[] colourMap){
        this.colourMap = colourMap;
        cachedColours = null;
    }

    //solid model

    private void renderSolids(ArrayList<SolidModel> models){
        if (models == null)
            return;

        SolidModel model1 = models.getFirst();
        modelOffset = model1.offset;
        modelVoxelsize = model1.voxelSize;
        modelWidth = model1.width;
        if (modelChunks == 0)
            modelChunks = 1;
        colourMinY = modelOffset.y - modelWidth / 2f;
        buildColourCache();

        if (debugEnabled) {
            renderSolidsWithDebug(models);
            return;
        }

        getChunkDepths(models);
        sortChunks(models.size(), models);

        for (SolidModel model : models){
            renderSolidModel(model);
        }
    }

    private void renderSolidsWithDebug(ArrayList<SolidModel> models){
        float chunkSortTime = 0f;
        float cullTime = 0f;
        float triangleSortTime = 0f;
        DrawDebugTimes drawTimes = new DrawDebugTimes();

        long start = System.nanoTime();
        getChunkDepths(models);
        sortChunks(models.size(), models);
        long end = System.nanoTime();
        chunkSortTime += (end - start) / 1_000_000f;

        for (SolidModel model : models){
            ModelDebugTimes times = renderSolidModelWithDebug(model);
            cullTime += times.cullTime;
            triangleSortTime += times.triangleSortTime;
            drawTimes.add(times.drawTimes);
        }

        float drawTime = drawTimes.getTotal();
        float totalTime = chunkSortTime + cullTime + triangleSortTime + drawTime;

        System.out.println("Chunk sort time: " + formatDebugTime(chunkSortTime, totalTime));
        System.out.println("Cull time: " + formatDebugTime(cullTime, totalTime));
        System.out.println("Triangle sort time: " + formatDebugTime(triangleSortTime, totalTime));
        System.out.println("Draw time: " + formatDebugTime(drawTime, totalTime));
        System.out.println("  Fill changes: " + drawTimes.fillChanges);
        System.out.println("  Projection: " + formatDebugTime(drawTimes.projectionTime, drawTime) +
                " | " + formatAverageTime(drawTimes.projectionTime, drawTimes.totalVertices, "vertex"));
        System.out.println("  Screen coords: " + formatDebugTime(drawTimes.screenCoordTime, drawTime) +
                " | " + formatAverageTime(drawTimes.screenCoordTime, drawTimes.drawnTriangles, "triangle"));
        System.out.println("  Colour lookup: " + formatDebugTime(drawTimes.colourLookupTime, drawTime) +
                " | " + formatAverageTime(drawTimes.colourLookupTime, drawTimes.drawnTriangles, "triangle"));
        System.out.println("  Set fill: " + formatDebugTime(drawTimes.setFillTime, drawTime) +
                " | " + formatAverageTime(drawTimes.setFillTime, drawTimes.fillChanges, "change"));
        System.out.println("  Fill polygons: " + formatDebugTime(drawTimes.fillPolygonTime, drawTime) +
                " | " + formatAverageTime(drawTimes.fillPolygonTime, drawTimes.drawnTriangles, "triangle"));
        System.out.println("Total render time: " + String.format("%.2fms", totalTime));
        System.out.println();
    }

    private String formatDebugTime(float time, float totalTime){
        float percentage = totalTime == 0f ? 0f : 100f * time / totalTime;
        return String.format("%.2fms, %.2f%%", time, percentage);
    }

    private String formatAverageTime(float time, int count, String unit){
        if (count == 0)
            return "0.0000ms/" + unit;

        return String.format("%.4fms/%s", time / count, unit);
    }

    private static class ModelDebugTimes {
        float cullTime;
        float triangleSortTime;
        DrawDebugTimes drawTimes;
    }

    private static class DrawDebugTimes {
        float projectionTime;
        float screenCoordTime;
        float colourLookupTime;
        float setFillTime;
        float fillPolygonTime;
        int totalVertices;
        int projectedVertices;
        int rejectedVertices;
        int consideredTriangles;
        int drawnTriangles;
        int skippedTriangles;
        int fillChanges;

        void add(DrawDebugTimes other){
            projectionTime += other.projectionTime;
            screenCoordTime += other.screenCoordTime;
            colourLookupTime += other.colourLookupTime;
            setFillTime += other.setFillTime;
            fillPolygonTime += other.fillPolygonTime;
            totalVertices += other.totalVertices;
            projectedVertices += other.projectedVertices;
            rejectedVertices += other.rejectedVertices;
            consideredTriangles += other.consideredTriangles;
            drawnTriangles += other.drawnTriangles;
            skippedTriangles += other.skippedTriangles;
            fillChanges += other.fillChanges;
        }

        float getTotal(){
            return projectionTime + screenCoordTime + colourLookupTime + setFillTime + fillPolygonTime;
        }
    }

    public void getChunkDepths(ArrayList<SolidModel> models){
        int len = models.size();
        if (chunkDepths == null ||
            chunkDepths.length < len)
            chunkDepths = new float[len];

        for (int i = 0; i < len; i++) {
            chunkDepths[i] = cam.getDepth(models.get(i).offset);
        }

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

    private void renderSolidModel(SolidModel model){
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

        postDirtyRender(model);
    }

    private void buildSolidModelCache(SolidModel model){
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
                        addFace(verts, tris,
                                new Vector3(wx,   wy,   wz),
                                new Vector3(wx,   wy+model.voxelSize, wz),
                                new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz),
                                new Vector3(wx+model.voxelSize, wy,   wz)
                        );

                    if (!model.isSolid(i, j, k + 1))
                        addFace(verts, tris,
                                new Vector3(wx,   wy,   wz+model.voxelSize),
                                new Vector3(wx+model.voxelSize, wy,   wz+model.voxelSize),
                                new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz+model.voxelSize),
                                new Vector3(wx,   wy+model.voxelSize, wz+model.voxelSize)
                        );

                    if (!model.isSolid(i - 1, j, k))
                        addFace(verts, tris,
                                new Vector3(wx, wy,   wz),
                                new Vector3(wx, wy,   wz+model.voxelSize),
                                new Vector3(wx, wy+model.voxelSize, wz+model.voxelSize),
                                new Vector3(wx, wy+model.voxelSize, wz)
                        );

                    if (!model.isSolid(i + 1, j, k))
                        addFace(verts, tris,
                                new Vector3(wx+model.voxelSize, wy,   wz),
                                new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz),
                                new Vector3(wx+model.voxelSize, wy+model.voxelSize, wz+model.voxelSize),
                                new Vector3(wx+model.voxelSize, wy,   wz+model.voxelSize)
                        );

                    if (!model.isSolid(i, j - 1, k))
                        addFace(verts, tris,
                                new Vector3(wx,   wy, wz),
                                new Vector3(wx+model.voxelSize, wy, wz),
                                new Vector3(wx+model.voxelSize, wy, wz+model.voxelSize),
                                new Vector3(wx,   wy, wz+model.voxelSize)
                        );

                    if (!model.isSolid(i, j + 1, k))
                        addFace(verts, tris,
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

    private ModelDebugTimes renderSolidModelWithDebug(SolidModel model){
        if (model.dirty)
            buildSolidModelCache(model);

        Vector3[] vertices = model.cachedVerts;
        ModelDebugTimes times = new ModelDebugTimes();

        long start = System.nanoTime();
        int count = backfaceCullTriangles(model.cachedTris, model.cachedVerts);
        long end = System.nanoTime();
        times.cullTime = (end - start) / 1_000_000f;

        start = System.nanoTime();
        sortTriangles(count);
        end = System.nanoTime();
        times.triangleSortTime = (end - start) / 1_000_000f;

        times.drawTimes = drawTrianglesWithDebug(model.cachedTris, vertices, count);

        return times;
    }

    private void postDirtyRender(SolidModel model){
        Vector3[] vertices = model.cachedVerts;

        //sorting the triangles
        int count = backfaceCullTriangles(model.cachedTris, model.cachedVerts);
        sortTriangles(count);

        drawTriangles(model.cachedTris, vertices, count);
    }

    private int backfaceCullTriangles(int[][] triangles, Vector3[] vertices){
        if (visibleTriangleIndexes == null ||
            visibleTriangleIndexes.length < triangles.length){
            visibleTriangleIndexes = new int[triangles.length];
            triangleDepths = new float[triangles.length];
        }


        int count = 0;
        for (int i = 0; i < triangles.length; i++) {
            Vector3 a = vertices[triangles[i][0]];
            Vector3 b = vertices[triangles[i][1]];
            Vector3 c = vertices[triangles[i][2]];

            if (cam.isTriangleFacingCamera(a, b, c)) {
                visibleTriangleIndexes[count] = i;
                triangleDepths[count++] = cam.getTriangleDepth(triangles[i], vertices);
            }
        }
        return count;
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
        colourMinY = modelOffset.y - modelWidth / 2f;
        renderSolids(models);
        return true;
    }

    private void buildColourCache(){
        int length = modelChunks * model1Units() + 1;
        if (cachedColours != null &&
        length == cachedColours.length)
            return;

        cachedColours = new Color[length];
        for (int i = 0; i < length; i++) {
            cachedColours[i] = ColourMap.getColour(
                i,
                length,
                colourMap
            );
        }
    }

    private void getProjectedPoints(Vector3[] vertices){
        getProjectedPointStorage(vertices.length);

        for (int i = 0; i < vertices.length; i++) {
            if (!cam.project(vertices[i], projectedPoint)){
                projectedToRender[i] = false;
                continue;
            }

            projectedX[i] = projectedPoint.x;
            projectedY[i] = projectedPoint.y;

            projectedToRender[i] = true;
        }
    }

    private void getProjectedPointStorage(int length){
        if (projectedX == null || projectedX.length < length) {
            projectedX = new float[length];
            projectedY = new float[length];
            projectedToRender = new boolean[length];

            triangleX = new double[3];
            triangleY = new double[3];
        }
    }

    private void drawTriangles(int[][] triangles, Vector3[] vertices, int count){
        getProjectedPoints(vertices);

        int centreX = Config.SCREEN_WIDTH / 2;
        int centreY = Config.SCREEN_HEIGHT / 2;

        int lastIndex = -1;
        for (int j = 0; j < count; j++) {
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

            int index = getColourIndex(vertices[i0].y);
            Color c = cachedColours[index];



            if (lastIndex != index) {
                gc.setFill(c);
                lastIndex = index;
            }

            gc.fillPolygon(triangleX, triangleY, 3);
        }
    }

    private DrawDebugTimes drawTrianglesWithDebug(int[][] triangles, Vector3[] vertices, int count){
        DrawDebugTimes times = new DrawDebugTimes();

        long start = System.nanoTime();
        ProjectionDebugStats projectionStats = getProjectedPointsWithDebug(vertices);
        long end = System.nanoTime();
        times.projectionTime = (end - start) / 1_000_000f;
        times.totalVertices = projectionStats.totalVertices;
        times.projectedVertices = projectionStats.projectedVertices;
        times.rejectedVertices = projectionStats.rejectedVertices;
        times.consideredTriangles = count;

        int centreX = Config.SCREEN_WIDTH / 2;
        int centreY = Config.SCREEN_HEIGHT / 2;

        int lastIndex = -1;
        for (int j = 0; j < count; j++) {
            int[] triangle = triangles[visibleTriangleIndexes[j]];

            int i0 = triangle[0];
            int i1 = triangle[1];
            int i2 = triangle[2];

            if (!projectedToRender[i0] ||
                !projectedToRender[i1] ||
                !projectedToRender[i2]){
                times.skippedTriangles++;
                continue;
            }

            start = System.nanoTime();
            triangleX[0] = projectedX[i0] + centreX;
            triangleX[1] = projectedX[i1] + centreX;
            triangleX[2] = projectedX[i2] + centreX;

            triangleY[0] = projectedY[i0] + centreY;
            triangleY[1] = projectedY[i1] + centreY;
            triangleY[2] = projectedY[i2] + centreY;
            end = System.nanoTime();
            times.screenCoordTime += (end - start) / 1_000_000f;

            start = System.nanoTime();
            int index = getColourIndex(vertices[i0].y);
            Color c = cachedColours[index];
            end = System.nanoTime();
            times.colourLookupTime += (end - start) / 1_000_000f;

            if (lastIndex != index) {
                start = System.nanoTime();
                gc.setFill(c);
                end = System.nanoTime();
                times.setFillTime += (end - start) / 1_000_000f;
                lastIndex = index;
                times.fillChanges++;
            }

            start = System.nanoTime();
            gc.fillPolygon(triangleX, triangleY, 3);
            end = System.nanoTime();
            times.fillPolygonTime += (end - start) / 1_000_000f;
            times.drawnTriangles++;
        }

        return times;
    }

    private static class ProjectionDebugStats {
        int totalVertices;
        int projectedVertices;
        int rejectedVertices;
    }

    private ProjectionDebugStats getProjectedPointsWithDebug(Vector3[] vertices){
        ProjectionDebugStats stats = new ProjectionDebugStats();
        stats.totalVertices = vertices.length;

        getProjectedPointStorage(vertices.length);

        for (int i = 0; i < vertices.length; i++) {
            if (!cam.project(vertices[i], projectedPoint)){
                projectedToRender[i] = false;
                stats.rejectedVertices++;
                continue;
            }

            projectedX[i] = projectedPoint.x;
            projectedY[i] = projectedPoint.y;

            projectedToRender[i] = true;
            stats.projectedVertices++;
        }

        return stats;
    }

    private int model1Units(){
        return Math.round(modelWidth / modelVoxelsize);
    }

    private int getColourIndex(float y){
        int index = (int)((y - colourMinY) / modelVoxelsize);

        if (index < 0)
            return 0;

        if (index >= cachedColours.length)
            return cachedColours.length - 1;

        return index;
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
