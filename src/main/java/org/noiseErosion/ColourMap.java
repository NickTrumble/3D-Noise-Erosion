package org.noiseErosion;

import javafx.scene.paint.Color;

public class ColourMap {
    public static final String RAINBOW = "Rainbow";
    public static final String CLOUD = "Cloud";
    public static final String TERRAIN = "Terrain";
    public static final String FIRE = "Fire";
    public static final String STONE = "Stone";
    public static final String VIRIDIS = "Viridis";

    public static Color[] rainbowCMAP = new Color[] {
      Color.ORANGE, Color.YELLOW, Color.LIME, Color.TURQUOISE, Color.BLUE
    };

    public static Color[] cloudCMAP = new Color[] {
            Color.rgb(52, 67, 79),
            Color.rgb(105, 130, 148),
            Color.rgb(188, 205, 216),
            Color.rgb(245, 248, 250)
    };

    public static Color[] terrainCMAP = new Color[] {
            Color.rgb(36, 80, 46),
            Color.rgb(91, 132, 65),
            Color.rgb(154, 142, 91),
            Color.rgb(128, 112, 96),
            Color.rgb(235, 235, 225)
    };

    public static Color[] fireCMAP = new Color[] {
            Color.rgb(40, 14, 24),
            Color.rgb(126, 32, 32),
            Color.rgb(217, 91, 36),
            Color.rgb(255, 188, 79),
            Color.rgb(255, 241, 184)
    };

    public static Color[] stoneCMAP = new Color[] {
            Color.rgb(45, 48, 52),
            Color.rgb(86, 88, 88),
            Color.rgb(132, 128, 119),
            Color.rgb(183, 178, 163),
            Color.rgb(232, 228, 211)
    };

    public static Color[] viridisCMAP = new Color[] {
            Color.rgb(68, 1, 84),
            Color.rgb(59, 82, 139),
            Color.rgb(33, 145, 140),
            Color.rgb(94, 201, 98),
            Color.rgb(253, 231, 37)
    };

    public static String[] getColourMapNames(){
        return new String[] { CLOUD, TERRAIN, FIRE, STONE, VIRIDIS, RAINBOW };
    }

    public static Color[] getColourMap(String name){
        return switch (name) {
            case RAINBOW -> rainbowCMAP;
            case TERRAIN -> terrainCMAP;
            case FIRE -> fireCMAP;
            case STONE -> stoneCMAP;
            case VIRIDIS -> viridisCMAP;
            default -> cloudCMAP;
        };
    }

    public static Color getColour(float j, float maxHeight, Color[] cmap){

        float t = Math.max(0f, Math.min(1f, j / maxHeight));
        float spacing = 1f / (cmap.length - 1);

        int i0 = (int) (t / spacing);
        float rem = (t - i0 * spacing) / spacing;

        int i1 = Math.min(i0 + 1, cmap.length - 1);
        return lerp(cmap[i0], cmap[i1], rem).darker();
    }

    private static Color lerp(Color a, Color b, float t) {
        return Color.color(
                a.getRed() + (b.getRed() - a.getRed()) * t,
                a.getGreen() + (b.getGreen() - a.getGreen()) * t,
                a.getBlue() + (b.getBlue() - a.getBlue()) * t
        );
    }
}
