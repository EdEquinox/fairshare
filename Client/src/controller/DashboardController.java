package controller;

import communication.ClientService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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
import utils.SharedState;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import com.google.gson.Gson;

public class DashboardController implements Initializable {

    public ClientService clientService;
    public Text userText;

    private User currentUser;
    private final Gson gson = new Gson();

    @FXML
    private ListView<Group> groupList;

    private ObservableList<Group> groups;

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

    public void handleAddNewGroup() {
        NavigationManager.switchScene(Routes.CREATE_GROUP);
    }

    public void handleSelectGroup(Group selectedGroup) {
        //Guarda o grupo selecionado no sharedState
        SharedState.setSelectedGroup(selectedGroup);
        NavigationManager.switchScene(Routes.GROUP);
    }

    public void handleGroupInvites() {
        // TODO: Handle group invites logic here
    }

    public void handleGroupInfo() {
        // TODO: Handle group info logic here
    }

    public void handleFetchGroups() {
        new Thread(() -> {
            if (clientService.isClientConnected()) {
                //Envia o type mais o current user na payload
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_GROUPS, currentUser));

                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        try {
                            // TypeToken captura o tipo genérico List<Group> para a desserialização com Gson; getType devolve o tipo List<Group>
                            Type groupListType = new TypeToken<List<Group>>() {}.getType();
                            // Converte o payload da resposta para JSON e desserializa-o para uma lista de objetos do tipo Group
                            List<Group> fetchedGroups = gson.fromJson(gson.toJson(response.payload()), groupListType);
                            // Atualiza a ObservableList "groups" com os novos grupos
                            groups.setAll(fetchedGroups);
                            Logger.info("Groups fetched successfully: " + fetchedGroups);
                        } catch (Exception e) {
                            Logger.error("Failed to deserialize groups: " + e.getMessage());
                            AlertUtils.showError("Error", "Invalid server response format.");
                        }
                    } else {
                        AlertUtils.showError("Error", "Failed to fetch groups: " + response.message());
                        Logger.error("Failed to fetch groups: " + response.message());
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


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.clientService = ClientService.getInstance();
        this.currentUser = clientService.getCurrentUser();
        userText.setText(currentUser.getName());

        groups = FXCollections.observableArrayList();
        groupList.setItems(groups);

        // Mostra os nomes dos grupos na listview
        groupList.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                } else {
                    setText(group.name()); // Mostra o nome
                }
            }
        });

        handleFetchGroups();

        // Listener para a seleçao do grupo da lista
        groupList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleSelectGroup(newValue);
            }
        });
    }
}
