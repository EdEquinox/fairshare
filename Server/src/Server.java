import utils.Logger;

import java.io.IOException;

public class Server {

    private int port;
    private String dbPath;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Server <port> <database_path>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String dbPath = args[1];

        ServerService serverService = new ServerService(port, dbPath);

        try {
            serverService.startServer();
        } catch (IOException e) {
            System.err.println("Error starting the server: " + e.getMessage());
            System.exit(1);
        }

    }
}