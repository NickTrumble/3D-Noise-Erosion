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
    private static ArrayList<SolidModel> solidModels;
    private static World world;

    private float angle = 0f;
    private static float orbitRadius = -5f;
    private float manualSpinAmount = 0.1f;
    private float orbitRadiusChange = 5f;
    private float movementStep = 1f;

    private boolean spinCamera = true;

    private long lastFPS = 0;
    private int frames = 0;
    private float fps;

    public static void launchWindow(Renderer r, ArrayList<SolidModel> sm){
        renderer = r;
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
        Config.DEFAULT_ORBIT = renderer.cam.getPosition().z;
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
            switch (key){
                //ROTATIONS
                case SPACE:
                    spinCamera = !spinCamera;
                    String title = spinCamera ? "Noise Erosion" : "Noise Erosion - Paused";
                    stage.setTitle(title);
                    break;
                case E:
                    updateCameraPos(manualSpinAmount);
                    break;
                case Q:
                    updateCameraPos(-manualSpinAmount);
                    break;
                //MOVEMENT
                case W:
                    updateOrbitRadius(orbitRadiusChange);
                    break;
                case S:
                    updateOrbitRadius(-orbitRadiusChange);
                    break;
                case D:
                    moveCameraSideways(-movementStep);
                    break;
                case A:
                    moveCameraSideways(movementStep);
                    break;
                case R:
                    resetCamera();
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


        new AnimationTimer(){
            @Override
            public void handle(long now){
                renderer.clearScreen();
                if (spinCamera)
                    updateCameraPos(0.01f);
                if (renderer.renderWorld(world)) {
                    updateFPS(now, stage);
                    return;
                }
                SolidModel model = solidModels.getFirst();
                renderer.maxHeight = model.units * model.voxelSize;
                if (renderer.renderSolids(solidModels)) {
                    updateFPS(now, stage);
                    return;
                }

                updateFPS(now, stage);
            }
        }.start();
    }

    private void updateFPS(long now, Stage stage){
        frames++;

        if (lastFPS == 0){
            lastFPS = now;
            return;
        }

        long elapsed = now - lastFPS;

        if (elapsed >= 1_000_000_000L){
            fps = (float) (frames * 1_000_000_000L) / elapsed;
            String title = "Noise Erosion";
            String FPS = String.format("%.2f", fps);
            stage.setTitle(title + " | FPS: " + FPS);

            frames = 0;
            lastFPS = 0;
        }
    }

    public void updateCameraPos(float angleIncrement){
        renderer.cam.rotateCam(angleIncrement, orbitRadius);
    }

    public void updateOrbitRadius(float increment){
        orbitRadius -= increment;
        renderer.cam.rotateCam(0f, orbitRadius);
    }

    public void moveCameraSideways(float increment){
        Vector3 right = renderer.cam.getRight();
        right.normalise();
        right.multiply(-increment);
        renderer.cam.moveCamera(right);
    }

    public void resetCamera(){
        orbitRadius = Config.DEFAULT_ORBIT;
        renderer.cam.resetCamera();
    }
}
