package controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import utils.AlertUtils;
import utils.NavigationManager;
import utils.Logger;

public class LoginController {

    public TextField emailField;
    public PasswordField passwordField;

    private String serverIp;
    private int serverPort;

    /**
     * Sets the server information for the login process.
     *
     * @param serverIp   the server's IP address
     * @param serverPort the server's port
     */
    public void setServerInfo(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    /**
     * Handles the login action when the user clicks the login button.
     *
     * @param actionEvent the action event triggered by the button click
     */
    public void handleLoginAction(ActionEvent actionEvent) {
        // Validate inputs
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (!validateInputs(email, password)) {
            return;
        }

        // Send login request in a new thread
        new Thread(() -> processLogin(email, password)).start();
    }

    /**
     * Validates the email and password fields.
     *
     * @param email    the email entered by the user
     * @param password the password entered by the user
     * @return true if inputs are valid, false otherwise
     */
    private boolean validateInputs(String email, String password) {
        Logger.info("Validating login inputs for email: " + email);

        if (email.isEmpty() || password.isEmpty()) {
            AlertUtils.showError("Invalid Fields", "Please fill in all fields!");
            return false;
        }

        if (!email.contains("@")) {
            AlertUtils.showError("Invalid Email", "Please enter a valid email address!");
            return false;
        }

        return true;
    }

    /**
     * Processes the login request by communicating with the server.
     *
     * @param email    the email entered by the user
     * @param password the password entered by the user
     */
    private void processLogin(String email, String password) {
        ClientService clientService = new ClientService(serverIp, serverPort);
        Logger.info("Sending login request to the server for email: " + email);

        String response = clientService.loginUser(email, password);
        handleServerResponse(response);
    }

    /**
     * Handles the server response for the login request.
     *
     * @param response the server's JSON response as a string
     */
    private void handleServerResponse(String response) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            String responseData = jsonResponse.get("data").getAsString();

            javafx.application.Platform.runLater(() -> {
                if ("SUCCESS".equals(responseData)) {
                    Logger.info("Login successful for user.");
                    AlertUtils.showSuccess("Login Successful", "Welcome back!");
                    NavigationManager.switchScene(emailField, "dashboard.fxml");
                } else {
                    Logger.error("Login failed: " + responseData);
                    AlertUtils.showError("Login Failed", responseData);
                }
            });
        } catch (Exception e) {
            Logger.error("Error processing server response: " + e.getMessage());
            javafx.application.Platform.runLater(() ->
                    AlertUtils.showError("Login Error", "Unexpected server response.")
            );
        }
    }

    /**
     * Handles the back action when the user clicks the back button.
     *
     * @param actionEvent the action event triggered by the button click
     */
    public void handleBackAction(ActionEvent actionEvent) {
        NavigationManager.switchScene(emailField, "home.fxml");
    }
}
