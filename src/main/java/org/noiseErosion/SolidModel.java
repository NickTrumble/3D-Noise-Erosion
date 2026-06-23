package org.noiseErosion;

import javafx.scene.paint.Color;

public class SolidModel {
    private final boolean[][][] solid;
    public int width;
    public Vector3 offset;
    public float voxelSize; //to add
    public int units;
    public Color colour;

    public Vector3[] cachedVerts;
    public int[][] cachedTris;
    public boolean[] cachedTrisToRender;
    public boolean dirty = true;

    public SolidModel(int modelWidth, Vector3 offset, int unitWidth){
        width = modelWidth;
        units = unitWidth;
        voxelSize = (float) width / unitWidth;
        this.offset = offset;
        solid = new boolean[unitWidth][unitWidth][unitWidth];
        setSolid();
    }

    private void setSolid(){
        for(int i = 0; i < units; i++){
            for (int j = 0; j < units; j++) {
                for (int k = 0; k < units; k++) {
                    solid[i][j][k] = true;
                }
            }
        }
    }
    
    public void loadSolidState(boolean[][][] state){
        for (int i = 0; i < units; i++) {
            for (int j = 0; j < units; j++) {
                for (int k = 0; k < units; k++) {
                    setSolid(state[i][j][k], i, j, k);
                }
            }
        }
    }

    public boolean isSolid(int x, int y, int z){
        if(x < 0 || x >= units) return false;
        if(y < 0 || y >= units) return false;
        if(z < 0 || z >= units) return false;
        return solid[x][y][z];
    }

    public void setSolid(boolean state, int i, int j, int k){
        solid[i][j][k] = state;
        dirty = true;
    }

    public void setColour(Color colour) { this.colour = colour; }
}
