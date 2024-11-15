package controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.User;
import utils.AlertUtils;
import utils.NavigationManager;
import utils.Logger;

public class RegisterController {
    public TextField nameField;
    public TextField phoneField;
    public TextField emailField;
    public PasswordField passwordField;
    public PasswordField confirmPasswordField;

    private String serverIp;
    private int serverPort;

    public void setServerInfo(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void handleRegisterAction(ActionEvent actionEvent) {
        // Trimmed inputs to avoid whitespace-only entries
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        // Log each field to ensure they're correctly populated
        Logger.info("Registering with details: Name = " + name + ", Phone = " + phone + ", Email = " + email);

        // Validate fields
        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AlertUtils.showError("Invalid fields!", "Please fill all fields!");
            return;
        }

        if (!email.contains("@")) {
            AlertUtils.showError("Invalid email!", "Please enter a valid email address!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            AlertUtils.showError("Passwords do not match!", "Please make sure both password fields are equal!");
            return;
        }

        // Send data to server in a new thread to avoid blocking UI
        new Thread(() -> {
            User user = new User(name, email, phone, password);
            ClientService clientService = new ClientService(serverIp, serverPort);

            // Register user and get response
            String response = clientService.registerUser(user);

            // Parse the JSON response
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            String responseData = jsonResponse.get("data").getAsString();

            // Run UI updates on the JavaFX Application thread
            javafx.application.Platform.runLater(() -> {
                if ("SUCCESS".equals(responseData)) {
                    AlertUtils.showSuccess("Registration Successful", "You can now log in with your email and password");
                    NavigationManager.switchScene(nameField, "login.fxml"); // Navigate to login page
                } else {
                    // Only show error dialog if there was an actual error message
                    AlertUtils.showError("Registration Failed", responseData);
                }
            });
        }).start();
    }

    public void handleBackAction(ActionEvent actionEvent) {
        NavigationManager.switchScene(nameField, "home.fxml");
    }
}