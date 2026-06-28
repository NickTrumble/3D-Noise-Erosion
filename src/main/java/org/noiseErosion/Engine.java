package org.noiseErosion;

import org.noiseErosion.lib.World;

public class Engine {
    private final Camera cam;
    private final Renderer renderer;
    private World world;

    public Engine(Vector3 campos){
        cam = new Camera(campos, new Vector3(0, 0, 0), new Vector3(0, 1, 0));
        Config.DEFAULT_POSITION = campos;
        renderer = new Renderer(cam);
    }

    public void loadWorld(World world){
        this.world = world;

        float chunkSize = world.getChunkWidth() * world.getVoxelSize();

        float cx = (world.getWorldWidth() - 1) * chunkSize / 2f;
        float cy = (world.getWorldHeight() - 1) * chunkSize / 2f;
        float cz = (world.getWorldDepth() - 1) * chunkSize / 2f;

        Vector3 centre = new Vector3(cx, cy, cz);
        cam.setLookat(centre);
        Config.DEFAULT_LOOKAT = new Vector3(centre);
    }

    public void start(){
        Window.launchWindow(renderer, world);
    }
}
