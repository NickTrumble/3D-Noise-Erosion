package org.noiseErosion;

import javafx.scene.paint.Color;

public class Main {
    public static void main(String[] args){
        Vector3 camPosition = new Vector3(5, 0, 10);
        Vector3 modelCentre = new Vector3(0, -5, -5);

        SolidModel sm = new SolidModel(4, modelCentre);
        sm.setColour(Color.BLACK);

        Engine engine = new Engine(camPosition, modelCentre);

        engine.addModel(sm);

        engine.start();



    }

    //public static void log(String msg) {System.out.println(msg); }
}
