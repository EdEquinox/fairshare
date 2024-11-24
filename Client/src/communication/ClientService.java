package communication;

import com.google.gson.JsonSyntaxException;
import com.google.gson.Gson;
import model.Message;
import model.ServerResponse;
import model.User;
import utils.Logger;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientService {

    private static final Gson gson = new Gson();
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MILLIS = 5000;
    private static ClientService instance;
    private final String serverIp;
    private final int serverPort;
    private User currentUser;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false;
    private BroadcastListener broadcastListener;

    // Private constructor to prevent instantiation
    private ClientService(String serverIp, int serverPort) {
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
    public static boolean initialize(String serverIp, int serverPort) {
        if (instance == null) {
            instance = new ClientService(serverIp, serverPort);
        } else {
            throw new IllegalStateException("ClientService has already been initialized.");
        }
        Logger.error("Failed to connect to the server after retries.");
        return false; // Connection failed
    }

    // Start listening for server broadcasts
    private void startListeningForBroadcasts() {
        new Thread(() -> {
            try {
                while (isConnected) {
                    // Listen for incoming broadcast messages from the server
                    String line = in.readLine();
                    if (line != null && !line.isBlank()) {
                        try {
                            ServerResponse response = gson.fromJson(line, ServerResponse.class);
                            if (broadcastListener != null) {
                                Platform.runLater(() -> broadcastListener.onBroadcastReceived(response));
                            }
                        } catch (JsonSyntaxException e) {
                            Logger.error("Error parsing broadcast message: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error("Error receiving broadcast message: " + e.getMessage());
            }
        }).start();
    }

    // Register the broadcast listener
    public void addBroadcastListener(BroadcastListener listener) {
        this.broadcastListener = listener;
    }

    // Method to establish the connection to the server
    public boolean connectToServer() {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                socket = new Socket(serverIp, serverPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Logger.info("Connected to server at " + serverIp + ":" + serverPort);
                isConnected = true;

                // Start listening for broadcasts after successful connection
                startListeningForBroadcasts();
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
        isConnected = false;
        return false;
    }

    // Method to send a request to the server
    public ServerResponse sendRequest(Message message) {
        try {
            if (socket == null || socket.isClosed()) {
                if (!connectToServer()) {
                    return new ServerResponse(false, "Unable to connect to server", null);
                }
            }

            String jsonMessage = gson.toJson(message);

            if (out != null) {
                out.println(jsonMessage);
                out.flush();
                Logger.info("Sent message to server: " + jsonMessage);
            } else {
                Logger.error("Output stream is not initialized.");
                return new ServerResponse(false, "Output stream is not initialized", null);
            }

            return receiveResponse();
        } catch (Exception e) {
            Logger.error("Error during command execution: " + e.getMessage());
            return new ServerResponse(false, "Error during command execution: " + e.getMessage(), null);
        }
    }

    private ServerResponse receiveResponse() {
        try {
            if (in != null) {
                String line = in.readLine();
                if (line == null || line.isBlank()) {
                    Logger.error("Received empty or null response from server.");
                    return null;
                }

                try {
                    return gson.fromJson(line, ServerResponse.class);
                } catch (JsonSyntaxException e) {
                    Logger.error("Error parsing JSON response: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            Logger.error("Error receiving message: " + e.getMessage());
        }
        return null;
    }

    // Check if the client is connected
    public boolean isClientConnected() {
        return isConnected;
    }

    // Close the connection to the server
    public void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                Logger.info("Connection closed.");
            }
        } catch (IOException e) {
            Logger.error("Error closing connection: " + e.getMessage());
        }
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    // Interface to handle broadcast events
    public interface BroadcastListener {
        void onBroadcastReceived(ServerResponse response);
    }
}
