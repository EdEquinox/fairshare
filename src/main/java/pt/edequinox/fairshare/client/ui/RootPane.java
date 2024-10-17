package pt.edequinox.fairshare.client.ui;

import javafx.scene.layout.BorderPane;
import pt.edequinox.fairshare.client.model.ClientManager;

public class RootPane extends BorderPane {

    ClientManager clientManager;

    public RootPane(ClientManager clientManager) {
        this.clientManager = clientManager;
        createViews();
        registerHandlers();
        update();
    }

    private void registerHandlers() {

    }

    private void createViews() {
    }

    private void update() {
    }
}
