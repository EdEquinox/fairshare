package controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import communication.ClientService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import model.Group;
import model.Invite;
import model.Message;
import model.ServerResponse;
import utils.AlertUtils;
import utils.NavigationManager;
import utils.Routes;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class InviteController implements Initializable {

    private List<Group> groups;
    private List<Invite> pendingInvites;
    private final Gson gson = new Gson();

    private ClientService clientService;

    @FXML
    private ComboBox<Group> groupComboBox;

    @FXML
    private ListView<Invite> inviteList;

    @FXML
    private TextField emailField;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clientService = ClientService.getInstance();

        if (!clientService.connectToServer()) {
            AlertUtils.showError("Connection failed", "Failed to connect to the server.");
            return;
        }

        fetchGroups();
        fetchPendingInvites();
    }

    private void fetchGroups() {
        new Thread(() -> {
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_GROUPS, clientService.getCurrentUser()));

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    Type groupListType = new TypeToken<ArrayList<Group>>() {}.getType();
                    groups = gson.fromJson(gson.toJson(response.payload()), groupListType);

                    if (groups != null && !groups.isEmpty()) {
                        groupComboBox.getItems().addAll(groups);
                        groupComboBox.getSelectionModel().selectFirst();
                    } else {
                        AlertUtils.showError("No Groups", "You are not part of any groups.");
                    }
                } else {
                    AlertUtils.showError("Failed to get groups", response.message());
                }
            });
        }).start();
    }

    private void fetchPendingInvites() {
        new Thread(() -> {
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_INVITES, clientService.getCurrentUser()));

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    Type inviteListType = new TypeToken<ArrayList<Invite>>() {}.getType();
                    pendingInvites = gson.fromJson(gson.toJson(response.payload()), inviteListType);

                    if (pendingInvites != null && !pendingInvites.isEmpty()) {
                        inviteList.getItems().addAll(pendingInvites);
                        inviteList.setCellFactory(param -> new ListCell<>() {
                            @Override
                            protected void updateItem(Invite invite, boolean empty) {
                                super.updateItem(invite, empty);
                                if (empty || invite == null) {
                                    setText(null);
                                } else {
                                    setText(String.format("Group: %s | Sent by: %s", invite.getGroupName(), invite.getSenderEmail()));
                                }
                            }
                        });
                    } else {
                        AlertUtils.showError("No Invites", "You have no pending invites.");
                    }
                } else {
                    AlertUtils.showError("Failed to get invites", response.message());
                }
            });
        }).start();
    }

    public void handleSendInvite() {
        Group selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            AlertUtils.showError("No group selected", "Please select a group.");
            return;
        }

        String inviteeEmail = emailField.getText().trim();
        if (inviteeEmail.isEmpty()) {
            AlertUtils.showError("No email entered", "Please enter an email address.");
            return;
        }

        new Thread(() -> {
            Invite invite = new Invite(
                    0, // ID será gerado pelo servidor
                    selectedGroup.getId(),
                    clientService.getCurrentUser().getId(),
                    0, // ReceiverId será resolvido pelo servidor
                    selectedGroup.getName(),
                    clientService.getCurrentUser().getEmail(),
                    inviteeEmail,
                    Invite.Status.PENDING // Define o status inicial como PENDING
            );

            ServerResponse response = clientService.sendRequest(new Message(Message.Type.CREATE_INVITE, invite));

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    AlertUtils.showSuccess("Invite Sent", "Invite sent successfully to " + inviteeEmail + ".");
                    emailField.clear();
                } else {
                    AlertUtils.showError("Failed to send invite", response.message());
                }
            });
        }).start();
    }

    public void handleDeclineButtonAction() {
        Invite selectedInvite = inviteList.getSelectionModel().getSelectedItem();
        if (selectedInvite == null) {
            AlertUtils.showError("No invite selected", "Please select an invite to decline.");
            return;
        }

        new Thread(() -> {
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.DECLINE_INVITE, selectedInvite));

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    AlertUtils.showSuccess("Invite Declined", "Invite declined successfully.");
                    inviteList.getItems().remove(selectedInvite);
                } else {
                    AlertUtils.showError("Failed to decline invite", response.message());
                }
            });
        }).start();
    }

    public void handleAcceptButtonAction() {
        Invite selectedInvite = inviteList.getSelectionModel().getSelectedItem();
        if (selectedInvite == null) {
            AlertUtils.showError("No invite selected", "Please select an invite to accept.");
            return;
        }

        new Thread(() -> {
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.ACCEPT_INVITE, selectedInvite));

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    AlertUtils.showSuccess("Invite Accepted", "Invite accepted successfully.");
                    inviteList.getItems().remove(selectedInvite);
                } else {
                    AlertUtils.showError("Failed to accept invite", response.message());
                }
            });
        }).start();
    }

    public void handleBackButtonAction() {
        NavigationManager.switchScene(Routes.DASHBOARD);
    }
}