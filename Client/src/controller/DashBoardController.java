package controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.AlertUtils;
import utils.Logger;
import utils.NavigationManager;

import java.io.IOException;

public class DashBoardController {

    private String serverIp;
    private int serverPort;

    // Method to set the server IP and port, called during navigation
    public void setServerInfo(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        Logger.info("DashBoardController: Server info set to " + serverIp + ":" + serverPort);
    }

    public void handleEditProfile(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/layouts/editProfile.fxml"));
            Scene scene = new Scene(loader.load());

            // Get the EditProfileController and set server info
            EditProfileController controller = loader.getController();
            controller.setServerInfo(serverIp, serverPort);

            // Fetch current user data
            new Thread(() -> {
                ClientService clientService = new ClientService(serverIp, serverPort);
                String response = clientService.getUserProfile("current_user_email@example.com"); // Replace with actual email from session or context

                javafx.application.Platform.runLater(() -> {
                    try {
                        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                        if (jsonResponse.get("data").getAsString().startsWith("Error")) {
                            Logger.error("Failed to fetch profile: " + jsonResponse.get("data").getAsString());
                            AlertUtils.showError("Error", "Failed to fetch profile. Try again later.");
                        } else {
                            JsonObject profileData = JsonParser.parseString(jsonResponse.get("data").getAsString()).getAsJsonObject();
                            controller.populateFields(
                                    profileData.get("name").getAsString(),
                                    profileData.get("email").getAsString(),
                                    profileData.get("phone").getAsString(),
                                    profileData.get("password").getAsString()
                            );
                        }
                    } catch (Exception e) {
                        Logger.error("Error processing profile response: " + e.getMessage());
                        AlertUtils.showError("Error", "Failed to fetch profile.");
                    }
                });
            }).start();

            // Switch the scene
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Edit Profile");
        } catch (IOException e) {
            Logger.error("Error loading Edit Profile screen: " + e.getMessage());
        }
    }



    public void handleLogout(ActionEvent actionEvent) {
        new Thread(() -> {
            ClientService clientService = new ClientService(serverIp, serverPort);
            if (clientService.connectToServer()) {
                clientService.sendMessage("LOGOUT");
                clientService.closeConnection();
            }

            javafx.application.Platform.runLater(() -> {
                Logger.info("User logged out. Redirecting to home page.");
                NavigationManager.switchScene((Node) actionEvent.getSource(), "home.fxml");
            });
        }).start();

    }

    public void handleAddNewGroup(ActionEvent actionEvent) {
        // Add new group logic here
    }

    public void handleSelectGroup(ActionEvent actionEvent) {
        // Select group logic here
    }

    public void handleGroupInvites(ActionEvent actionEvent) {
        // Handle group invites logic here
    }

    public void handleGroupInfo(ActionEvent actionEvent) {
        // Handle group info logic here
    }
}
