package org.noiseErosion;

public class Main {
    public static void main(String[] args){
        Engine engine = new Engine();
        Vector3 offset = new Vector3(-4, -5, 10);
        engine.addModel(new Model(2, offset));
        engine.start();
    }

    //public static void log(String msg) {System.out.println(msg); }
}
