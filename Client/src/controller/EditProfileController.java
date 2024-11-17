package controller;

import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.Message;
import model.User;
import utils.AlertUtils;
import utils.Logger;
import utils.NavigationManager;
import utils.Routes;

public class EditProfileController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField passwordField;

    private String serverIp;
    private int serverPort;

    /**
     * Sets the server information for updating the profile.
     *
     * @param serverIp   the server's IP address
     * @param serverPort the server's port
     */
    public void setServerInfo(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    /**
     * Populates the fields with existing user data (this could be passed from the previous screen).
     *
     * @param name     the user's name
     * @param email    the user's email
     * @param phone    the user's phone number
     * @param password the user's password
     */
    public void populateFields(String name, String email, String phone, String password) {
        nameField.setText(name);
        emailField.setText(email);
        phoneField.setText(phone);
        passwordField.setText(password);
    }

    /**
     * Handles the save changes action.
     */
    public void handleSaveChangesAction(ActionEvent actionEvent) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            AlertUtils.showError("Invalid Fields", "Please fill in all fields.");
            return;
        }

        // Send updated profile to server
        new Thread(() -> {
            ClientService clientService = new ClientService(serverIp, serverPort);
            String response = clientService.sendRequest(new Message(Message.Type.EDIT_PROFILE, new User(name, email, phone, password)));

            javafx.application.Platform.runLater(() -> {
                if (response.contains("SUCCESS")) {
                    Logger.info("Profile updated successfully.");
                    AlertUtils.showSuccess("Success", "Your profile has been updated.");
                    NavigationManager.switchScene(Routes.DASHBOARD);
                } else {
                    Logger.error("Failed to update profile: " + response);
                    AlertUtils.showError("Error", "Failed to update profile. Try again later.");
                }
            });
        }).start();
    }

    /**
     * Handles the cancel action.
     */
    public void handleCancelAction(ActionEvent actionEvent) {
        NavigationManager.switchScene(Routes.DASHBOARD);
    }
}
