package org.noiseErosion;

public class Main {
    public static void main(String[] args){
        Vector3 camPosition = new Vector3(5, 0, 10);
        Vector3 modelCentre = new Vector3(0, 0, 0);
        Engine engine = new Engine(camPosition, modelCentre);

        Vector3 offset = new Vector3(0, 0, 0);
        Model m = new Model(2, modelCentre);

        engine.addModel(m);
        engine.start();

    }

    //public static void log(String msg) {System.out.println(msg); }
}
