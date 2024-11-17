package com.example.onepix;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the first FXML file (canvasSize.fxml)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/onepix/canvasSize.fxml"));
        Parent root = loader.load();

        CanvasSizeController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        // Set up the scene
        Scene scene = new Scene(root,1920,1080);

        // Set the scene on the stage and show the primary stage
        primaryStage.setScene(scene);
        primaryStage.setTitle("One-Pix");
        CanvasSizeController canvasSizeController = new CanvasSizeController();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
