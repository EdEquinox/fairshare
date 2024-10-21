package pt.edequinox.fairshare.client.ui;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import pt.edequinox.fairshare.client.model.ClientManager;

public class EditProfileUI extends BorderPane {

    ClientManager clientManager;

    public EditProfileUI(ClientManager clientManager) {
        this.clientManager = clientManager;
        createViews();
        registerHandlers();
        update();
    }

    private void createViews() {
    }

    private void registerHandlers() {
    }

    private void update() {
    }
}
