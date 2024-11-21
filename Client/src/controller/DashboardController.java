package controller;

import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.text.Text;
import model.Message;
import model.User;
import utils.Logger;
import utils.NavigationManager;
import utils.Routes;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    public ClientService clientService;
    public Text userText;

    private User currentUser;

    public void handleEditProfile() {
        NavigationManager.switchScene(Routes.EDIT_PROFILE);
    }

    public void handleLogout() {
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
        NavigationManager.switchScene(Routes.CREATE_GROUP);
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
        this.currentUser = clientService.getCurrentUser();
        userText.setText(currentUser.getName());
    }
}
