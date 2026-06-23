package org.noiseErosion;

import javafx.scene.paint.Color;

public class Main {
    public static void main(String[] args){
        Vector3 camPosition = new Vector3(5, 0, 10);
        Vector3 modelCentre = new Vector3(0, 0, -5);

        SolidModel sm = new SolidModel(4, modelCentre, 32);
        sm.setColour(Color.GREEN);

        sm.loadSolidState(Noise.apply(sm, 0.1f, 0.2f));


        Engine engine = new Engine(camPosition, modelCentre);
        engine.addModel(sm);

        engine.start();
    }
}
