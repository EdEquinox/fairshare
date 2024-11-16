package communication;

import com.google.gson.JsonObject;
import model.User;
import utils.Logger;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;

public class ClientService {
    private final String serverIp;
    private final int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final Gson gson = new Gson();

    public ClientService(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    /**
     * Establishes a connection to the server.
     * Retries up to 3 times if the connection fails.
     */
    public boolean connectToServer() {
        int retries = 3;
        while (retries > 0) {
            try {
                if (socket == null || socket.isClosed()) {
                    socket = new Socket(serverIp, serverPort);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    Logger.info("Connected to server at " + serverIp + ":" + serverPort);
                    return true; // Connection successful
                }
            } catch (IOException e) {
                retries--;
                Logger.error("Connection failed. Retrying... (" + retries + " attempts left)");
                closeConnection(); // Release resources between retries
                try {
                    Thread.sleep(1000); // Wait 1 second before retrying
                } catch (InterruptedException ex) {
                    Logger.error("Retry sleep interrupted: " + ex.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupted state
                }
            }
        }
        Logger.error("Failed to connect to the server after retries.");
        return false; // Connection failed
    }


    /**
     * Sends a message to the server.
     * @param message The message to send.
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush();
            Logger.info("Sent message to server: " + message);
        } else {
            Logger.error("Attempted to send message, but output stream is not initialized.");
        }
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



    public String registerUser(User user) {
        return sendCommand("REGISTER", gson.toJson(user));
    }

    public String loginUser(String email, String password) {
        JsonObject loginData = new JsonObject();
        loginData.addProperty("email", email);
        loginData.addProperty("password", password);
        return sendCommand("LOGIN", loginData.toString());
    }

    public String getUserProfile(String email) {
        return sendCommand("GET_PROFILE", email);
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

    public String sendCommand(String command, String payload) {
        try {
            if (socket == null || socket.isClosed()) {
                if (!connectToServer()) {
                    return createErrorResponse("Error: Unable to connect to server");
                }
            }

            sendMessage(command);
            if (payload != null) {
                sendMessage(payload);
            }
            return receiveMessage();
        } catch (Exception e) {
            Logger.error("Error during command execution: " + e.getMessage());
            return createErrorResponse("Error: " + e.getMessage());
        }
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
