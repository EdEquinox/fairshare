package controller;

import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import utils.AlertUtils;
import utils.NavigationManager;

public class RegisterController {
    public TextField nameField;
    public TextField phoneField;
    public TextField emailField;
    public PasswordField passwordField;
    public PasswordField confirmPasswordField;

    public void handleRegisterAction(ActionEvent actionEvent) {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if(!password.equals(confirmPassword)) {
            AlertUtils.showError("Passwords do not match!", "Please make sure both password fields are equal!");
            return;
        }

        if (!email.contains("@")) {
            AlertUtils.showError("Invalid email!", "Please enter a valid email address!");
            return;
        }

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AlertUtils.showError("Invalid fields!", "Please fill all fields!");
            return;
        }

        // Send data to server



        System.out.println("User Registered: " + name + ", " + email);

        AlertUtils.showSuccess("Registration Successful", "You can now log in with your email and password");

    }

    public void handleBackAction(ActionEvent actionEvent) {
        NavigationManager.switchScene(nameField, "home.fxml");
    }
}
