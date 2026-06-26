package org.noiseErosion;

import javafx.scene.paint.Color;
import org.noiseErosion.lib.World;

public class Main {
    public static void main(String[] args){
        Vector3 modelCentre = new Vector3(0, 0, -5);

        SolidModel sm = new SolidModel(4, modelCentre, 32);
        sm.setColour(Color.GREEN);

        sm.loadSolidState(Noise.apply(sm, 0.1f, 0.4f));

        int mWidth = 4;
        int chunks = 2;
        World world = new World(new Vector3(chunks, chunks, chunks), 8, mWidth);
        world.generateWorld();

        float cameraDepth = mWidth * chunks * 2f;
        Vector3 cameraPosition = new Vector3(
                0,
                mWidth * 2,
                cameraDepth
        );

        Engine engine = new Engine(cameraPosition, modelCentre);
        engine.addModel(sm);
        //engine.addModel(sm2);

        engine.loadWorld(world);

        engine.start();
    }
}
