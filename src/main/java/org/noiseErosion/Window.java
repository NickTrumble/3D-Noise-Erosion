package org.noiseErosion;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.noiseErosion.lib.World;

public class Window extends Application {
    private static Renderer renderer;
    private static World world;

    private static float orbitRadius = -5f;
    private float manualSpinAmount = 0.1f;
    private float orbitRadiusChange = 5f;
    private float movementStep = 1f;

    private boolean spinCamera = true;

    private long lastFPS = 0;
    private int frames = 0;
    private float fps;

    private int modelWidth = 4;
    private int chunkCount = 6;
    private int voxelsPerChunk = 8;
    private int worldSeed = 0;

    public static void launchWindow(Renderer r, World w){
        renderer = r;
        world = w;
        orbitRadius = renderer.cam.getPosition().z;
        Application.launch(Window.class);
    }

    @Override
    public void start(Stage stage){
        loadWorldSettings();
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
        ControlPanel controlPanel = new ControlPanel();
        Button resetCameraButton = controlPanel.addButton("Reset Camera");
        resetCameraButton.setOnAction(event -> {
            resetCamera();
            canvas.requestFocus();
        });

        ToggleButton debugToggle = controlPanel.addToggle("Debug Mode");
        debugToggle.setOnAction(event -> {
            renderer.setDebugEnabled(debugToggle.isSelected());
            canvas.requestFocus();
        });

        Slider modelWidthSlider = controlPanel.addIntSlider("Model Width", 2, Math.max(12, modelWidth), modelWidth);
        Slider chunkCountSlider = controlPanel.addIntSlider("Chunks", 1, Math.max(10, chunkCount), chunkCount);
        Slider voxelsPerChunkSlider = controlPanel.addIntSlider("Voxels/Chunk", 4, Math.max(16, voxelsPerChunk), voxelsPerChunk);
        addWorldRebuildHandlers(modelWidthSlider, chunkCountSlider, voxelsPerChunkSlider, canvas);

        BorderPane root = new BorderPane();
        root.setLeft(controlPanel.getRoot());
        BorderPane.setMargin(controlPanel.getRoot(), new Insets(12, 0, 12, 12));
        root.setCenter(canvas);

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

    private void loadWorldSettings(){
        if (world == null)
            return;

        modelWidth = world.getModelWidth();
        chunkCount = world.getWorldWidth();
        voxelsPerChunk = world.getChunkWidth();
        worldSeed = world.getSeed();
    }

    private void addWorldRebuildHandlers(Slider modelWidthSlider, Slider chunkCountSlider, Slider voxelsPerChunkSlider, Canvas canvas){
        Slider[] sliders = { modelWidthSlider, chunkCountSlider, voxelsPerChunkSlider };

        for (Slider slider : sliders) {
            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                rebuildWorldFromSliders(modelWidthSlider, chunkCountSlider, voxelsPerChunkSlider);
                canvas.requestFocus();
            });
        }
    }

    private void rebuildWorldFromSliders(Slider modelWidthSlider, Slider chunkCountSlider, Slider voxelsPerChunkSlider){
        int newModelWidth = (int)Math.round(modelWidthSlider.getValue());
        int newChunkCount = (int)Math.round(chunkCountSlider.getValue());
        int newVoxelsPerChunk = (int)Math.round(voxelsPerChunkSlider.getValue());

        if (newModelWidth == modelWidth &&
            newChunkCount == chunkCount &&
            newVoxelsPerChunk == voxelsPerChunk)
            return;

        modelWidth = newModelWidth;
        chunkCount = newChunkCount;
        voxelsPerChunk = newVoxelsPerChunk;
        rebuildWorld();
    }

    private void rebuildWorld(){
        if (world == null)
            return;

        World newWorld = new World(
                new Vector3(chunkCount, chunkCount, chunkCount),
                voxelsPerChunk,
                modelWidth,
                worldSeed
        );
        newWorld.generateWorld();
        world = newWorld;
        centreCameraOnWorld();
    }

    private void centreCameraOnWorld(){
        Vector3 oldPosition = renderer.cam.getPosition();
        Vector3 oldLookat = renderer.cam.getLookat();
        Vector3 cameraOffset = new Vector3(
                oldPosition.x - oldLookat.x,
                oldPosition.y - oldLookat.y,
                oldPosition.z - oldLookat.z
        );

        float chunkSize = world.getChunkWidth() * world.getVoxelSize();

        float cx = (world.getWorldWidth() - 1) * chunkSize / 2f;
        float cy = (world.getWorldHeight() - 1) * chunkSize / 2f;
        float cz = (world.getWorldDepth() - 1) * chunkSize / 2f;
        Vector3 centre = new Vector3(cx, cy, cz);

        Vector3 cameraPosition = new Vector3(
                centre.x + cameraOffset.x,
                centre.y + cameraOffset.y,
                centre.z + cameraOffset.z
        );

        renderer.cam.setLookat(centre);
        renderer.cam.setPosition(cameraPosition);
        Config.DEFAULT_LOOKAT = new Vector3(centre);
        Config.DEFAULT_POSITION = new Vector3(cameraPosition);
        Config.DEFAULT_ORBIT = orbitRadius;
    }
}
