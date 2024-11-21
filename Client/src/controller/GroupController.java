package controller;

import com.google.gson.Gson;
import communication.ClientService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import model.Group;
import model.Message;
import model.ServerResponse;
import utils.AlertUtils;
import utils.Logger;
import utils.NavigationManager;
import utils.Routes;
import utils.SharedState;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;


import java.util.List;

public class GroupController {

    @FXML
    private ListView<Group> groupListView; // Mostra os grupos de que o user faz parte

    @FXML
    private ListView<String> userListView; // Mostra os users no grupo selecionado

    private ClientService clientService;

    private ObservableList<Group> groups; // Lista de grupos
    private ObservableList<String> usersInGroup; // Lista de nomes de users
    private final Gson gson = new Gson();

    public void initialize() {
        // Instancia do cliente
        clientService = ClientService.getInstance();

        // Inicializa as listas
        groups = FXCollections.observableArrayList();
        usersInGroup = FXCollections.observableArrayList();

        // Conecta as listas as views correspondentes
        groupListView.setItems(groups);
        userListView.setItems(usersInGroup);

        // Configura como os grupos sao mostrados na listveiw
        groupListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                } else {
                    setText(group.name());
                }
            }
        });

        fetchGroups();

        // Listener para a seleçao do grupo
        groupListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                SharedState.setSelectedGroup(newValue); // Guarda o grupo em SharedState
                fetchUsersForGroup(newValue); // Procura os utilizadores desse grupo
            }
        });
    }

    private void fetchGroups() {
        // Guarda o utilizador que fez login em currentUser que foi guardado no sharedstate
        var currentUser = SharedState.getCurrentUser();
        if (currentUser == null) {
            Logger.error("No current user found in SharedState. Please log in again.");
            AlertUtils.showError("Error", "No logged-in user found. Please log in again.");
            return;
        }

        new Thread(() -> {
            if (clientService.isClientConnected()) {
                // Envia um pedido de procura de grupos para o currentuser
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_GROUPS, currentUser));

                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        try {
                            // Guarda no tipo generico o List<Group>
                            Type groupListType = new TypeToken<List<Group>>() {}.getType();
                            List<Group> fetchedGroups = new Gson().fromJson(new Gson().toJson(response.payload()), groupListType);
                            groups.setAll(fetchedGroups);
                            Logger.info("Groups fetched successfully for user: " + currentUser.getName());
                        } catch (Exception e) {
                            Logger.error("Failed to deserialize groups: " + e.getMessage());
                            AlertUtils.showError("Error", "Invalid server response format.");
                        }
                    } else {
                        AlertUtils.showError("Error", "Failed to fetch groups: " + response.message());
                        Logger.error("Failed to fetch groups for user: " + currentUser.getName());
                    }
                });
            } else {
                javafx.application.Platform.runLater(() -> {
                    AlertUtils.showError("Error", "Client is not connected to the server.");
                    Logger.error("Client is not connected to the server.");
                });
            }
        }).start();
    }


    private void fetchUsersForGroup(Group selectedGroup) {
        new Thread(() -> {
            if (clientService.isClientConnected()) {
                // Envia um pedido para reaver os utilizadores do grupo selecionado
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_USERS_FOR_GROUP, selectedGroup));

                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        // atualiza a lista de users com o cast da payload
                        List<String> users = (List<String>) response.payload();
                        usersInGroup.setAll(users);
                        Logger.info("Users fetched successfully for group: " + selectedGroup.name());
                    } else {
                        AlertUtils.showError("Error", "Failed to fetch users: " + response.message());
                        Logger.error("Failed to fetch users for group: " + selectedGroup.name());
                    }
                });
            } else {
                javafx.application.Platform.runLater(() -> {
                    AlertUtils.showError("Error", "Client is not connected to the server.");
                    Logger.error("Client is not connected to the server.");
                });
            }
        }).start();
    }

    public void handleGoToExpensesPage(ActionEvent actionEvent) {
        // Verifica se algum grupo foi selecionado antes de ir para as expenses
        Group selectedGroup = SharedState.getSelectedGroup();
        if (selectedGroup == null) {
            AlertUtils.showError("Error", "No group selected. Please select a group first.");
            return;
        }

        Logger.info("Navigating to expenses page for group: " + selectedGroup.name());
        NavigationManager.switchScene(Routes.EXPENSES);
    }

    public void handleGoToDashboard(ActionEvent actionEvent) {
        NavigationManager.switchScene(Routes.DASHBOARD);
    }
}
