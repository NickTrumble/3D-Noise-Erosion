package org.noiseErosion;

import javafx.scene.paint.Color;
import org.noiseErosion.lib.World;

public class Main {
    public static void main(String[] args){
        Vector3 camPosition = new Vector3(0, 0, 30);
        Vector3 modelCentre = new Vector3(0, 0, -5);
        Vector3 modelCentre2 = new Vector3(0, 0, -9);

        SolidModel sm = new SolidModel(4, modelCentre, 32);
        sm.setColour(Color.GREEN);
        SolidModel sm2 = new SolidModel(4, modelCentre2, 32);


        sm.loadSolidState(Noise.apply(sm, 0.1f, 0.4f));

        World world = new World(new Vector3(2,2,2), 8, 4);
        world.generateWorld();


        Engine engine = new Engine(camPosition, modelCentre);
        engine.addModel(sm);
        //engine.addModel(sm2);

        engine.loadWorld(world);

        engine.start();
    }
}
