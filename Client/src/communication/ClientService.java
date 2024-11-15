package communication;

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

    public void connectToServer() {
        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Logger.info("Connected to server at " + serverIp + ":" + serverPort);
        } catch (IOException e) {
            Logger.error("Error connecting to server: " + e.getMessage());
            // Ensure the socket is closed on failure
            closeConnection();
        }
    }


    public void sendMessage(String message) {
        try {
            out.println(message);
            out.flush();
            Logger.info("Sent message to server: " + message);
        } catch (Exception e) {
            Logger.error("Error sending message: " + e.getMessage());
        }
    }

    public String receiveMessage() {
        try {
            return in.readLine();
        } catch (IOException e) {
            Logger.error("Error receiving message: " + e.getMessage());
        }
        return null;
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

    public String registerUser(User user) {
        try {
            connectToServer();

            if (socket == null || socket.isClosed() || out == null) {
                Logger.error("Connection to server failed.");
                return "Connection error: Unable to register user";
            }

            sendMessage("REGISTER");

            // Send user details as JSON
            String userJson = gson.toJson(user);
            sendMessage(userJson);

            return receiveMessage();

        } catch (Exception e) {
            Logger.error("Error during user registration: " + e.getMessage());
            return "Error during registration: " + e.getMessage();
        } finally {
            closeConnection();
        }
    }

}
