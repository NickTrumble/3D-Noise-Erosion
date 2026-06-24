package org.noiseErosion;

import org.noiseErosion.lib.World;

import java.util.ArrayList;

public class Engine {
    private ArrayList<Model> hollowModels;
    private ArrayList<SolidModel> solidModels;
    private final Camera cam;
    private final Renderer renderer;
    private World world;

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

    public void loadWorld(World world){
        this.world = world;

        float cx = (world.getWorldWidth() - 1) * 4f / 2f;
        float cy = (world.getWorldHeight() - 1) * 4f / 2f;
        float cz = (world.getWorldDepth() - 1) * 4f / 2f;

        cam.setLookat(new Vector3(cx, cy, cz));
    }

    public void start(){
        if (world == null){
            //models
            Window.launchWindow(renderer, hollowModels, solidModels);
        } else {
            //worlds
            Window.launchWindow(renderer, world);
        }
    }
}
