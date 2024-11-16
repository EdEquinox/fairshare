package communication;

import utils.Logger;
import java.io.*;
import java.net.Socket;

public class ClientService {

    private static ClientService instance;

    private final String serverIp;
    private final int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MILLIS = 5000;

    // Private constructor to prevent instantiation
    public ClientService(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    // Method to get the singleton instance
    public static ClientService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ClientService not initialized. Call initialize() first.");
        }
        return instance;
    }

    // Method to initialize the singleton instance
    public static void initialize(String serverIp, int serverPort) {
        if (instance == null) {
            instance = new ClientService(serverIp, serverPort);
        } else {
            throw new IllegalStateException("ClientService has already been initialized.");
        }
    }

    public boolean connectToServer() {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                socket = new Socket(serverIp, serverPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Logger.info("Connected to server at " + serverIp + ":" + serverPort);
                return true;
            } catch (IOException e) {
                attempts++;
                Logger.error("Error connecting to server (Attempt " + attempts + "): " + e.getMessage());
                if (attempts < MAX_RETRIES) {
                    Logger.info("Retrying in " + RETRY_DELAY_MILLIS / 1000 + " seconds...");
                    try {
                        Thread.sleep(RETRY_DELAY_MILLIS);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    Logger.info("Max retries reached. Could not connect to the server.");
                    break;
                }
            }
        }
        return false;
    }

    public void sendMessage(String message) {
        try {
            out.println(message);
            Logger.info("Sent message to server: " + message);
        } catch (Exception e) {
            Logger.error("Error sending message: " + e.getMessage());
        }
    }

    public String receiveMessage() {
        String response = null;
        try {
            response = in.readLine();
            Logger.info("Received message from server: " + response);
        } catch (IOException e) {
            Logger.error("Error receiving message: " + e.getMessage());
        }

        return response;
    }

    public void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
                Logger.info("Connection closed.");
            }
        } catch (IOException e) {
            Logger.error("Error closing connection: " + e.getMessage());
        }
    }
}