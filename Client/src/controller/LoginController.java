package controller;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import communication.ClientService;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.Message;
import model.ServerResponse;
import model.User;
import utils.*;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    public TextField emailField;
    public PasswordField passwordField;

    private ClientService clientService;

    private static final Gson gson = new Gson();


    /**
     * Handles the login action when the user clicks the login button.
     */
    public void handleLoginAction() {
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
        Logger.debug("Initiating login process for email: " + email);

        // Validate input fields
        if (email == null || email.isBlank()) {
            AlertUtils.showError("Login Error", "Email cannot be empty.");
            return;
        }

        if (password == null || password.isBlank()) {
            AlertUtils.showError("Login Error", "Password cannot be empty.");
            return;
        }

        try {
            // Encrypt password
            String encryptedPassword = Base64.getEncoder().encodeToString(password.getBytes());
            Logger.debug("Password encrypted successfully.");

            // Create a login message
            Message loginMessage = new Message(Message.Type.LOGIN, new User(email, encryptedPassword));
            Logger.debug("Login message created: " + new Gson().toJson(loginMessage));

            // Send the request to the server
            ServerResponse response = clientService.sendRequest(loginMessage);

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    try {
                        Logger.info("Server response received: " + response.payload());

                        // Validate that payload contains a valid JSON object
                        String payloadJson = response.payload().toString();
                        if (payloadJson == null || payloadJson.isBlank()) {
                            throw new IllegalArgumentException("Payload is empty or null.");
                        }

                        // Deserialize JSON payload into a User object
                        User user = gson.fromJson(gson.toJson(response.payload()), User.class);
                        if (user == null) {
                            throw new IllegalStateException("Failed to parse user information from payload.");
                        }

                        // Save user information and navigate to the dashboard
                        clientService.setCurrentUser(user);
                        SharedState.setCurrentUser(user);
                        Logger.info("Login successful for user: " + user.getEmail());
                        NavigationManager.switchScene(Routes.DASHBOARD);
                    } catch (JsonSyntaxException e) {
                        Logger.error("Error parsing JSON payload: " + e.getMessage());
                        AlertUtils.showError("Login Error", "Failed to parse server response. Please contact support.");
                    } catch (Exception e) {
                        Logger.error("Unexpected error: " + e.getMessage());
                        AlertUtils.showError("Login Error", "An unexpected error occurred. Please try again.");
                    }
                } else {
                    Logger.error("Login failed: " + response.message());
                    AlertUtils.showError("Login Failed", response.message());
                }
            });

        } catch (Exception e) {
            Logger.error("Error during login process: " + e.getMessage());
            javafx.application.Platform.runLater(() ->
                    AlertUtils.showError("Login Error", "Failed to communicate with the server. Please try again later.")
            );
        }
    }

    /**
     * Handles the back action when the user clicks the back button.
     */
    public void handleBackAction() {
        NavigationManager.switchScene(Routes.HOME);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.clientService = ClientService.getInstance();
    }
}
