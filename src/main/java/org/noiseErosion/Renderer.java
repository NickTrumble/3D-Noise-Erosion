package org.noiseErosion;

import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;

public class Renderer {
    private Camera cam;
    private GraphicsContext gc;

    public Renderer(Camera cam){
        this.cam = cam;
    }

    public void render(ArrayList<Model> models){
        for (Model model : models){
            renderModel(model);
        }
    }

    public void renderModel(Model model){
        int[][] triangles = model.getTriangles();
        Vector3[] vertices = model.getVertices();
        boolean[] trianglesToRender = new boolean[triangles.length];
        boolean[] verticesToRender = new boolean[vertices.length];

        for (int i = 0; i < vertices.length; i++){
            verticesToRender[i] = cam.isVertexInFrame(vertices[i]);
        }

        for (int i = 0; i < triangles.length; i++){
            trianglesToRender[i] = cam.isTriangleInFrame(i, triangles, verticesToRender);
        }

        for (int i = 0; i < triangles.length; i++){
            if (!trianglesToRender[i])
                continue;

            drawTriangle(triangles[i], vertices);
        }

    }

    public void drawTriangle(int[] triangle, Vector3[] vertices){
        if (gc == null)
            return;

        int centreX = Config.SCREEN_WIDTH / 2;
        int centreY = Config.SCREEN_HEIGHT / 2;

        //get screen coordinates
        Vector3 a = cam.project(vertices[triangle[0]]);
        Vector3 b = cam.project(vertices[triangle[1]]);
        Vector3 c = cam.project(vertices[triangle[2]]);

        if (a == null || b == null || c == null)
            return;

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

    public void setGraphicsContext(GraphicsContext gc){
        this.gc = gc;
    }
}
