package communication;

import utils.Logger;
import java.io.*;
import java.net.Socket;

public class ClientService {
    private final String serverIp;
    private final int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ClientService(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    /**
     * Establishes a connection with the server.
     */
    public void connectToServer() {
        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Logger.info("Connected to server at " + serverIp + ":" + serverPort);
        } catch (IOException e) {
            Logger.error("Error connecting to server: " + e.getMessage());
        }
    }

    /**
     * Sends a message to the server.
     * @param message The message to send.
     */
    public void sendMessage(String message) {
        try {
            out.println(message);
            Logger.info("Sent message to server: " + message);
        } catch (Exception e) {
            Logger.error("Error sending message: " + e.getMessage());
        }
    }

    /**
     * Receives a response from the server.
     * @return The response from the server.
     */
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

    /**
     * Closes the connection to the server.
     */
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