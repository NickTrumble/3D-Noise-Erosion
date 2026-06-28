package org.noiseErosion;

import org.noiseErosion.lib.World;

public class Main {
    public static void main(String[] args){
        int mWidth = 4;
        int chunks = 6;
        World world = new World(new Vector3(chunks, chunks, chunks), 8, mWidth);
        world.generateWorld();

        float cameraDepth = mWidth * chunks * 2f;
        Vector3 cameraPosition = new Vector3(
                0,
                mWidth * 2,
                cameraDepth
        );

        Engine engine = new Engine(cameraPosition);
        engine.loadWorld(world);

        engine.start();
    }
}
