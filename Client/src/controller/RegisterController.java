package controller;

import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.Message;
import model.ServerResponse;
import model.User;
import utils.AlertUtils;
import utils.Logger;
import utils.NavigationManager;
import utils.Routes;

import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    public TextField nameField;
    public TextField phoneField;
    public TextField emailField;
    public PasswordField passwordField;
    public PasswordField confirmPasswordField;

    private ClientService clientService;

    public void handleRegisterAction(ActionEvent actionEvent) {
        // Trimmed inputs to avoid whitespace-only entries
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        Logger.debug("Registering with details: Name = " + name + ", Phone = " + phone + ", Email = " + email);

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

            String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());
            User user = new User(name, email, phone, encodedPassword);

            // Register user and get response
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.REGISTER, user));

            // Parse the JSON response
            // Run UI updates on the JavaFX Application thread
            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    AlertUtils.showSuccess("Registration Successful", "You can now log in with your email and password");
                    NavigationManager.switchScene(Routes.LOGIN); // Navigate to login page
                } else {
                    // Only show error dialog if there was an actual error message
                    Logger.error(response.message());
                    AlertUtils.showError("Registration Failed", response.message());
                }
            });
        }).start();
    }

    public void handleBackAction(ActionEvent actionEvent) {
        NavigationManager.switchScene(Routes.HOME);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.clientService = ClientService.getInstance();
    }
}