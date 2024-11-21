import com.google.gson.Gson;
import model.Group;
import model.Message;
import model.ServerResponse;
import model.User;
import utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;

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

                try {
                    Message message = gson.fromJson(command, Message.class); // Parseia diretamente o comando JSON
                    switch (message.type()) {
                        case Message.Type.REGISTER -> {
                            User user = gson.fromJson(gson.toJson(message.payload()), User.class);
                            handleRegister(user);
                        }
                        case Message.Type.LOGIN -> {
                            User user = gson.fromJson(gson.toJson(message.payload()), User.class);
                            handleLogin(user);
                        }
                        case Message.Type.LOGOUT -> {
                            User user = gson.fromJson(gson.toJson(message.payload()), User.class);
                            handleLogout(user);
                        }
                        case Message.Type.EDIT_PROFILE -> {
                            User user = gson.fromJson(gson.toJson(message.payload()), User.class);
                            handleEditProfile(user);
                        }
                        case Message.Type.GET_PROFILE -> {
                            User user = gson.fromJson(gson.toJson(message.payload()), User.class);
                            handleGetProfile(user);
                        }
                        case Message.Type.CREATE_GROUP -> {
                            Group group = gson.fromJson(gson.toJson(message.payload()), Group.class);
                            handleCreateGroup(group);
                        }
                        default -> sendResponse(new ServerResponse(false, "Invalid command", null));
                    }
                } catch (Exception e) {
                    Logger.error("Error receiving message: " + e.getMessage());
                    sendResponse(new ServerResponse(false, "Internal Server Error...", null));
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

    private void handleCreateGroup(Group group) {
        String url = "jdbc:sqlite:" + databasePath;

        // SQL para verificar se o grupo já existe
        String checkGroupSQL = "SELECT id FROM groups WHERE name = ?";

        // SQL para inserir um novo grupo
        String insertGroupSQL = "INSERT INTO groups (name) VALUES (?)";

        // SQL para associar o usuário ao grupo
        String insertUserGroupSQL = "INSERT INTO users_groups (user_id, group_id) VALUES (?, ?)";

        boolean isSuccess = false;
        String message;
        Group createdGroup = null;

        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);

            int groupId;

            // Verificar se o grupo já existe
            try (PreparedStatement checkStmt = conn.prepareStatement(checkGroupSQL)) {
                checkStmt.setString(1, group.name());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    groupId = rs.getInt("id");
                    Logger.info("Group already exists: " + group.name());
                    message = "Group already exists";
                } else {
                    try (PreparedStatement insertGroupStmt = conn.prepareStatement(insertGroupSQL, Statement.RETURN_GENERATED_KEYS)) {
                        insertGroupStmt.setString(1, group.name());
                        insertGroupStmt.executeUpdate();

                        try (ResultSet generatedKeys = insertGroupStmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                groupId = generatedKeys.getInt(1);
                                Logger.info("Group created successfully: " + group.name());
                            } else {
                                throw new SQLException("Failed to retrieve group ID after insertion");
                            }
                        }
                    }
                }
            }

            try (PreparedStatement insertUserGroupStmt = conn.prepareStatement(insertUserGroupSQL)) {
                insertUserGroupStmt.setInt(1, group.ownerId());
                insertUserGroupStmt.setInt(2, groupId);
                insertUserGroupStmt.executeUpdate();
                Logger.info("User associated with group: User ID " + group.ownerId() + ", Group ID " + groupId);
            }

            conn.commit();
            isSuccess = true;
            message = "Group created successfully";
        } catch (SQLException e) {
            Logger.error("Error creating group or associating user: " + e.getMessage());
            message = "Error creating group or associating user";
            isSuccess = false;
        }

        sendResponse(new ServerResponse(isSuccess, message, null));
    }

    private void handleLogout(User user) {
        try {
            Logger.info("User requested logout. Closing connection.");
            // sendResponse(createJsonResponse("response", "SUCCESS"));

            // Close the server-side socket connection
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                Logger.info("Client connection closed after logout.");
            }
        } catch (IOException e) {
            Logger.error("Error closing client connection during logout: " + e.getMessage());
        }
    }


    private void handleEditProfile(User user) throws IOException {

        String url = "jdbc:sqlite:" + databasePath;
        String updateSQL = "UPDATE users WHERE id = ?";

        boolean isSuccess = false;
        String message;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement stmt = conn.prepareStatement(updateSQL)) {

            stmt.setString(1, String.valueOf(user.getId()));

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                isSuccess = true;
                message = "User profile updated successfully";
                Logger.info("User profile updated successfully for email: " + user.getEmail());
            } else {
                message = "Incorrect password!";
                Logger.error("Incorrect password: " + user.getEmail());
            }
        } catch (SQLException e) {
            message = "Error while editing profile";
            Logger.error("Database error while updating profile: " + e.getMessage());
        }

        sendResponse(new ServerResponse(isSuccess, message, null));

    }

    private void handleGetProfile(User user) throws IOException {
        String url = "jdbc:sqlite:" + databasePath;
        String querySQL = "SELECT name, email, phone, password FROM users WHERE email = ?";

        boolean isSuccess = false;
        String message;
        User retrievedUser = null;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement stmt = conn.prepareStatement(querySQL)) {

            stmt.setString(1, user.getEmail());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                retrievedUser = new User(rs.getString("name"), rs.getString("email"), rs.getString("phone"), null);
                isSuccess = true;
                message = "User retrieved successfully";
            } else {
                message = "Error while retrieving profile";
                Logger.error("No user found with email: " + user.getEmail());
            }
        } catch (SQLException e) {
            message = "Error while retrieving profile";
            Logger.error("Database error while fetching profile: " + e.getMessage());
        }
        sendResponse(new ServerResponse(isSuccess, message, retrievedUser));
    }

    private void handleRegister(User user) throws IOException {
        String url = "jdbc:sqlite:" + databasePath;
        String checkEmailSQL = "SELECT COUNT(*) FROM users WHERE email = ?";
        String insertSQL = "INSERT INTO users (name, email, phone, password) VALUES (?, ?, ?, ?)";

        boolean isSuccess = false;
        String message;

        try (Connection conn = DriverManager.getConnection(url)) {
            // Check if the email already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(checkEmailSQL)) {
                checkStmt.setString(1, user.getEmail());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    message = "Error: Email already in use";
                    Logger.error("Email already exists in the database: " + user.getEmail());
                    sendResponse(new ServerResponse(false, message, null));
                    return;
                }
            }

            // Insert if email doesn't exist
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                insertStmt.setString(1, user.getName());
                insertStmt.setString(2, user.getEmail());
                insertStmt.setString(3, user.getPhone());
                insertStmt.setString(4, user.getPassword());
                insertStmt.executeUpdate();
                isSuccess = true;
                message = "User registered successfully";
                Logger.info("User added successfully to the database: " + user.getName());
            }

        } catch (SQLException e) {
            message = "Error while creating a new user";
            Logger.error("Database error: " + e.getMessage());
        }

        ServerResponse response = new ServerResponse(isSuccess, message, null);
        sendResponse(response);

    }

    private void handleLogin(User user) throws IOException {

        String url = "jdbc:sqlite:" + databasePath;
        String querySQL = "SELECT * FROM users WHERE email = ? AND password = ?";

        boolean isSuccess = false;
        String message;
        User authenticatedUser = null;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement stmt = conn.prepareStatement(querySQL)) {

            // Set the parameters for the query
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getPassword());

            // Execute the query and check if a matching record exists
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                authenticatedUser = new User(rs.getString("name"), rs.getString("email"), rs.getString("phone"), null);
                authenticatedUser.setId(rs.getInt("id"));
                Logger.info("User authenticated successfully: " + user.getEmail());
                message = "User authenticated successfully";
                isSuccess = true;
            } else {
                Logger.error("Authentication failed for email: " + user.getEmail());
                message = "Authentication failed";
            }
        } catch (SQLException e) {
            Logger.error("Database error during authentication: " + e.getMessage());
            message = "Error while authenticating...";
        }

        ServerResponse response = new ServerResponse(isSuccess, message, authenticatedUser);
        sendResponse(response);
    }

    private void sendResponse(ServerResponse response) {
        String jsonResponse = gson.toJson(response);
        out.println(jsonResponse);
        out.flush();
        Logger.info("Server response to client: " + response);
    }

}
