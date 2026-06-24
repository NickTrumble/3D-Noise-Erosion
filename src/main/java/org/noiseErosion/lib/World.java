package org.noiseErosion.lib;

import javafx.scene.paint.Color;
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

    private final int modelWidth = 4;

    public World(int ChunksWide, int ChunksTall, int ChunksDeep, int voxelsPerChunk){
        worldWidth = ChunksWide;
        worldHeight = ChunksTall;
        worldDepth = ChunksDeep;
        chunkWidth = voxelsPerChunk;
        chunks = new SolidModel[worldWidth][worldHeight][worldDepth];
        seed = (int)(Math.random() * 1000);
    }


    public void generateWorld(){
        for (int i = 0; i < worldWidth; i++) {
            for (int j = 0; j < worldHeight; j++) {
                for (int k = 0; k < worldDepth; k++) {
                    SolidModel sm = new SolidModel(modelWidth, new Vector3(i * modelWidth, j * modelWidth, k * modelWidth), chunkWidth);
                    chunks[i][j][k] = sm;
                    chunks[i][j][k].setColour(Color.GREEN);
                    chunks[i][j][k].loadSolidState(Noise.apply(sm, 0.1f, 0.5f, new Vector3(i, j, k), seed));
                }
            }
        }
    }

    public int getWorldWidth() { return worldWidth;}
    public int getWorldHeight() { return worldHeight;}
    public int getWorldDepth() { return worldDepth;}
    public int getChunkWidth() { return chunkWidth;}
}
