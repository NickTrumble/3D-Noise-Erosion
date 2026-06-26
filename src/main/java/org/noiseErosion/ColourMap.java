package org.noiseErosion;

import javafx.scene.paint.Color;

public class ColourMap {
    public static Color[] rainbowCMAP = new Color[] {
      Color.ORANGE, Color.YELLOW, Color.LIME, Color.TURQUOISE, Color.BLUE
    };

    //-units / 2 to units / 2
    public static Color getColour(float j, float maxHeight, Color[] cmap){

        float t = (j) / maxHeight;
        float spacing = 1f / (cmap.length - 1);

        int i0 = (int) (t / spacing);
        float rem = (t - i0 * spacing) / spacing;

        int i1 = Math.min(i0 + 1, cmap.length - 1);
//        if (j <=-1.0) {
//            System.out.println("how on earth does this work: " + j);
//            System.out.println("i0:" + i0 + ", i1:" + i1);
//            System.out.println("t/spacing: " + t / spacing);
//            System.out.println("int^^: " + (int)(t / spacing));
//        }
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