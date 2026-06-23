package org.noiseErosion;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public class Renderer {
    public Camera cam;
    private GraphicsContext gc;

    public Renderer(Camera cam){
        this.cam = cam;
    }

    //hollow models
    public void renderHollows(ArrayList<Model> models){
        if (models == null)
            return;
        for (Model model : models){
            renderHollowModel(model);
        }
    }

    public void renderHollowModel(Model model){
        int[][] triangles = model.getTriangles();
        Vector3[] vertices = model.getVertices();
        boolean[] trianglesToRender = getTrianglesToRender(triangles, vertices);

        drawTriangles(triangles, trianglesToRender, vertices, false);
    }

    //solid model

    public void renderSolids(ArrayList<SolidModel> models){
        if (models == null)
            return;
        for (SolidModel model : models){
            renderSolidModel(model);
        }
    }

    public void renderSolidModel(SolidModel model){
        int width = model.width;
        Vector3 offset = model.offset;
        ArrayList<Vector3> verts = new ArrayList<>();
        ArrayList<int[]> tris = new ArrayList<>();

        float half = width / 2f;

        for(int i = 0; i < width; i++){
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < width; k++) {
                    if(!model.isSolid(i, j, k))
                        continue;

                    float wx = offset.x + i - half;
                    float wy = offset.y + j - half;
                    float wz = offset.z + k - half;

                    if (!model.isSolid(i, j, k - 1))
                        addFace(verts, tris,  // -k face — reversed
                                new Vector3(wx,   wy,   wz),
                                new Vector3(wx,   wy+1, wz),   // swap b and d
                                new Vector3(wx+1, wy+1, wz),
                                new Vector3(wx+1, wy,   wz)
                        );

                    if (!model.isSolid(i, j, k + 1))
                        addFace(verts, tris, // +k face
                                new Vector3(wx,   wy,   wz+1),
                                new Vector3(wx+1, wy,   wz+1),
                                new Vector3(wx+1, wy+1, wz+1),
                                new Vector3(wx,   wy+1, wz+1)
                        );

                    if (!model.isSolid(i - 1, j, k))
                        addFace(verts, tris, // -i face
                                new Vector3(wx, wy,   wz),
                                new Vector3(wx, wy,   wz+1),
                                new Vector3(wx, wy+1, wz+1),
                                new Vector3(wx, wy+1, wz)
                        );

                    if (!model.isSolid(i + 1, j, k))
                        addFace(verts, tris, // +i face
                                new Vector3(wx+1, wy,   wz),
                                new Vector3(wx+1, wy+1, wz),
                                new Vector3(wx+1, wy+1, wz+1),
                                new Vector3(wx+1, wy,   wz+1)
                        );

                    if (!model.isSolid(i, j - 1, k))
                        addFace(verts, tris, // -j face
                                new Vector3(wx,   wy, wz),
                                new Vector3(wx+1, wy, wz),
                                new Vector3(wx+1, wy, wz+1),
                                new Vector3(wx,   wy, wz+1)
                        );

                    if (!model.isSolid(i, j + 1, k))
                        addFace(verts, tris, // +j face
                                new Vector3(wx,   wy+1, wz),
                                new Vector3(wx,   wy+1, wz+1),
                                new Vector3(wx+1, wy+1, wz+1),
                                new Vector3(wx+1, wy+1, wz)
                        );
                }
            }
        }

        Vector3[] vertices = verts.toArray(new Vector3[0]);
        int[][] triangles = tris.toArray(new int[0][]);

        boolean[] trianglesToRender = getTrianglesToRender(triangles, vertices);

        drawTriangles(triangles, trianglesToRender, vertices, model.colour != null);
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

    //needed for both versions
    private boolean[] getVerticesToRender(Vector3[] vertices){
        boolean[] verticesToRender = new boolean[vertices.length];

        for (int i = 0; i < vertices.length; i++){
            verticesToRender[i] = cam.isVertexInFrame(vertices[i]);
        }
        return verticesToRender;
    }

    private boolean[] getTrianglesToRender(int[][] triangles, Vector3[] vertices){
        boolean[] verticesToRender = getVerticesToRender(vertices);
        boolean[] trianglesToRender = new boolean[triangles.length];
        for (int i = 0; i < triangles.length; i++){
            trianglesToRender[i] = cam.isTriangleInFrame(i, triangles, verticesToRender);
        }
        return trianglesToRender;
    }

    private void drawTriangles(int[][] triangles, boolean[] trianglesToRender, Vector3[] vertices, boolean colour){
        for (int i = 0; i < triangles.length; i++){
            if (!trianglesToRender[i])
                continue;

            drawTriangle(triangles[i], vertices, colour);
        }
    }

    public void drawTriangle(int[] triangle, Vector3[] vertices, boolean colour){
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

        float maxColour = 0.8f;
        if (facingVal <= 0){
            return;
        } else {
            gc.setFill(Color.color(facingVal * maxColour, facingVal * maxColour, facingVal * maxColour));
        }

        if (colour){
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
        else{
            gc.strokePolygon(
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
                    3
            );
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

    public Vector3 getCentre(Vector3 a, Vector3 b, Vector3 c){
        return new Vector3(
                (a.x + b.x + c.x) / 3f,
                (a.y + b.y + c.y) / 3f,
                (a.z + b.z + c.z) / 3f
        );
    }

    public void clearScreen(){
        gc.clearRect(0, 0, Config.SCREEN_WIDTH, Config.SCREEN_HEIGHT);
    }

    public void setGraphicsContext(GraphicsContext gc){
        this.gc = gc;
    }
}
