package controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import utils.AlertUtils;
import utils.Logger;
import controller.RegisterController;
import utils.NavigationManager;

import java.io.IOException;

public class HomeController {

    private String serverIp;
    private int serverPort;

    // Method to set the server IP and port, called from the main application
    public void setServerInfo(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        Logger.info("HomeController: Server info set to " + serverIp + ":" + serverPort);
    }

    @FXML
    public void handleLoginAction(ActionEvent actionEvent) {
        // Navigate to the login screen and pass server info
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/layouts/login.fxml"));
            Scene scene = new Scene(loader.load());

            // Get the LoginController from the loader and set the server info
            LoginController loginController = loader.getController();
            loginController.setServerInfo(serverIp, serverPort);

            // Set the new scene on the current stage
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login Page");
        } catch (IOException e) {
            Logger.error("Error navigating to login screen: " + e.getMessage());
        }
    }




    @FXML
    public void handleRegisterAction(ActionEvent actionEvent) {
        // Navigate to the registration screen and pass server info
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/layouts/register.fxml"));
            Scene scene = new Scene(loader.load());

            // Get the RegisterController from the loader and set the server info
            RegisterController registerController = loader.getController();
            registerController.setServerInfo(serverIp, serverPort);

            // Set the new scene on the current stage
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Register Page");
        } catch (IOException e) {
            Logger.error("Error navigating to register screen: " + e.getMessage());
        }
    }
}
