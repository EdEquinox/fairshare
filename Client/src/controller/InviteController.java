package controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import communication.ClientService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import model.Group;
import model.Message;
import model.ServerResponse;
import utils.AlertUtils;
import utils.NavigationManager;
import utils.Routes;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class InviteController {

    public ArrayList<Group> groups;
    public ArrayList<Group> pendingInvites;
    Gson gson = new Gson();

    ClientService clientService;

    @FXML
    public ComboBox<Group> groupComboBox;

    @FXML
    public ListView<Group> inviteList;

    @FXML
    TextField emailField;

    @FXML
    public void initialize() {

        clientService = ClientService.getInstance();
        if (!clientService.connectToServer()) {
            AlertUtils.showError("Connection failed", "Failed to connect to the server.");
            return;
        }
       ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_GROUPS, clientService.getCurrentUser()));

       if (response == null) {
          AlertUtils.showError("Failed to get groups", "Failed to get groups from the server.");
          return;
       }

        Type groupListType = new TypeToken<ArrayList<Group>>() {}.getType();
        groups = gson.fromJson(gson.toJson(response.payload()), groupListType);

        groupComboBox.getItems().addAll(groups);
        groupComboBox.getSelectionModel().selectFirst();

        System.out.println(groupComboBox.getItems());

        ServerResponse response2 = clientService.sendRequest(new Message(Message.Type.GET_PENDING_INVITES, clientService.getCurrentUser()));

        if (response2 == null) {
            AlertUtils.showError("Failed to get invites", "Failed to get invites from the server.");
            return;
        }

        Type pendingInvitesType = new TypeToken<ArrayList<Group>>() {}.getType();
        pendingInvites = gson.fromJson(gson.toJson(response2.payload()), pendingInvitesType);

        inviteList.getItems().addAll(pendingInvites);

    }

    public void handleInviteButtonAction() {
        Group selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            AlertUtils.showError("No group selected", "Please select a group.");
            return;
        }

        if (emailField.getText().isEmpty()) {
            AlertUtils.showError("No email entered", "Please enter an email address.");
            return;
        }

        new Thread(() -> {

            ClientService clientService = ClientService.getInstance();
            if (!clientService.connectToServer()) {
                AlertUtils.showError("Connection failed", "Failed to connect to the server.");
                return;
            }

            String inviteeEmail = emailField.getText();

            ArrayList<Object> inviteData = new ArrayList<>();
            inviteData.add(selectedGroup);
            inviteData.add(clientService.getCurrentUser());
            inviteData.add(inviteeEmail);

            ServerResponse response = clientService.sendRequest(new Message(Message.Type.INVITE, inviteData));

            if (response == null) {
                AlertUtils.showError("Failed to send invite", "Failed to send invite to the server.");
                return;
            }
            AlertUtils.showSuccess("Invite sent", "Invite sent successfully.");

        }).start();

    }

    public void handleBackButtonAction() {
        NavigationManager.switchScene(Routes.DASHBOARD);
    }
}
