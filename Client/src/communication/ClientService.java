package communication;

import com.google.gson.JsonObject;
import model.Invite;
import model.Message;
import model.User;
import utils.Logger;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;

public class ClientService {

    private static ClientService instance;

    private final String serverIp;
    private final int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final Gson gson = new Gson();
    private boolean isConnected = false;

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
    public static boolean initialize(String serverIp, int serverPort) {
        if (instance == null) {
            instance = new ClientService(serverIp, serverPort);
        } else {
            throw new IllegalStateException("ClientService has already been initialized.");
        }
        Logger.error("Failed to connect to the server after retries.");
        return false; // Connection failed
    }

    public boolean connectToServer() {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                socket = new Socket(serverIp, serverPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Logger.info("Connected to server at " + serverIp + ":" + serverPort);
                isConnected = true;
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

    /**
     * Receives a message from the server.
     * @return The message received, or null if an error occurs.
     */
    public String receiveMessage() {
        try {
            if (in != null) {
                return in.readLine();
            }
        } catch (IOException e) {
            Logger.error("Error receiving message: " + e.getMessage());
        }
        return null;
    }

    public int getUserId() {
        return 0;
    }

    public String registerUser(User user) {
        return sendRequest(new Message(Message.Type.REGISTER, user));
    }

    public String loginUser(String email, String password) {
        return sendRequest(new Message(Message.Type.LOGIN, new User(email, password, null, null)));
    }

    public String getUserProfile(String email) {
        return sendRequest(new Message(Message.Type.GET_PROFILE, new User(email, null, null, null)));
    }

    public String sendInvite(String inviteeEmail, int groupId) {
        return sendRequest(new Message(Message.Type.INVITE, new Invite(inviteeEmail, groupId)));
    }

    public String getGroups() {
        return sendRequest(new Message(Message.Type.GET_GROUPS, null));
    }

    public String getPendingInvites( int userId) {
        return sendRequest(new Message(Message.Type.GET_INVITES, userId));
    }

    /**
     * Helper method to create error responses in JSON format.
     * @param message The error message.
     * @return A JSON string containing the error response.
     */
    private String createErrorResponse(String message) {
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("type", "response");
        errorResponse.addProperty("data", message);
        return errorResponse.toString();
    }

    public String sendRequest(Message message) {
        try {
            if (socket == null || socket.isClosed()) {
                if (!connectToServer()) {
                    return createErrorResponse("Error: Unable to connect to server");
                }
            }

            JsonObject request = new JsonObject();
            request.addProperty("timeStamp", System.currentTimeMillis());
            request.addProperty("type", message.type().toString());
            request.addProperty("message", gson.toJson(message.payload()));

            if (out != null) {
                out.println(request);
                out.flush();
                Logger.info("Sent message to server: " + request);
            } else {
                Logger.error("Attempted to send message, but output stream is not initialized.");
            }

            return receiveMessage();
        } catch (Exception e) {
            Logger.error("Error during command execution: " + e.getMessage());
            return createErrorResponse("Error: " + e.getMessage());
        }
    }

    public boolean isClientConnected() {
        return isConnected;
    }

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




}
