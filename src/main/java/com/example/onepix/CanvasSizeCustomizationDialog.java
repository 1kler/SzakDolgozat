package com.example.onepix;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CanvasSizeCustomizationDialog extends Stage {
    private int width = 800;
    private int height = 600;

    public CanvasSizeCustomizationDialog(Stage primaryStage) {
        initModality(Modality.APPLICATION_MODAL);
        initOwner(primaryStage);
        setTitle("Set Canvas Size");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        Label widthLabel = new Label("Width:");
        TextField widthField = new TextField(Integer.toString(width));
        Label heightLabel = new Label("Height:");
        TextField heightField = new TextField(Integer.toString(height));

        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> {
            width = Integer.parseInt(widthField.getText());
            height = Integer.parseInt(heightField.getText());
            close();
        });

        grid.add(widthLabel, 0, 0);
        grid.add(widthField, 1, 0);
        grid.add(heightLabel, 0, 1);
        grid.add(heightField, 1, 1);
        grid.add(applyButton, 1, 2);

        Scene scene = new Scene(grid);
        setScene(scene);
    }

    public int getCanvasWidth() {
        return width;
    }

    public int getCanvasHeight() {
        return height;
    }
}
