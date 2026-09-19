# 3D Noise Erosion

A JavaFX terrain experiment that generates a 3D landscape from OpenSimplex noise and renders it with an interactive camera, colour map, and control panel.

The project separates terrain generation (`Noise` and `Engine`), the solid terrain model, rendering, and UI controls. `noise4j` is used alongside the included noise code.

## Requirements and running

- JDK 21
- Maven

Run the application with:

```text
mvn javafx:run
```

The Maven configuration starts `org.noiseErosion.Main`.

## Notes

This is an exploratory visual project rather than a geological erosion model. The terrain shape comes from noise and the controls are intended for experimenting with its appearance.
