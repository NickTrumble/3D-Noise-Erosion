package org.noiseErosion.lib;

import org.noiseErosion.Noise;
import org.noiseErosion.SolidModel;
import org.noiseErosion.Vector3;

//comprised off multiple solid models
public class World {
    public SolidModel[][][] chunks;
    private final int worldWidth;
    private final int worldHeight;
    private final int worldDepth;
    private final int chunkWidth;
    private final int seed;

    private final int modelWidth;
    private final float noiseScale;
    private final float noiseThreshold;

    public World(Vector3 worldSize, int voxelsPerChunk, int mWidth){
        this(worldSize, voxelsPerChunk, mWidth, (int)(Math.random() * 1000));
    }

    public World(Vector3 worldSize, int voxelsPerChunk, int mWidth, int seed){
        this(worldSize, voxelsPerChunk, mWidth, seed, 0.03f, 0.5f);
    }

    public World(Vector3 worldSize, int voxelsPerChunk, int mWidth, int seed, float noiseScale, float noiseThreshold){
        worldWidth = (int) worldSize.x;
        worldHeight = (int) worldSize.y;
        worldDepth = (int) worldSize.z;
        chunkWidth = voxelsPerChunk;
        chunks = new SolidModel[worldWidth][worldHeight][worldDepth];
        this.seed = seed;
        modelWidth = mWidth;
        this.noiseScale = noiseScale;
        this.noiseThreshold = noiseThreshold;
    }


    public void generateWorld(){
        for (int i = 0; i < worldWidth; i++) {
            for (int j = 0; j < worldHeight; j++) {
                for (int k = 0; k < worldDepth; k++) {
                    SolidModel sm = new SolidModel(modelWidth, new Vector3(i * modelWidth, j * modelWidth, k * modelWidth), chunkWidth);
                    chunks[i][j][k] = sm;
                    chunks[i][j][k].loadSolidState(Noise.apply(sm, noiseScale, noiseThreshold, new Vector3(i, j, k), seed));
                }
            }
        }
    }

    public int getWorldWidth() { return worldWidth;}
    public int getWorldHeight() { return worldHeight;}
    public int getWorldDepth() { return worldDepth;}
    public int getChunkWidth() { return chunkWidth;}
    public int getModelWidth() { return modelWidth;}
    public int getSeed() { return seed;}
    public float getNoiseScale() { return noiseScale;}
    public float getNoiseThreshold() { return noiseThreshold;}
    public float getVoxelSize() { return (float) modelWidth / chunkWidth; }
}
