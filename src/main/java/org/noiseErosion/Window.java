package org.noiseErosion;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    private float manualSpinAmount = 0.1f;
    private float orbitRadiusChange = 5f;
    private float movementStep = 1f;

    private boolean spinCamera = true;

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

        canvas.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            KeyCode key = keyEvent.getCode();
            System.out.println(key);
            switch (key){
                //ROTATIONS
                case KeyCode.SPACE:
                    spinCamera = !spinCamera;
                    break;
                case KeyCode.E:
                    updateCameraPos(manualSpinAmount);
                    break;
                case KeyCode.Q:
                    updateCameraPos(-manualSpinAmount);
                    break;
                //MOVEMENT
                case KeyCode.W:
                    updateOrbitRadius(orbitRadiusChange);
                    break;
                case KeyCode.S:
                    updateOrbitRadius(-orbitRadiusChange);
                    break;
                case KeyCode.D:
                    moveCameraSideways(movementStep);
                    break;
                case A:
                    moveCameraSideways(-movementStep);
                    break;
                default:
                    break;
            }
        });

        renderer.setGraphicsContext(gc);
        Pane root = new Pane(canvas);

        stage.setScene(new Scene(root));
        stage.setTitle("Noise Erosion");
        stage.show();

        canvas.requestFocus();

        renderer.renderHollows(models);


        new AnimationTimer(){
            @Override
            public void handle(long now){
                renderer.clearScreen();
                if (spinCamera)
                    updateCameraPos(0.001f);
                if (renderer.renderWorld(world))
                    return;
                SolidModel model = solidModels.getFirst();
                renderer.maxHeight = model.units * model.voxelSize;
                if (renderer.renderSolids(solidModels))
                    return;

                Model hmodel = models.getFirst();
                renderer.maxHeight = hmodel.getEdgeLength();
                renderer.renderHollows(models);
            }
        }.start();
    }

    public void updateCameraPos(float angleIncrement){
        renderer.cam.rotateCam(angleIncrement, orbitRadius);
    }

    public void updateOrbitRadius(float increment){
        orbitRadius = Math.max(1, orbitRadius - increment);
    }

    public void moveCameraSideways(float increment){
        Vector3 right = renderer.cam.getRight();
        right.normalise();
        right.multiply(-increment);
        renderer.cam.moveCamera(right);
        System.out.println(right);
    }
}
