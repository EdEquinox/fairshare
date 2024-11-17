package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationManager {

    private static Stage primaryStage;

    public static void initialize(Stage stage) {
        if (primaryStage == null) {
            primaryStage = stage;
        } else {
            throw new IllegalStateException("Primary Stage was initialized already.");
        }
    }

    // Method to switch scenes and inject ClientService into controllers
    public static void switchScene(Routes route) {
        try {
            String layout = route.toString().toLowerCase();
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource("/resources/layouts/" + layout + ".fxml"));

            // Create and set the scene
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            primaryStage.setTitle(route.toString().toUpperCase() + " Page");
        } catch (IOException e) {
            // Catch IO errors while loading FXML
            e.printStackTrace();
            throw new RuntimeException("Error while loading scene: " + e.getMessage(), e);
        }
    }
}