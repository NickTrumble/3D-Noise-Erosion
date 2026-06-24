package org.noiseErosion;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.noiseErosion.lib.World;

import java.util.ArrayList;

public class Window extends Application {
    private static Renderer renderer;
    private static ArrayList<Model> models;
    private static ArrayList<SolidModel> solidModels;
    private static World world;

    private float angle = 0f;
    private static float orbitRadius;

    public static void launchWindow(Renderer r, ArrayList<Model> m, ArrayList<SolidModel> sm){
        renderer = r;
        models = m;
        solidModels = sm;
        orbitRadius = renderer.cam.getPosition().z;
        Application.launch(Window.class);
    }

    public static void launchWindow(Renderer r, World w){
        renderer = r;
        world = w;
        orbitRadius = renderer.cam.getPosition().z;
        Application.launch(Window.class);
    }

    @Override
    public void start(Stage stage){
        Canvas canvas = new Canvas(Config.SCREEN_WIDTH, Config.SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        canvas.addEventFilter(ScrollEvent.SCROLL, scrollEvent -> {
            double delta = scrollEvent.getDeltaY();
            if (delta != 0) {
                orbitRadius -= (float) (delta / 40f);
                orbitRadius = Math.max(5f, orbitRadius);
            }

        });

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
                updateCameraPos();
                ColourMap.colourSwitch = ColourMap.ColourSwitch.WORLD;
                if (renderer.renderWorld(world))
                    return;
                ColourMap.colourSwitch = ColourMap.ColourSwitch.MODEL;
                SolidModel model = solidModels.getFirst();
                renderer.maxHeight = model.units * model.voxelSize;
                if (renderer.renderSolids(solidModels))
                    return;

                Model hmodel = models.getFirst();
                renderer.maxHeight = hmodel.getEdgeLength();
                if (renderer.renderHollows(models))
                    return;

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
