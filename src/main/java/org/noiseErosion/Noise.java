package org.noiseErosion;

import org.noiseErosion.lib.OpenSimplex;

public class Noise {
    public static boolean[][][] apply(SolidModel model, float scale, float threshold) {
        int w = model.units;
        boolean[][][] state = new boolean[w][w][w];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < w; j++) {
                for (int k = 0; k < w; k++) {
                    double nx = i * scale;
                    double ny = j * scale;
                    double nz = k * scale;

                    float noise = OpenSimplex.noise3_ImproveXZ(0L, nx, ny, nz);

                    // vertical bias — carves more from the top
                    float verticalBias = (float) j / w;

                    state[i][j][k] = noise + verticalBias > threshold;
                }
            }
        }
        return state;
    }
}