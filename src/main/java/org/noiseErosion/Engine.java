package org.noiseErosion;

import java.util.ArrayList;

public class Engine {
    private ArrayList<Model> hollowModels;
    private ArrayList<SolidModel> solidModels;
    private final Camera cam;
    private final Renderer renderer;

    public Engine(){
        cam = new Camera();
        renderer = new Renderer(cam);
        hollowModels = new ArrayList<>();
        solidModels = new ArrayList<>();
    }

    public Engine(Vector3 campos, Vector3 camLookAt){
        cam = new Camera(campos, camLookAt, new Vector3(0, 1, 0));
        renderer = new Renderer(cam);
        hollowModels = new ArrayList<>();
    }

    public void addModel(Model model){
        if (hollowModels == null)
            hollowModels = new ArrayList<>();
        hollowModels.add(model);
    }

    public void addModel(SolidModel model){
        if (solidModels == null)
            solidModels = new ArrayList<>();
        solidModels.add(model);
    }

    public void start(){
        Window.launchWindow(renderer, hollowModels, solidModels);
    }
}
