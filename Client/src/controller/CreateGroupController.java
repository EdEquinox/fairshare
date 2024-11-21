package controller;

import communication.ClientService;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import model.Group;
import model.Message;
import model.ServerResponse;
import model.User;
import utils.AlertUtils;
import utils.NavigationManager;
import utils.Routes;

import java.net.URL;
import java.util.ResourceBundle;

public class CreateGroupController implements Initializable {

    public TextField groupNameField;
    private ClientService clientService;
    private User currentUser;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.clientService = ClientService.getInstance();
        this.currentUser = clientService.getCurrentUser();

    }

    public void handleCreateGroup() {
        String groupName = groupNameField.getText().trim();

        if (groupName.isEmpty()) {
            AlertUtils.showError("Error", "Group name cannot be empty.");
            return;
        }

        ServerResponse response = clientService.sendRequest(new Message(Message.Type.CREATE_GROUP, new Group(groupName, this.currentUser.getId())));

        if (response.isSuccess()) {
            AlertUtils.showSuccess("Success", "Group " + groupName + " Created.");
            NavigationManager.switchScene(Routes.DASHBOARD);
        } else {
            AlertUtils.showError("Error", response.message());
        }

    }

    public void handleBackButton() {
        NavigationManager.switchScene(Routes.DASHBOARD);
    }
}
