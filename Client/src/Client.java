import communication.ClientService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.Logger;

import java.io.IOException;

public class Client extends Application {


    @Override
    public void start(Stage primaryStage) throws IOException {
        // Carregar o arquivo FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/layouts/home.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Home Page");
        primaryStage.setScene(scene);
        // Limita o tamanho da janela a 800x600
        primaryStage.setMaxWidth(800);
        primaryStage.setMaxHeight(600);
        primaryStage.setMinWidth(800);  // Define o tamanho mínimo para 800x600
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {

        if (args.length != 2) {
            Logger.error("Usage: java Client <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        ClientService clientService = new ClientService(host, port);

        clientService.connectToServer();

        clientService.sendMessage("helloWorld");

        String response = clientService.receiveMessage();
        Logger.info("Server response: " + response);

        clientService.closeConnection();

        launch(args);
    }
}