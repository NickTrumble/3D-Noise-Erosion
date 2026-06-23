package org.noiseErosion;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.util.ArrayList;

public class Window extends Application {
    private static Renderer renderer;
    private static ArrayList<Model> models;
    private static ArrayList<SolidModel> solidModels;

    private float angle = 0f;
    private final float orbitRadius = 10f;

    public static void launchWindow(Renderer r, ArrayList<Model> m, ArrayList<SolidModel> sm){
        renderer = r;
        models = m;
        solidModels = sm;
        Application.launch(Window.class);
    }

    @Override
    public void start(Stage stage){
        Canvas canvas = new Canvas(Config.SCREEN_WIDTH, Config.SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        renderer.setGraphicsContext(gc);
        Pane root = new Pane(canvas);

        stage.setScene(new Scene(root));
        stage.setTitle("Noise Erosion");
        stage.show();

        renderer.renderHollows(models);


        new AnimationTimer(){
            @Override
            public void handle(long now){
                renderer.clearScreen();
                renderer.renderHollows(models);
                renderer.renderSolids(solidModels);
                updateCameraPos();
            }
        }.start();
    }

    public void updateCameraPos(){
        angle += 0.001f;

        Vector3 lookat = renderer.cam.getLookat();

        float camX = lookat.x + (float) Math.cos(angle) * orbitRadius;
        float camZ = lookat.z + (float) Math.sin(angle) * orbitRadius;
        float camY = renderer.cam.getPosition().y;

        renderer.cam.setPosition(new Vector3(camX, camY, camZ));
    }


}
