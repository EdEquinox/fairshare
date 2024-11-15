import com.google.gson.JsonObject;
import utils.Logger;
import com.google.gson.Gson;
import model.Message;
import model.User;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final String databasePath;
    private static final Gson gson = new Gson();

    public ClientHandler(Socket clientSocket, String databasePath) {
        this.clientSocket = clientSocket;
        this.databasePath = databasePath;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String command;
            while ((command = in.readLine()) != null) {
                Logger.info("Received command: " + command);  // Log the command received

                if ("REGISTER".equals(command)) {
                    String userData = in.readLine();
                    Logger.info("Received user data: " + userData);  // Log the user data received

                    if (userData == null) {
                        String errorResponse = createJsonResponse("response", "Error: No user data received.");
                        out.println(errorResponse);
                        Logger.info("Server response to client: " + errorResponse); // Log the error response sent
                        continue;
                    }

                    User user = gson.fromJson(userData, User.class);
                    String response = addUserToDatabase(user);
                    out.println(response);
                    Logger.info("Server response to client: " + response); // Log the success/error response sent
                } else {
                    String unknownCommandResponse = createJsonResponse("response", "Error: Unknown command");
                    out.println(unknownCommandResponse);
                    Logger.info("Server response to client: " + unknownCommandResponse); // Log unknown command
                }
            }
        } catch (IOException e) {
            Logger.error("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
                Logger.info("Client connection closed.");
            } catch (IOException e) {
                Logger.error("Error closing client connection: " + e.getMessage());
            }
        }
    }

    private String addUserToDatabase(User user) {
        String url = "jdbc:sqlite:" + databasePath;
        String checkEmailSQL = "SELECT COUNT(*) FROM users WHERE email = ?";
        String insertSQL = "INSERT INTO users (name, email, phone, password) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            // Check if the email already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(checkEmailSQL)) {
                checkStmt.setString(1, user.getEmail());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    String errorResponse = createJsonResponse("response", "Error: Email already exists");
                    Logger.error("Email already exists in the database: " + user.getEmail());
                    return errorResponse;
                }
            }

            // Insert if email doesn't exist
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                insertStmt.setString(1, user.getName());
                insertStmt.setString(2, user.getEmail());
                insertStmt.setString(3, user.getPhone());
                insertStmt.setString(4, user.getPassword());
                insertStmt.executeUpdate();
                Logger.info("User added successfully to the database: " + user.getName());
                return createJsonResponse("response", "SUCCESS");
            }

        } catch (SQLException e) {
            String errorResponse = createJsonResponse("response", "Error: " + e.getMessage());
            Logger.error("Database error: " + e.getMessage());
            return errorResponse;
        }
    }

    // Helper method to format JSON responses consistently
    private String createJsonResponse(String type, String data) {
        JsonObject response = new JsonObject();
        response.addProperty("type", type);
        response.addProperty("data", data);
        return response.toString();
    }
}
