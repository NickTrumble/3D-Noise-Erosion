package org.noiseErosion;

import javafx.scene.paint.Color;

public class Main {
    public static void main(String[] args){
        Vector3 camPosition = new Vector3(5, 0, 10);
        Vector3 modelCentre = new Vector3(0, 0, -5);

        SolidModel sm = new SolidModel(4, modelCentre, 4);
        sm.setColour(Color.BLANCHEDALMOND);

        sm.setSolid(false, 0, 0, 0);
        sm.setSolid(false, 1, 0, 0);
        sm.setSolid(false, 2, 0, 0);

        Engine engine = new Engine(camPosition, modelCentre);

        engine.addModel(sm);

        engine.start();
    }
}
