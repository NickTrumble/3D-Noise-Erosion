package org.noiseErosion;

import java.util.ArrayList;

public class Engine {
    private ArrayList<Model> models;
    private Camera cam;
    private Renderer renderer;

    public Engine(){
        cam = new Camera();
        renderer = new Renderer(cam);
        models = new ArrayList<>();
    }

    public void addModel(Model model){
        models.add(model);
    }

    public void start(){
        Window.launchWindow(renderer, models);
    }
}
