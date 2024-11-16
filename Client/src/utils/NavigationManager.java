package utils;

import communication.ClientService;
import communication.ClientServiceAware;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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
    public static void switchScene(String layout) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource("/resources/layouts/" + layout + ".fxml"));

            // Set the ControllerFactory to inject the ClientService
            loader.setControllerFactory(controllerClass -> {
                try {
                    // Create a new instance of the controller
                    Object controller = controllerClass.getDeclaredConstructor().newInstance();

                    // If the controller implements ClientServiceAware, inject the ClientService
                    if (controller instanceof ClientServiceAware)
                        ((ClientServiceAware) controller).setClientService(ClientService.getInstance());

                    return controller;
                } catch (Exception e) {
                    // Handle any exception when instantiating the controller
                    throw new RuntimeException("Error while creating controller: " + e.getMessage(), e);
                }
            });

            // Create and set the scene
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            // Catch IO errors while loading FXML
            e.printStackTrace();
            throw new RuntimeException("Error while loading scene: " + e.getMessage(), e);
        }
    }
}