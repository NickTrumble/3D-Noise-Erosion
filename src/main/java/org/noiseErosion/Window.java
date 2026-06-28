package org.noiseErosion;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
    private Stage stage;

    private static float orbitRadius = -5f;
    private float manualSpinAmount = 0.1f;
    private float autoSpinAmount = 0.01f;
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
    private float noiseScale = 0.03f;
    private float noiseThreshold = 0.5f;
    private Slider orbitRadiusSlider;

    public static void launchWindow(Renderer r, World w){
        renderer = r;
        world = w;
        orbitRadius = renderer.cam.getPosition().z;
        Application.launch(Window.class);
    }

    @Override
    public void start(Stage stage){
        this.stage = stage;
        loadWorldSettings();
        Config.DEFAULT_ORBIT = renderer.cam.getPosition().z;
        Canvas canvas = new Canvas(Config.SCREEN_WIDTH, Config.SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        canvas.addEventFilter(ScrollEvent.SCROLL, scrollEvent -> {
            double delta = scrollEvent.getDeltaY();
            if (delta != 0) {
                orbitRadius -= (float) (delta / 40f);
                orbitRadius = Math.max(5f, orbitRadius);
                updateOrbitRadiusSlider();
            }

        });

        canvas.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            KeyCode key = keyEvent.getCode();
            switch (key){
                //ROTATIONS
                case SPACE:
                    spinCamera = !spinCamera;
                    updateTitle();
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
        Button newSeedButton = controlPanel.addButton("New Seed");

        ToggleButton debugToggle = controlPanel.addToggle("Debug Mode");
        debugToggle.setOnAction(event -> {
            renderer.setDebugEnabled(debugToggle.isSelected());
            canvas.requestFocus();
        });

        ComboBox<String> colourMapSelect = controlPanel.addComboBox(
                "Colour Scheme",
                ColourMap.getColourMapNames(),
                ColourMap.VIRIDIS
        );
        colourMapSelect.setOnAction(event -> {
            renderer.setColourMap(ColourMap.getColourMap(colourMapSelect.getValue()));
            canvas.requestFocus();
        });

        Slider modelWidthSlider = controlPanel.addIntSlider("Model Width", 2, Math.max(12, modelWidth), modelWidth);
        Slider chunkCountSlider = controlPanel.addIntSlider("Chunks", 1, Math.max(10, chunkCount), chunkCount);
        Slider voxelsPerChunkSlider = controlPanel.addIntSlider("Voxels/Chunk", 4, Math.max(16, voxelsPerChunk), voxelsPerChunk);
        Slider noiseThresholdSlider = controlPanel.addFloatSlider("Threshold", -1, 1, noiseThreshold);
        Slider noiseScaleSlider = controlPanel.addFloatSlider("Noise Scale", 0.005, 0.1, noiseScale);
        addWorldRebuildHandlers(modelWidthSlider, chunkCountSlider, voxelsPerChunkSlider, noiseThresholdSlider, noiseScaleSlider, canvas);

        orbitRadiusSlider = controlPanel.addFloatSlider("Orbit Radius", 5, Math.max(200, orbitRadius * 2), orbitRadius);
        Slider spinSpeedSlider = controlPanel.addIntSlider("Spin Speed", 1, 10, getSpinSpeedValue());
        Slider movementStepSlider = controlPanel.addFloatSlider("Move Step", 0.1, 10, movementStep);
        addCameraControlHandlers(orbitRadiusSlider, spinSpeedSlider, movementStepSlider, canvas);

        resetCameraButton.setOnAction(event -> {
            resetCamera();
            canvas.requestFocus();
        });

        newSeedButton.setOnAction(event -> {
            generateNewSeed();
            rebuildWorld();
            updateTitle();
            canvas.requestFocus();
        });

        BorderPane root = new BorderPane();
        root.setLeft(controlPanel.getRoot());
        BorderPane.setMargin(controlPanel.getRoot(), new Insets(12, 0, 12, 12));
        root.setCenter(canvas);

        stage.setScene(new Scene(root));
        updateTitle();
        stage.show();

        canvas.requestFocus();


        new AnimationTimer(){
            @Override
            public void handle(long now){
                renderer.clearScreen();
                if (spinCamera)
                    updateCameraPos(autoSpinAmount);
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
            updateTitle();

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
        updateOrbitRadiusSlider();
    }

    private void loadWorldSettings(){
        if (world == null)
            return;

        modelWidth = world.getModelWidth();
        chunkCount = world.getWorldWidth();
        voxelsPerChunk = world.getChunkWidth();
        worldSeed = world.getSeed();
        noiseScale = world.getNoiseScale();
        noiseThreshold = world.getNoiseThreshold();
    }

    private void addWorldRebuildHandlers(Slider modelWidthSlider, Slider chunkCountSlider, Slider voxelsPerChunkSlider, Slider noiseThresholdSlider, Slider noiseScaleSlider, Canvas canvas){
        Slider[] sliders = { modelWidthSlider, chunkCountSlider, voxelsPerChunkSlider, noiseThresholdSlider, noiseScaleSlider };

        for (Slider slider : sliders) {
            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                rebuildWorldFromSliders(modelWidthSlider, chunkCountSlider, voxelsPerChunkSlider, noiseThresholdSlider, noiseScaleSlider);
                canvas.requestFocus();
            });
        }
    }

    private void addCameraControlHandlers(Slider orbitRadiusSlider, Slider spinSpeedSlider, Slider movementStepSlider, Canvas canvas){
        orbitRadiusSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            orbitRadius = newValue.floatValue();
            renderer.cam.rotateCam(0f, orbitRadius);
            canvas.requestFocus();
        });

        spinSpeedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            autoSpinAmount = getSpinAmount(newValue.intValue());
            canvas.requestFocus();
        });

        movementStepSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            movementStep = newValue.floatValue();
            canvas.requestFocus();
        });
    }

    private void updateTitle(){
        if (stage == null)
            return;

        String title = spinCamera ? "Noise Erosion" : "Noise Erosion - Paused";
        String FPS = String.format("%.2f", fps);
        stage.setTitle(title + " | Seed: " + worldSeed + " | FPS: " + FPS);
    }

    private void updateOrbitRadiusSlider(){
        if (orbitRadiusSlider != null &&
            Math.abs(orbitRadiusSlider.getValue() - orbitRadius) > 0.001)
            orbitRadiusSlider.setValue(orbitRadius);
    }

    private void generateNewSeed(){
        worldSeed = (int)(Math.random() * 1_000_000);
    }

    private int getSpinSpeedValue(){
        return Math.max(1, Math.min(10, Math.round(autoSpinAmount * 1000f)));
    }

    private float getSpinAmount(int speed){
        return speed / 1000f;
    }

    private void rebuildWorldFromSliders(Slider modelWidthSlider, Slider chunkCountSlider, Slider voxelsPerChunkSlider, Slider noiseThresholdSlider, Slider noiseScaleSlider){
        int newModelWidth = (int)Math.round(modelWidthSlider.getValue());
        int newChunkCount = (int)Math.round(chunkCountSlider.getValue());
        int newVoxelsPerChunk = (int)Math.round(voxelsPerChunkSlider.getValue());
        float newNoiseThreshold = (float)noiseThresholdSlider.getValue();
        float newNoiseScale = (float)noiseScaleSlider.getValue();

        if (newModelWidth == modelWidth &&
            newChunkCount == chunkCount &&
            newVoxelsPerChunk == voxelsPerChunk &&
            Math.abs(newNoiseThreshold - noiseThreshold) < 0.0001f &&
            Math.abs(newNoiseScale - noiseScale) < 0.0001f)
            return;

        if (newChunkCount != chunkCount)
            generateNewSeed();

        modelWidth = newModelWidth;
        chunkCount = newChunkCount;
        voxelsPerChunk = newVoxelsPerChunk;
        noiseThreshold = newNoiseThreshold;
        noiseScale = newNoiseScale;
        rebuildWorld();
        updateTitle();
    }

    private void rebuildWorld(){
        if (world == null)
            return;

        World newWorld = new World(
                new Vector3(chunkCount, chunkCount, chunkCount),
                voxelsPerChunk,
                modelWidth,
                worldSeed,
                noiseScale,
                noiseThreshold
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
