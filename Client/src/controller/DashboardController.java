package controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import communication.ClientService;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import model.Group;
import model.Message;
import model.ServerResponse;
import model.User;
import utils.AlertUtils;
import utils.Logger;
import utils.NavigationManager;
import utils.Routes;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    public ClientService clientService;
    Gson gson = new Gson();
    public ArrayList<Group> groups;

    public Text userText;

    private User currentUser;

    public ListView<Group> groupList;

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
        NavigationManager.switchScene(Routes.INVITE);
    }

    public void handleGroupInfo(ActionEvent actionEvent) {
        // TODO: Handle group info logic here
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.clientService = ClientService.getInstance();
        this.currentUser = clientService.getCurrentUser();
        userText.setText(currentUser.getName());

        ServerResponse responseG = clientService.sendRequest(new Message(Message.Type.GET_GROUPS, clientService.getCurrentUser()));

        if (responseG == null) {
            AlertUtils.showError("Failed to get groups", "Failed to get groups from the server.");
            return;
        }

        Type groupListType = new TypeToken<ArrayList<Group>>() {}.getType();
        groups = gson.fromJson(gson.toJson(responseG.payload()), groupListType);

        groupList.getItems().addAll(groups);
    }
}
