package pt.edequinox.fairshare.client.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.edequinox.fairshare.client.Main;
import pt.edequinox.fairshare.client.model.ClientManager;

public class FairShareClient extends Application {
    ClientManager clientManager;

    public void main(String[] args) {
        this.clientManager = Main.clientManager;
    }

    @Override
    public void start(Stage stage) throws Exception {
        RootPane root = new RootPane(clientManager);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("FairShare Client");
        stage.show();
    }



}
