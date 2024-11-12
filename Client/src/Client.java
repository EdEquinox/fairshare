import communication.ClientService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.Logger;

import java.io.IOException;

public class Client extends Application {

    private ClientService clientService;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/layouts/home.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Home Page");
        primaryStage.setScene(scene);

        primaryStage.setMaxWidth(800);
        primaryStage.setMaxHeight(600);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        primaryStage.show();

        initializeCommunication();

    }

    private void initializeCommunication() {
        String host = getParameters().getRaw().get(0);
        int port = Integer.parseInt(getParameters().getRaw().get(1));

        clientService = new ClientService(host, port);

        new Thread(() -> {
            clientService.connectToServer();
            clientService.sendMessage("helloWorld");

            String response = clientService.receiveMessage();
            Logger.info("Server response: " + response);

            clientService.closeConnection();
        }).start();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            Logger.error("Usage: java Client <host> <port>");
            return;
        }

        launch(args);
    }

}