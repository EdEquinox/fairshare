import communication.ClientService;
import communication.ClientServiceAware;
import controller.HomeController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.Logger;
import utils.NavigationManager;
import utils.Routes;

import java.io.IOException;

public class Client extends Application {


    @Override
    public void start(Stage primaryStage) throws IOException {

        NavigationManager.initialize(primaryStage);

        String host = getParameters().getRaw().get(0);
        int port = Integer.parseInt(getParameters().getRaw().get(1));

        // TODO: Check if using Singleton is good practice
        ClientService.initialize(host, port);

        NavigationManager.switchScene(Routes.HOME.toString().toLowerCase());


        primaryStage.setTitle("Home Page");
        primaryStage.setMaxWidth(800);
        primaryStage.setMaxHeight(600);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            Logger.error("Usage: java Client <host> <port>");
            return;
        }

        launch(args);
    }

}