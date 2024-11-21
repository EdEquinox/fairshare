package controller;

import communication.ClientService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import model.Group;
import utils.AlertUtils;
import utils.NavigationManager;
import utils.Routes;

public class InviteController {

    public Group[] groups;
    public Group[] pendingInvites;

    ClientService clientService;

    @FXML
    public ComboBox<Group> groupComboBox;

    @FXML
    public ListView<Group> inviteList;

    @FXML
    TextField inviteeEmailField;

    public InviteController() {


        clientService = ClientService.getInstance();
        if (!clientService.connectToServer()) {
            AlertUtils.showError("Connection failed", "Failed to connect to the server.");
            return;
        }

        String message = clientService.getGroups();

        if (message == null) {
            AlertUtils.showError("Failed to get groups", "Failed to get groups from the server.");
            return;
        }

        String[] groupNames = message.split(",");
        groups = new Group[groupNames.length];
        for (int i = 0; i < groupNames.length; i++) {
            groups[i] = new Group(i, groupNames[i]);
        }

        groupComboBox.getItems().addAll(groups);

        String pendingInvitesMessage = clientService.getPendingInvites(clientService.getUserId());

        if (pendingInvitesMessage == null) {
            AlertUtils.showError("Failed to get pending invites", "Failed to get pending invites from the server.");
            return;
        }

        String[] pendingInviteNames = pendingInvitesMessage.split(",");
        pendingInvites = new Group[pendingInviteNames.length];
        for (int i = 0; i < pendingInviteNames.length; i++) {
            pendingInvites[i] = new Group(i, pendingInviteNames[i]);
        }

        inviteList.getItems().addAll(pendingInvites);

    }

    public void handleInviteButtonAction() {
        Group selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            AlertUtils.showError("No group selected", "Please select a group.");
            return;
        }

        if (inviteeEmailField.getText().isEmpty()) {
            AlertUtils.showError("No email entered", "Please enter an email address.");
            return;
        }

        new Thread(() -> {

            ClientService clientService = ClientService.getInstance();
            if (!clientService.connectToServer()) {
                AlertUtils.showError("Connection failed", "Failed to connect to the server.");
                return;
            }

            String inviteeEmail = inviteeEmailField.getText();
            int groupId = selectedGroup.getId();

            String response = clientService.sendInvite(inviteeEmail, groupId);

            if (response == null) {
                AlertUtils.showError("Failed to send invite", "Failed to send invite to the server.");
                return;
            }
            AlertUtils.showSuccess("Invite sent", "Invite sent successfully.");

        }).start();

    }

    public void handleBackButtonAction() {
        NavigationManager.switchScene(Routes.HOME);
    }
}
