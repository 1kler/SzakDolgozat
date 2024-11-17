package com.example.onepix;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;

public class CanvasSizeController {

    private Stage primaryStage;

    @FXML
    private TextArea height;

    @FXML
    private TextArea width;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void handleStart() {
        String heightValue = height.getText();
        String widthValue = width.getText();

        try {
            // Load the second FXML file (canvaspane.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/onepix/canvasPane.fxml"));
            Parent root = loader.load();

            // Get the controller of the second FXML file
            CanvasPane canvasPaneController = loader.getController();
            // Pass the values to the controller of the second FXML file
            canvasPaneController.setCanvasSize(Double.parseDouble(heightValue), Double.parseDouble(widthValue));

            // Create a new stage for the second FXML file
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("One-Pix");
            stage.show();

            primaryStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}
