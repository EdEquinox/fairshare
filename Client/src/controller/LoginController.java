package controller;

import com.google.gson.Gson;
import communication.ClientService;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.Message;
import model.ServerResponse;
import model.User;
import utils.*;

import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    public TextField emailField;
    public PasswordField passwordField;

    private ClientService clientService;

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
        Logger.debug("Sending login request to the server for email: " + email);

        // Encrypt password:
        Base64.Encoder encoder = Base64.getEncoder();
        String encryptedPassword = encoder.encodeToString(password.getBytes());

        ServerResponse response = clientService.sendRequest(new Message(Message.Type.LOGIN, new User(null, email, null, encryptedPassword)));
        try {
            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    Logger.info("Login successful for user.");
                    User user = new Gson().fromJson(response.payload().toString(), User.class);
                    clientService.setCurrentUser(user);
                    // Guarda o utilizador que fez login em sharedstate
                    SharedState.setCurrentUser(user);
                    NavigationManager.switchScene(Routes.DASHBOARD);
                } else {
                    Logger.error("Login failed: " + response.message());
                    AlertUtils.showError("Login Failed", response.message());
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
     */
    public void handleBackAction() {
        NavigationManager.switchScene(Routes.HOME);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.clientService = ClientService.getInstance();
    }
}
