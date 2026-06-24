package org.noiseErosion;

import org.noiseErosion.lib.OpenSimplex;

public class Noise {
    public static boolean[][][] apply(SolidModel model, float scale, float threshold, Vector3 chunkOffset, int seed) {
        int w = model.units;
        boolean[][][] state = new boolean[w][w][w];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < w; j++) {
                for (int k = 0; k < w; k++) {
                    double nx = (chunkOffset.x * w + i) * scale;
                    double ny = (chunkOffset.y * w + j) * scale;
                    double nz = (chunkOffset.z * w + k) * scale;

                    float noise = OpenSimplex.noise3_ImproveXZ(seed, nx, ny, nz);

                    state[i][j][k] = noise > threshold;
                }
            }
        }
        return state;
    }

    public static boolean[][][] apply(SolidModel model, float scale, float threshold){
        return apply(model, scale, threshold, new Vector3(0, 0, 0), (int)(Math.random() * 1000));
    }
}