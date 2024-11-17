package controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import model.Message;
import model.User;
import utils.AlertUtils;
import utils.Logger;
import utils.NavigationManager;
import utils.Routes;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    public ClientService clientService;

    private User currentUser;

    public void handleEditProfile(ActionEvent actionEvent) {
        // try {

        NavigationManager.switchScene(Routes.EDIT_PROFILE);

        // Fetch current user data
        new Thread(() -> {
            String response = clientService.getUserProfile("current_user_email@example.com"); // Replace with actual email from session or context

            javafx.application.Platform.runLater(() -> {
                try {
                    JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                    if (jsonResponse.get("data").getAsString().startsWith("Error")) {
                        Logger.error("Failed to fetch profile: " + jsonResponse.get("data").getAsString());
                        AlertUtils.showError("Error", "Failed to fetch profile. Try again later.");
                    } else {
                        JsonObject profileData = JsonParser.parseString(jsonResponse.get("data").getAsString()).getAsJsonObject();
                        // controller.populateFields(profileData.get("name").getAsString(), profileData.get("email").getAsString(), profileData.get("phone").getAsString(), profileData.get("password").getAsString());
                    }
                } catch (Exception e) {
                    Logger.error("Error processing profile response: " + e.getMessage());
                    AlertUtils.showError("Error", "Failed to fetch profile.");
                }
            });
        }).start();

        // } catch (IOException e) {
        // Logger.error("Error loading Edit Profile screen: " + e.getMessage());
        // }
    }


    public void handleLogout(ActionEvent actionEvent) {
        new Thread(() -> {
            if (clientService.isClientConnected()) {
                clientService.sendRequest(new Message(Message.Type.LOGOUT, currentUser));
                clientService.closeConnection();
            }

            javafx.application.Platform.runLater(() -> {
                Logger.info("User logged out. Redirecting to home page.");
                NavigationManager.switchScene(Routes.HOME);
            });
        }).start();

    }

    public void handleAddNewGroup(ActionEvent actionEvent) {
        // TODO: Add new group logic here
    }

    public void handleSelectGroup(ActionEvent actionEvent) {
        // TODO: Select group logic here
    }

    public void handleGroupInvites(ActionEvent actionEvent) {
        // TODO: Handle group invites logic here
    }

    public void handleGroupInfo(ActionEvent actionEvent) {
        // TODO: Handle group info logic here
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.clientService = ClientService.getInstance();
    }
}
