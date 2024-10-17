package pt.edequinox.fairshare.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import pt.edequinox.fairshare.client.model.ClientManager;
import pt.edequinox.fairshare.client.ui.FairShareClient;

import java.io.IOException;

public class Main {

    static String ip;
    public static ClientManager clientManager = null;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -jar fairshare-client.jar <server-ip>");
            System.exit(1);
        }
        ip = args[0];
        clientManager = new ClientManager(ip);
        Application.launch(FairShareClient.class, args);
    }
}