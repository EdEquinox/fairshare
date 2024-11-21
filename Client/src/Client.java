import communication.ClientService;
import javafx.application.Application;
import javafx.stage.Stage;
import utils.Logger;
import utils.NavigationManager;
import utils.Routes;

import java.io.IOException;

// javafxPath -> /Users/josexavier/development/javafx-sdk-23.0.1/lib

public class Client extends Application {

    private static String serverHost;
    private static int serverPort;

    public static void main(String[] args) {
        // Check if correct number of arguments is provided
        if (args.length != 2) {
            Logger.error("Usage: java Client <host> <port>");
            System.exit(1);
        }

        try {
            // Store the command-line arguments for future use in Application methods
            serverHost = args[0];
            serverPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            Logger.error("Port must be an integer. Provided: " + args[1]);
            System.exit(1);
        }

        // Launch the JavaFX application
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        NavigationManager.initialize(primaryStage);

        // TODO: Check if using Singleton is good practice
        ClientService.initialize(serverHost, serverPort);

        NavigationManager.switchScene(Routes.HOME);

        primaryStage.setTitle("Home Page");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }
}
