package controller;

import communication.ClientService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
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

public class EditProfileController implements Initializable {

    public Label emailLabel;
    private ClientService clientService;
    private User currentUser;

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField passwordField;

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
        emailLabel.setText("Email: " + email);
        phoneField.setText(phone);
        passwordField.setText(password);
    }

    /**
     * Handles the save changes action.
     */
    public void handleSaveChangesAction() {
        String name = nameField.getText().trim();
        String email = currentUser.getEmail();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            AlertUtils.showError("Invalid Fields", "Please fill in all fields.");
            return;
        }

        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());

        // Send updated profile to server
        new Thread(() -> {
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.EDIT_PROFILE, new User(currentUser.getId(), name, email, phone, encodedPassword)));

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    Logger.info("Profile updated successfully.");
                    AlertUtils.showSuccess("Success", "Your profile has been updated.");
                    NavigationManager.switchScene(Routes.DASHBOARD);
                } else {
                    Logger.error("Failed to update profile: " + response);
                    AlertUtils.showError("Error", response.message());
                }
            });
        }).start();
    }

    /**
     * Handles the cancel action.
     */
    public void handleCancelAction() {
        NavigationManager.switchScene(Routes.DASHBOARD);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.clientService = ClientService.getInstance();
        currentUser = clientService.getCurrentUser();
        populateFields(currentUser.getName(), currentUser.getEmail(), currentUser.getPhone(), currentUser.getPassword());
    }
}
