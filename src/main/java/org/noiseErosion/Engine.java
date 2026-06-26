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
        Config.DEFAULT_LOOKAT = camLookAt;
        Config.DEFAULT_POSITION = campos;
        renderer = new Renderer(cam);
        hollowModels = new ArrayList<>();
    }

    public void addModel(Model model){
        if (hollowModels == null)
            hollowModels = new ArrayList<>();
        hollowModels.add(model);
        centreCameraHollow();
    }

    public void addModel(SolidModel model){
        if (solidModels == null)
            solidModels = new ArrayList<>();
        solidModels.add(model);
        centreCameraSolid();
    }

    public void loadWorld(World world){
        this.world = world;

        float chunkSize = world.getChunkWidth() * world.getVoxelSize();

        float cx = (world.getWorldWidth() - 1) * chunkSize / 2f;
        float cy = (world.getWorldHeight() - 1) * chunkSize / 2f;
        float cz = (world.getWorldDepth() - 1) * chunkSize / 2f;

        cam.setLookat(new Vector3(cx, cy, cz));
    }

    public void centreCameraHollow(){
        Model m = hollowModels.getFirst();
        float cx = (m.getEdgeLength() - 1) * 4f / 2f;

        cam.setLookat(new Vector3(cx, cx, cx));
    }

    public void centreCameraSolid(){
        SolidModel sm = solidModels.getFirst();
        float size = sm.voxelSize * sm.units;

        float cx = sm.offset.x + size / 2f;
        float cy = sm.offset.y + size / 2f;
        float cz = sm.offset.z + size / 2f;

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
