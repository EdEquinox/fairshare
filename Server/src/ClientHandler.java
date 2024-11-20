import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.Logger;
import com.google.gson.Gson;
import model.Message;
import model.User;
import utils.Mailer;

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
                Logger.info("Received command: " + command);

                switch (command) {
                    case "REGISTER":
                        handleRegister();
                        break;
                    case "LOGIN":
                        handleLogin();
                        break;
                    case "LOGOUT":
                        handleLogout();
                        break;
                    case "EDIT_PROFILE":
                        handleEditProfile();
                        break;
                    case "GET_PROFILE":
                        handleGetProfile();
                        break;
                    case "INVITE":
                        handleInvite();
                        break;
                    case "GET_GROUPS":
                        handleGetGroups();
                        break;

                    default:
                        handleUnknownCommand();
                }
            }
        } catch (IOException e) {
            Logger.error("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    Logger.info("Client connection closed.");
                }
            } catch (IOException e) {
                Logger.error("Error closing client connection: " + e.getMessage());
            }
        }
    }

    private void handleGetGroups() {
        String url = "jdbc:sqlite:" + databasePath;
        String querySQL = "SELECT id, name FROM groups";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(querySQL)) {

            ResultSet rs = stmt.executeQuery();
            JsonObject groupsJson = new JsonObject();
            int count = 0;

            while (rs.next()) {
                JsonObject groupJson = new JsonObject();
                groupJson.addProperty("id", rs.getInt("id"));
                groupJson.addProperty("name", rs.getString("name"));
                groupsJson.add(String.valueOf(count), groupJson);
                count++;
            }

            sendResponse(createJsonResponse("response", groupsJson.toString()));
        } catch (SQLException e) {
            Logger.error("Database error while fetching groups: " + e.getMessage());
            sendErrorResponse("Database error while fetching groups.");
        }
    }

    private void handleLogout() {
        try {
            Logger.info("User requested logout. Closing connection.");
            sendResponse(createJsonResponse("response", "SUCCESS"));

            // Close the server-side socket connection
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                Logger.info("Client connection closed after logout.");
            }
        } catch (IOException e) {
            Logger.error("Error closing client connection during logout: " + e.getMessage());
        }
    }


    private void handleEditProfile() throws IOException {
        String updatedProfileData = in.readLine();
        if (updatedProfileData == null) {
            sendErrorResponse("No profile data received.");
            return;
        }

        JsonObject profileJson = JsonParser.parseString(updatedProfileData).getAsJsonObject();
        String name = profileJson.get("name").getAsString();
        String updatedEmail = profileJson.get("email").getAsString(); // Renamed to updatedEmail
        String phone = profileJson.get("phone").getAsString();
        String password = profileJson.get("password").getAsString();

        String updateResponse = updateUserProfile(name, updatedEmail, phone, password);
        sendResponse(updateResponse);
    }

    private void handleGetProfile() throws IOException {
        String userEmail = in.readLine();
        if (userEmail == null) {
            sendErrorResponse("No email provided for fetching profile.");
            return;
        }

        String profileResponse = getUserProfile(userEmail);
        sendResponse(profileResponse);
    }

    private String getUserProfile(String email) {
        String url = "jdbc:sqlite:" + databasePath;
        String querySQL = "SELECT name, email, phone, password FROM users WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(querySQL)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                JsonObject profileJson = new JsonObject();
                profileJson.addProperty("name", rs.getString("name"));
                profileJson.addProperty("email", rs.getString("email"));
                profileJson.addProperty("phone", rs.getString("phone"));
                profileJson.addProperty("password", rs.getString("password"));

                return createJsonResponse("response", profileJson.toString());
            } else {
                return createJsonResponse("response", "Error: User not found");
            }
        } catch (SQLException e) {
            Logger.error("Database error while fetching profile: " + e.getMessage());
            return createJsonResponse("response", "Error: Database error");
        }
    }

    private String updateUserProfile(String name, String email, String phone, String password) {
        String url = "jdbc:sqlite:" + databasePath;
        String updateSQL = "UPDATE users SET name = ?, phone = ?, password = ? WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(updateSQL)) {

            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, password);
            stmt.setString(4, email);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                Logger.info("User profile updated successfully for email: " + email);
                return createJsonResponse("response", "SUCCESS");
            } else {
                Logger.error("No user found with email: " + email);
                return createJsonResponse("response", "Error: User not found");
            }
        } catch (SQLException e) {
            Logger.error("Database error while updating profile: " + e.getMessage());
            return createJsonResponse("response", "Error: Database error");
        }
    }

    private void handleRegister() throws IOException {
        String userData = in.readLine();
        if (userData == null) {
            sendErrorResponse("No user data received.");
            return;
        }
        User user = gson.fromJson(userData, User.class);
        String response = addUserToDatabase(user);
        sendResponse(response);
    }

    private void handleLogin() throws IOException {
        String loginData = in.readLine();
        if (loginData == null) {
            sendErrorResponse("No login data received.");
            return;
        }
        JsonObject loginJson = JsonParser.parseString(loginData).getAsJsonObject();
        String email = loginJson.get("email").getAsString();
        String password = loginJson.get("password").getAsString();

        String loginResponse = authenticateUser(email, password);
        sendResponse(loginResponse);
    }

    private void handleInvite() throws IOException {
        String inviteData = in.readLine();
        if (inviteData == null) {
            sendErrorResponse("No invite data received.");
            return;
        }
        JsonObject inviteJson = JsonParser.parseString(inviteData).getAsJsonObject();
        String inviteeEmail = inviteJson.get("inviteeEmail").getAsString();
        int groupId = inviteJson.get("groupId").getAsInt();
        int userId = inviteJson.get("userId").getAsInt();
        String inviterEmail = inviteJson.get("inviterEmail").getAsString();

        String inviteResponse = sendInvite(inviteeEmail, groupId, userId);
        sendResponse(inviteResponse);
    }

    private void handleUnknownCommand() {
        String unknownCommandResponse = createJsonResponse("response", "Error: Unknown command");
        sendResponse(unknownCommandResponse);
    }

    private void sendResponse(String response) {
        out.println(response);
        Logger.info("Server response to client: " + response);
    }

    private void sendErrorResponse(String error) {
        sendResponse(createJsonResponse("response", "Error: " + error));
    }

    private void closeClientConnection() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                Logger.info("Client connection closed.");
            }
        } catch (IOException e) {
            Logger.error("Error closing client connection: " + e.getMessage());
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

    private String sendInvite(String inviteeEmail, int groupId, int userId) {
        String url = "jdbc:sqlite:" + databasePath;
        String checkInviteeSQL = "SELECT COUNT(*) FROM users WHERE email = ?";
        String checkGroupSQL = "SELECT COUNT(*) FROM groups WHERE id = ?";
        String insertSQL = "INSERT INTO group_invites (group_id, user_id) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            // Check if the invitee email exists
            try (PreparedStatement checkInviteeStmt = conn.prepareStatement(checkInviteeSQL)) {
                checkInviteeStmt.setString(1, inviteeEmail);
                ResultSet rs = checkInviteeStmt.executeQuery();
                if (!rs.next() || rs.getInt(1) == 0) {
                    String errorResponse = createJsonResponse("response", "Error: Invitee email not found");
                    Logger.error("Invitee email not found in the database: " + inviteeEmail);
                    return errorResponse;
                }
            }

            // Check if the group exists
            try (PreparedStatement checkGroupStmt = conn.prepareStatement(checkGroupSQL)) {
                checkGroupStmt.setInt(1, groupId);
                ResultSet rs = checkGroupStmt.executeQuery();
                if (!rs.next() || rs.getInt(1) == 0) {
                    String errorResponse = createJsonResponse("response", "Error: Group not found");
                    Logger.error("Group not found in the database: " + groupId);
                    return errorResponse;
                }
            }

            // Send email to invitee
            Mailer.sendMail(inviteeEmail, "You have been invited to a group",
                    "You have been invited to join a group. Please log in to view the invite.", "invite@farishare.pt");

            // Insert the invite
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                insertStmt.setInt(1, groupId);
                insertStmt.setInt(2, userId);
                insertStmt.executeUpdate();
                Logger.info("Invite sent successfully to: " + inviteeEmail);
                return createJsonResponse("response", "SUCCESS");
            }


        } catch (SQLException e) {
            String errorResponse = createJsonResponse("response", "Error: " + e.getMessage());
            Logger.error("Database error: " + e.getMessage());
            return errorResponse;
        }
    }

    private String authenticateUser(String email, String password) {
        String url = "jdbc:sqlite:" + databasePath;
        String querySQL = "SELECT COUNT(*) FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(querySQL)) {

            // Set the parameters for the query
            stmt.setString(1, email);
            stmt.setString(2, password);

            // Execute the query and check if a matching record exists
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                Logger.info("User authenticated successfully: " + email);
                return createJsonResponse("response", "SUCCESS");
            } else {
                Logger.error("Authentication failed for email: " + email);
                return createJsonResponse("response", "Error: Invalid email or password");
            }
        } catch (SQLException e) {
            String errorResponse = createJsonResponse("response", "Error: Database error during authentication");
            Logger.error("Database error during authentication: " + e.getMessage());
            return errorResponse;
        }
    }



}
