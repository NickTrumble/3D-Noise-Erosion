package org.noiseErosion;

import javafx.scene.paint.Color;

public class SolidModel {
    private boolean[][][] solid;
    public int width;
    public Vector3 offset;
    public float voxelScale; //to add
    public Color colour;

    public SolidModel(int modelWidth, Vector3 offset){
        width = modelWidth;
        this.offset = offset;
        solid = new boolean[modelWidth][modelWidth][modelWidth];
        setSolid();
    }

    private void setSolid(){
        for(int i = 0; i < width; i++){
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < width; k++) {
                    solid[i][j][k] = true;
                }
            }
        }
    }


    public boolean isSolid(int x, int y, int z){
        if(x < 0 || x >= width) return false;
        if(y < 0 || y >= width) return false;
        if(z < 0 || z >= width) return false;
        return solid[x][y][z];
    }

    public void setColour(Color colour) { this.colour = colour; }
}
