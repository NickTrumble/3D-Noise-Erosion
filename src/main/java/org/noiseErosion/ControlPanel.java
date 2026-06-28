package org.noiseErosion;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ControlPanel {
    private static final double WIDTH = 180;

    private final VBox root;

    public ControlPanel(){
        root = new VBox(8);
        root.setPrefWidth(WIDTH);
        root.setMinWidth(WIDTH);
        root.setMaxWidth(WIDTH);
        root.setStyle("""
                -fx-background-color: #20242a;
                -fx-padding: 12;
                """);
    }

    public Button addButton(String label){
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(button, Priority.NEVER);
        root.getChildren().add(button);
        return button;
    }

    public ToggleButton addToggle(String label){
        ToggleButton button = new ToggleButton(label + ": false");
        button.setSelected(false);
        button.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(button, Priority.NEVER);
        root.getChildren().add(button);
        addValueSwapper(button, label);
        return button;
    }

    public Slider addIntSlider(String label, int min, int max, int value){
        Label valueLabel = new Label(label + ": " + value);
        valueLabel.setStyle("-fx-text-fill: white;");

        Slider slider = new Slider(min, max, value);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setBlockIncrement(1);
        slider.setSnapToTicks(true);
        slider.setShowTickMarks(true);
        slider.setMaxWidth(Double.MAX_VALUE);

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            valueLabel.setText(label + ": " + Math.round(newValue.floatValue()));
        });

        VBox sliderGroup = new VBox(4, valueLabel, slider);
        VBox.setVgrow(sliderGroup, Priority.NEVER);
        root.getChildren().add(sliderGroup);

        return slider;
    }

    public Slider addFloatSlider(String label, double min, double max, double value){
        Label valueLabel = new Label(label + ": " + formatFloat(value));
        valueLabel.setStyle("-fx-text-fill: white;");

        Slider slider = new Slider(min, max, value);
        slider.setMaxWidth(Double.MAX_VALUE);

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            valueLabel.setText(label + ": " + formatFloat(newValue.doubleValue()));
        });

        VBox sliderGroup = new VBox(4, valueLabel, slider);
        VBox.setVgrow(sliderGroup, Priority.NEVER);
        root.getChildren().add(sliderGroup);

        return slider;
    }

    public ComboBox<String> addComboBox(String label, String[] options, String value){
        Label valueLabel = new Label(label);
        valueLabel.setStyle("-fx-text-fill: white;");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(options);
        comboBox.setValue(value);
        comboBox.setMaxWidth(Double.MAX_VALUE);

        VBox comboGroup = new VBox(4, valueLabel, comboBox);
        VBox.setVgrow(comboGroup, Priority.NEVER);
        root.getChildren().add(comboGroup);

        return comboBox;
    }

    private String formatFloat(double value){
        return String.format("%.2f", value);
    }

    public void addValueSwapper(ToggleButton button, String label){
        button.selectedProperty().addListener((observable, oldValue, newValue) -> {
            button.setText(label + ": " + newValue);
        });
    }

    public Node getRoot(){
        return root;
    }
}
