package utils;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class NavigationManager {

    private static Stage primaryStage;

    // Initialize the primary stage
    public static void initialize(Stage stage) {
        if (primaryStage == null) {
            primaryStage = stage;
            primaryStage.setMinWidth(800); // Default minimum width
            primaryStage.setMinHeight(600); // Default minimum height
        } else {
            throw new IllegalStateException("Primary Stage was initialized already.");
        }
    }

    // Switch scenes and return the controller of the new scene
    public static void switchScene(Routes route) {
        try {
            String layout = route.toString().toLowerCase();
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource("/resources/layouts/" + layout + ".fxml"));

            // Load the scene
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            primaryStage.setTitle(route.toString().toUpperCase() + " Page");

            // Center the window without resizing
            primaryStage.centerOnScreen();

        } catch (IOException e) {
            Logger.error("Error while loading scene: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
