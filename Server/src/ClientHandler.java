import com.google.gson.Gson;
import model.*;
import utils.Logger;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final String databasePath;
    private static final Gson gson = new Gson();
    private final static String MULTICAST_ADDRESS = "230.44.44.44";
    private final static int MULTICAST_PORT = 4444;
    private int version;

    public ClientHandler(Socket clientSocket, String databasePath) {
        this.clientSocket = clientSocket;
        this.databasePath = databasePath;
        this.version = 1;
    }

    @Override
    public void run() {

        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String command;
            while ((command = in.readLine()) != null) {
                Logger.info("Received command: " + command);

                if (command.equals("GET_DATABASE_FILE")) {
                    sendDatabaseFile(clientSocket);
                    continue;
                }
                if (command.equals("GET_VERSION")) {
                    sendVersion(clientSocket);
                    continue;
                }

                try {
                    Message message = gson.fromJson(command, Message.class); // Parseia diretamente o comando JSON
                    Logger.info("Received message: " + message);
                    Logger.info("Message type: " + message.type());
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
                        case Message.Type.GET_GROUPS -> {
                            Logger.info("Fetching groups...");
                            User user = gson.fromJson(gson.toJson(message.payload()), User.class);
                            handleGetGroups(user);
                        }
                        case Message.Type.GET_PENDING_INVITES  -> {
                            User user = gson.fromJson(gson.toJson(message.payload()), User.class);
                            handleGetInvites(user);
                        }
                        case Message.Type.INVITE -> {
                            ArrayList<Object> payload = gson.fromJson(gson.toJson(message.payload()), ArrayList.class);
                            Group group = gson.fromJson(gson.toJson(payload.get(0)), Group.class);
                            User user = gson.fromJson(gson.toJson(payload.get(1)), User.class);
                            String email = gson.fromJson(gson.toJson(payload.get(2)), String.class);
                            sendInvite(user, group, email);
                        }
                        case Message.Type.GET_USERS_FOR_GROUP -> {
                            Group group = gson.fromJson(gson.toJson(message.payload()), Group.class); // Deserialize Group object
                            handleGetUsersForGroup(group);
                        }
                        case Message.Type.GET_EXPENSES -> {
                            Group group = gson.fromJson(gson.toJson(message.payload()), Group.class); // Deserialize Group object
                            handleGetExpenses(group);
                        }
                        case Message.Type.BACKUP_INIT -> {
                            sendDatabaseFile(clientSocket);
                        }
                        case Message.Type.ACCEPT_INVITE -> {
                            ArrayList<Object> payload = gson.fromJson(gson.toJson(message.payload()), ArrayList.class);
                            Invite invite = gson.fromJson(gson.toJson(payload.get(0)), Invite.class);
                            User user = gson.fromJson(gson.toJson(payload.get(1)), User.class);
                            acceptInvite(invite, user);
                        }
                        case Message.Type.DECLINE_INVITE -> {
                            ArrayList<Object> payload = gson.fromJson(gson.toJson(message.payload()), ArrayList.class);
                            Invite invite = gson.fromJson(gson.toJson(payload.get(0)), Invite.class);
                            User user = gson.fromJson(gson.toJson(payload.get(1)), User.class);
                            declineInvite(invite, user);
                        }
                        case Message.Type.GET_GROUP_NAME -> {
                            int groupId = gson.fromJson(gson.toJson(message.payload()), Integer.class);
                            String groupName = getGroupNamefromDB(groupId);
                            sendResponse(new ServerResponse(true, "Group name retrieved successfully", groupName));
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

    private void acceptInvite(Invite invite, User user) {
        String url = "jdbc:sqlite:" + databasePath;
        String updateSQL = "UPDATE group_invites SET status = 'accepted' WHERE id = ? AND invited_user = ?";
        String insertUserGroupSQL = "INSERT INTO users_groups (user_id, group_id) VALUES (?, ?)";
        String checkUserGroupSQL = "SELECT COUNT(*) FROM users_groups WHERE user_id = ? AND group_id = ?";

        boolean isSuccess = false;
        String message;

        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);

            try (PreparedStatement checkUserGroupStmt = conn.prepareStatement(checkUserGroupSQL)) {
                checkUserGroupStmt.setInt(1, user.getId());
                checkUserGroupStmt.setInt(2, invite.getGroupId());
                ResultSet rs = checkUserGroupStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    Logger.info("User already associated with group: User ID " + user.getId() + ", Group ID " + invite.getGroupId());
                    message = "User already associated with group";
                    sendResponse(new ServerResponse(isSuccess, message, null));
                    return;
                }
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setInt(1, invite.getId());
                updateStmt.setInt(2, user.getId());
                updateStmt.executeUpdate();
                Logger.info("Invite accepted successfully");
            }

            try (PreparedStatement insertUserGroupStmt = conn.prepareStatement(insertUserGroupSQL)) {
                insertUserGroupStmt.setInt(1, user.getId());
                insertUserGroupStmt.setInt(2, invite.getGroupId());
                insertUserGroupStmt.executeUpdate();
                Logger.info("User associated with group: User ID " + user.getId() + ", Group ID " + invite.getGroupId());
            }

            conn.commit();
            updateVersion();
            updateSQL = "UPDATE group_invites SET status = 'accepted' WHERE id = " + invite.getId() + " AND invited_user = " + user.getId();
            sendHeartbeat(updateSQL, getVersion());
            updateVersion();
            insertUserGroupSQL = "INSERT INTO users_groups (user_id, group_id) VALUES (" + user.getId() + ", " + invite.getGroupId() + ")";
            sendHeartbeat(insertUserGroupSQL, getVersion());
            isSuccess = true;
            message = "Invite accepted successfully";
            sendResponse(new ServerResponse(isSuccess, message, null));
        } catch (SQLException e) {
            Logger.error("Database error while accepting invite: " + e.getMessage());
            message = "Error while accepting invite";
            isSuccess = false;
            sendResponse(new ServerResponse(isSuccess, message, null));
        }

    }

    private void declineInvite(Invite invite, User user) {
        String url = "jdbc:sqlite:" + databasePath;
        String updateSQL = "UPDATE group_invites SET status = 'declined' WHERE id = ? AND invited_user = ?";

        boolean isSuccess = false;
        String message;

        try (Connection conn = DriverManager.getConnection(url)) {
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setInt(1, invite.getId());
                updateStmt.setInt(2, user.getId());
                updateStmt.executeUpdate();
                Logger.info("Invite declined successfully");
                isSuccess = true;
                message = "Invite declined successfully";
            }
        } catch (SQLException e) {
            Logger.error("Database error while declining invite: " + e.getMessage());
            message = "Error while declining invite";
            isSuccess = false;
        }

        updateVersion();
        updateSQL = "UPDATE group_invites SET status = 'declined' WHERE id = " + invite.getId() + " AND invited_user = " + user.getId();
        sendHeartbeat(updateSQL, getVersion());


        sendResponse(new ServerResponse(isSuccess, message, null));

    }

    private int getVersion() {
        String url = "jdbc:sqlite:" + databasePath;
        String query = "SELECT version FROM version";
        int version = 0;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                version = rs.getInt("version");
            }
        } catch (SQLException e) {
            Logger.error("Error getting version: " + e.getMessage());
        }

        return version;
    }

    private void updateVersion() {
        String url = "jdbc:sqlite:" + databasePath;
        String updateSQL = "UPDATE version SET version = version + 1";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(updateSQL)) {
            stmt.executeUpdate();
            Logger.info("Version updated successfully");
        } catch (SQLException e) {
            Logger.error("Error updating version: " + e.getMessage());
        }
    }

    private void sendVersion(Socket clientSocket) {
        try {
            Logger.info("Sending version to client...");
            out.println(getVersion());
            out.flush();
            Logger.info("Version sent successfully: " + getVersion());
        } catch (Exception e) {
            Logger.error("Error sending version: " + e.getMessage());
        }
    }

    private void sendDatabaseFile(Socket clientSocket) {
        try (FileInputStream fis = new FileInputStream(databasePath);
        OutputStream os = clientSocket.getOutputStream()) {
            Logger.info("Sending database file to client...");
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                Logger.info("bytesRead: " + bytesRead);
            }
            os.flush();
            Logger.info("buffer" + buffer.length);
            Logger.info("Database file sent successfully.");

        } catch (Exception e) {
            Logger.error("Error sending database file: " + e.getMessage());
        }
    }

    private void sendHeartbeat(String query, Integer version) {
        Logger.info("Sending heartbeat from main server...");
        ArrayList<Object> payload = new ArrayList<>();
        payload.add(query);
        payload.add(version);

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            ServerResponse response = new ServerResponse(true, "Heartbeat sent successfully", payload);
            byte[] data = gson.toJson(response).getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_PORT);
            socket.send(packet);
            Logger.info("Heartbeat sent successfully, Version: " + version);

        } catch (Exception e) {
            Logger.error("Error sending heartbeat: " + e.getMessage());
        }

    }

    private void handleGetExpenses(Group group) {
        // Query que seleciona as expenses de um determinado grupo pelo id e ordenado pelas datas
        String query = "SELECT id, group_id, paid_by, amount, description, date " +
                "FROM expenses WHERE group_id = ? ORDER BY date ASC";

        boolean isSuccess = false;
        String message;
        List<Expense> expenses = new ArrayList<>();

        // conecta à base de dados
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, group.getId()); // Usa o id do grupo passado como parametro
            ResultSet rs = stmt.executeQuery();
            // executa a query e em quanto tiver mais expenses continua a adicionar
            while (rs.next()) {
                expenses.add(new Expense(
                        rs.getInt("id"),
                        rs.getInt("group_id"),
                        rs.getInt("paid_by"),
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        rs.getString("date")
                ));
            }
            // verifica se a lista esta vazia
            if (expenses.isEmpty()) {
                message = "No expenses found for the group.";
                Logger.info("No expenses found for group: " + group.name());
            } else {
                isSuccess = true;
                message = "Expenses retrieved successfully.";
                Logger.info("Expenses retrieved for group: " + group.name());
            }

        } catch (SQLException e) {
            message = "Error while retrieving expenses.";
            Logger.error("Database error while fetching expenses: " + e.getMessage());
        }

        sendResponse(new ServerResponse(isSuccess, message, expenses));
    }

    private void handleGetUsersForGroup(Group group) {
        // query que utiliza a tabela conjuta de users e grupos para devolver os users de um determinado grupo
        String query = "SELECT u.name " +
                "FROM users u " +
                "JOIN users_groups ug ON u.id = ug.user_id " +
                "WHERE ug.group_id = ?";

        boolean isSuccess = false;
        String message;
        List<String> users = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, group.getId()); // usa o id do grupo
            ResultSet rs = stmt.executeQuery();
            // adiciona os nomes dos grupos enquanto existirem mais
            while (rs.next()) {
                users.add(rs.getString("name"));
            }

            if (users.isEmpty()) {
                message = "No users found for the group.";
                Logger.info("No users found for group: " + group.name());
            } else {
                isSuccess = true;
                message = "Users retrieved successfully.";
                Logger.info("Users retrieved for group: " + group.name());
            }

        } catch (SQLException e) {
            message = "Error while retrieving users.";
            Logger.error("Database error while fetching users: " + e.getMessage());
        }

        sendResponse(new ServerResponse(isSuccess, message, users));
    }

    private void handleGetGroups(User user) {

        // verificaçao no caso do user ser null ou o id ser menor ou igual a 0
        if (user == null || user.getId() <= 0) {
            Logger.error("Invalid user data received for GET_GROUPS: " + user);
            sendResponse(new ServerResponse(false, "Invalid user data.", null));
            return;
        }

        // query que seleciona os grupos relacionados a um determinado user_id
        String url = "jdbc:sqlite:" + databasePath;
        String query = "SELECT g.id, g.name, ug.user_id AS owner_id " +
                "FROM groups g " +
                "JOIN users_groups ug ON g.id = ug.group_id " +
                "WHERE ug.user_id = ?";

        boolean isSuccess = false;
        String message;
        List<Group> groups = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                groups.add(new Group(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("owner_id")
                ));
            }

            if (groups.isEmpty()) {
                message = "No groups found for the user.";
                Logger.info("No groups found for user ID: " + user.getId());
            } else {
                isSuccess = true;
                message = "Groups retrieved successfully.";
                Logger.info("Groups retrieved for user ID: " + user.getId() + ": " + groups);
            }

        } catch (SQLException e) {
            message = "Error while retrieving groups.";
            Logger.error("Database error while fetching groups: " + e.getMessage());
        }

        sendResponse(new ServerResponse(isSuccess, message, groups));
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
            updateVersion();
            insertGroupSQL = "INSERT INTO groups (name) VALUES ('" + group.name() + "')";
            sendHeartbeat(insertGroupSQL, getVersion());
            updateVersion();
            insertUserGroupSQL = "INSERT INTO users_groups (user_id, group_id) VALUES (" + group.ownerId() + ", " + groupId + ")";
            sendHeartbeat(insertUserGroupSQL, getVersion());
            isSuccess = true;
            message = "Group created successfully";
        } catch (SQLException e) {
            Logger.error("Error creating group or associating user: " + e.getMessage());
            message = "Error creating group or associating user";
            isSuccess = false;
        }

        sendResponse(new ServerResponse(isSuccess, message, null));
    }

    private String getGroupNamefromDB(int groupId) {
        String query = "SELECT name FROM groups WHERE id = ?";
        try(Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath + "/db.db");
            PreparedStatement preparedStatement = connection.prepareStatement(query);) {
            preparedStatement.setInt(1, groupId);
            ResultSet resultSet = preparedStatement.executeQuery();
            String groupName = resultSet.getString("name");
            connection.close();
            return groupName;
        } catch (Exception e) {
            Logger.error("Error getting group name: " + e.getMessage());
            return null;
        }
    }

    private void handleGetInvites(User user) {

        String url = "jdbc:sqlite:" + databasePath;
        String querySQL = "SELECT gi.id, gi.group_id, gi.invited_user, gi.invited_by, gi.status, g.name AS group_name " +
                "FROM group_invites gi " +
                "JOIN groups g ON gi.group_id = g.id " +
                "WHERE gi.invited_user = ? AND gi.status = 'pending'";

        String groupNameQuery = "SELECT name FROM groups WHERE id = ?";
        String inviterNameQuery = "SELECT name FROM users WHERE id = ?";
        String groupName;
        String inviterName;
        boolean isSuccess = false;
        String message;
        List<Invite> invites = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url)) {
            try (PreparedStatement inviterNameStmt = conn.prepareStatement(inviterNameQuery)) {
                try (PreparedStatement groupNameStmt = conn.prepareStatement(groupNameQuery)) {
                    try (PreparedStatement stmt = conn.prepareStatement(querySQL)) {
                        stmt.setInt(1, user.getId());
                        ResultSet rs = stmt.executeQuery();
                        while (rs.next()) {
                            groupNameStmt.setInt(1, rs.getInt("group_id"));
                            inviterNameStmt.setInt(1, rs.getInt("invited_by"));
                            ResultSet rs2 = groupNameStmt.executeQuery();
                            ResultSet rs3 = inviterNameStmt.executeQuery();
                            if (rs2.next()) {
                                groupName = rs2.getString("name");
                                inviterName = rs3.getString("name");
                                invites.add(new Invite(
                                        rs.getInt("id"),
                                        rs.getInt("group_id"),
                                        rs.getInt("invited_by"),
                                        rs.getInt("invited_user"),
                                        groupName,
                                        inviterName
                                ));
                            }
                        }
                        if (invites.isEmpty()) {
                            message = "No invites found for the user.";
                            Logger.info("No invites found for user ID: " + user.getId());
                        } else {
                            isSuccess = true;
                            message = "Invites retrieved successfully.";
                            Logger.info("Invites retrieved for user ID: " + user.getId() + ": " + invites);
                        }

                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            message = "Error while retrieving invites";
            Logger.error("Database error while fetching invites: " + e.getMessage());
        }

        sendResponse(new ServerResponse(isSuccess, message, invites));
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
            updateVersion();
            updateSQL = "UPDATE users WHERE id = " + user.getId();
            sendHeartbeat(updateSQL, getVersion());
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
                updateVersion();
                insertSQL = "INSERT INTO users (name, email, phone, password) VALUES ('" + user.getName() + "', '" + user.getEmail() + "', '" + user.getPhone() + "', '" + user.getPassword() + "')";
                sendHeartbeat(insertSQL, getVersion());
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

    private void sendInvite(User user, Group group, String email) {
        String url = "jdbc:sqlite:" + databasePath;
        String checkInviteeSQL = "SELECT COUNT(*) FROM users WHERE email = ?";
        String checkGroupSQL = "SELECT COUNT(*) FROM groups WHERE id = ?";
        String insertSQL = "INSERT INTO group_invites (group_id, invited_user, invited_by, status) VALUES (?, ?, ?, 'pending')";

        try (Connection conn = DriverManager.getConnection(url)) {
            // Check if the invitee email exists
            try (PreparedStatement checkInviteeStmt = conn.prepareStatement(checkInviteeSQL)) {
                checkInviteeStmt.setString(1, email);
                ResultSet rs = checkInviteeStmt.executeQuery();
                if (!rs.next() || rs.getInt(1) == 0) {
                    Logger.error("Invitee email not found in the database: " + email);
                    sendResponse(new ServerResponse(false, "Error: Invitee email not found", null));
                    return;
                }
                Logger.info("Invitee email found in the database: " + user.getEmail());
            }

            // Check if the group exists
            try (PreparedStatement checkGroupStmt = conn.prepareStatement(checkGroupSQL)) {
                checkGroupStmt.setInt(1, group.getId());
                ResultSet rs = checkGroupStmt.executeQuery();
                if (!rs.next() || rs.getInt(1) == 0) {
                    Logger.error("Group not found in the database: " + group.getName());
                    sendResponse(new ServerResponse(false, "Error: Group not found", null));
                    return;
                }
                Logger.info("Group found in the database: " + group.getName());
            }

            //Get invitee user
            String getUserSQL = "SELECT id FROM users WHERE email = ?";
            int inviteeId = 0;
            try (PreparedStatement getUserStmt = conn.prepareStatement(getUserSQL)) {
                getUserStmt.setString(1, email);
                ResultSet rs = getUserStmt.executeQuery();
                if (rs.next()) {
                    inviteeId = rs.getInt("id");
                    Logger.info("User found in the database: " + email);
                } else {
                    Logger.error("User not found in the database: " + email);
                    sendResponse(new ServerResponse(false, "Error: User not found", null));
                    return;
                }
            }

            // Check if the invitee is already a member of the group
            String checkUserGroupSQL = "SELECT COUNT(*) FROM users_groups WHERE user_id = ? AND group_id = ?";
            try (PreparedStatement checkUserGroupStmt = conn.prepareStatement(checkUserGroupSQL)) {
                checkUserGroupStmt.setInt(1, inviteeId);
                checkUserGroupStmt.setInt(2, group.getId());
                ResultSet rs = checkUserGroupStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    Logger.error("User is already a member of the group: " + group.getName());
                    sendResponse(new ServerResponse(false, "Error: User is already a member of the group", null));
                    return;
                }
                Logger.info("User is not a member of the group: " + user.getName());
            }

            // Insert the invite
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                insertStmt.setInt(1, group.getOwnerId());
                insertStmt.setInt(2, inviteeId);
                insertStmt.setInt(3, user.getId());
                insertStmt.executeUpdate();
                updateVersion();
                insertSQL = "INSERT INTO group_invites (group_id, invited_user, invited_by, status) VALUES (" + group.getOwnerId() + ", " + inviteeId + ", " + user.getId() + ", 'pending')";
                sendHeartbeat(insertSQL, getVersion());
                Logger.info("Invite sent successfully");
                sendResponse(new ServerResponse(true, "Invite sent successfully", null));
            }


        } catch (SQLException e) {
            Logger.error("Database error: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Error while sending invite", null));
        }
    }

    private void sendResponse(ServerResponse response) {
        String jsonResponse = gson.toJson(response);
        out.println(jsonResponse);
        out.flush();
        Logger.info("Server response to client: " + response);
    }

}
