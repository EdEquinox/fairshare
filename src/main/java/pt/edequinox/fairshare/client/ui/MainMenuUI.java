package pt.edequinox.fairshare.client.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import pt.edequinox.fairshare.client.model.ClientManager;

public class MainMenuUI extends BorderPane {

    ClientManager clientManager;
    Button groupsBtn, logoutBtn, newGroupBtn, addExpenseBtn, editProfileBtn, invitesBtn;

    public MainMenuUI(ClientManager clientManager) {
        this.clientManager = clientManager;
        createViews();
        registerHandlers();
        update();
    }

    private void createViews() {
        groupsBtn = new Button("Groups");
        logoutBtn = new Button("Logout");
        newGroupBtn = new Button("New Group");
        addExpenseBtn = new Button("Add Expense");
        editProfileBtn = new Button("Edit Profile");
        invitesBtn = new Button("Invites");
    }

    private void registerHandlers() {

        clientManager.addPropertyChangeListener(evt -> {
            update();
        });

        VBox vBox = new VBox(groupsBtn, newGroupBtn, addExpenseBtn, editProfileBtn, invitesBtn, logoutBtn);
        this.setCenter(vBox);

        groupsBtn.setOnAction(e -> {
            this.setCenter(new GroupsUI(clientManager));
            clientManager.seeGroups();
        });

        newGroupBtn.setOnAction(e -> {
            this.setCenter(new NewGroupUI(clientManager));
            clientManager.newGroup();
        });

        addExpenseBtn.setOnAction(e -> {
            this.setCenter(new AddExpenseUI(clientManager));
            clientManager.addExpense();
        });

        editProfileBtn.setOnAction(e -> {
            this.setCenter(new EditProfileUI(clientManager));
            clientManager.editCredentials();
        });

        invitesBtn.setOnAction(e -> {
            this.setCenter(new InvitesUI(clientManager));
            clientManager.seeInvites();
        });

        logoutBtn.setOnAction(e -> {
            clientManager.logout();
            this.setCenter(new LandingUI(clientManager));
        });

    }

    private void update() {
        System.out.println(clientManager.getState());
    }

}
