package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationManager {

    public static void switchScene(Node node, String fxmlFile) {
        try {
            // Corrige o caminho para carregar o FXML de resources/layouts
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource("/resources/layouts/" + fxmlFile));
            Parent root = loader.load();
            Stage stage = (Stage) node.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading scene: " + fxmlFile);
        }
    }
}