package pt.edequinox.fairshare.client.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.edequinox.fairshare.client.Main;
import pt.edequinox.fairshare.client.model.ClientManager;

public class FairShareClient extends Application {
    ClientManager clientManager;
    public FairShareClient() {
        this.clientManager = Main.clientManager;
    }

    @Override
    public void start(Stage stage) throws Exception {
        LandingUI root = new LandingUI(clientManager);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("FairShare Client");
        stage.show();
    }



}
