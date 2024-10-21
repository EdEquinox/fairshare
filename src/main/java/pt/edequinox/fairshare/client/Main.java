package pt.edequinox.fairshare.client;

import javafx.application.Application;
import pt.edequinox.fairshare.client.model.ClientManager;
import pt.edequinox.fairshare.client.ui.FairShareClient;

import java.io.IOException;

public class Main {

    public static ClientManager clientManager = null;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java -jar fairshare-client.jar <server-ip>");
            System.exit(1);
        }
        String ip = args[0];
        clientManager = new ClientManager();
        Application.launch(FairShareClient.class, args);
    }
}