import communication.ClientService;
import controller.HomeController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.Logger;

import java.io.IOException;

public class Client extends Application {

    private static String serverHost;
    private static int serverPort;
    private ClientService clientService;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Initialize client communication first to get serverHost and serverPort
        initializeCommunication();

        // Load the home.fxml and get the corresponding controller (HomeController)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/layouts/home.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Home Page");
        primaryStage.setScene(scene);
        primaryStage.setMaxWidth(800);
        primaryStage.setMaxHeight(600);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        primaryStage.show();

        // Get the controller for the home.fxml and set server info
        HomeController homeController = loader.getController();
        homeController.setServerInfo(serverHost, serverPort);
    }

    private void initializeCommunication() {
        // Use command-line arguments to set up the host and port
        serverHost = getParameters().getRaw().get(0);
        serverPort = Integer.parseInt(getParameters().getRaw().get(1));

        clientService = new ClientService(serverHost, serverPort);

        // Run the client communication in a separate thread to keep the UI responsive
        new Thread(() -> {
            try {
                clientService.connectToServer();
                clientService.sendMessage("helloWorld");

                String response = clientService.receiveMessage();
                Logger.info("Server response: " + response);
            } catch (Exception e) {
                Logger.error("Communication error: " + e.getMessage());
            } finally {
                clientService.closeConnection();
            }
        }).start();
    }

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
}
