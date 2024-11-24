import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.*;
import utils.Logger;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
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
                        case Message.Type.CREATE_INVITE -> {
                            Invite invite = gson.fromJson(gson.toJson(message.payload()), Invite.class);
                            handleCreateInvite(invite);
                        }
                        case Message.Type.ACCEPT_INVITE -> {
                            Invite invite = gson.fromJson(gson.toJson(message.payload()), Invite.class);
                            handleAcceptInvite(invite);
                        }
                        case Message.Type.DECLINE_INVITE -> {
                            Invite invite = gson.fromJson(gson.toJson(message.payload()), Invite.class);
                            handleDeclineInvite(invite);
                        }
                        case Message.Type.GET_INVITES -> {
                            User user = gson.fromJson(gson.toJson(message.payload()), User.class);
                            handleGetInvites(user);
                        }
                        case Message.Type.CREATE_GROUP -> {
                            Group group = gson.fromJson(gson.toJson(message.payload()), Group.class);
                            handleCreateGroup(group);
                        }
                        case Message.Type.EDIT_GROUP -> {
                            Group group = gson.fromJson(gson.toJson(message.payload()), Group.class);
                            handleEditGroup(group);
                        }
                        case Message.Type.REMOVE_GROUP -> {
                            Group group = gson.fromJson(gson.toJson(message.payload()), Group.class);
                            handleRemoveGroup(group);
                        }
                        case Message.Type.GET_GROUPS -> {
                            Logger.info("Fetching groups...");
                            User user = gson.fromJson(gson.toJson(message.payload()), User.class);
                            handleGetGroups(user);
                        }
                        case Message.Type.GET_GROUP_USERS -> {
                            Group group = gson.fromJson(gson.toJson(message.payload()), Group.class); // Deserialize Group object
                            handleGetGroupUsers(group);
                        }
                        case Message.Type.BACKUP_INIT -> {
                            sendDatabaseFile(clientSocket);
                        }
                        case Message.Type.GET_GROUP_NAME -> {
                            int groupId = gson.fromJson(gson.toJson(message.payload()), Integer.class);
                            String groupName = getGroupNamefromDB(groupId);
                            sendResponse(new ServerResponse(true, "Group name retrieved successfully", groupName));
                        }
                        case Message.Type.ADD_EXPENSE -> {
                            Expense expense = gson.fromJson(gson.toJson(message.payload()), Expense.class);
                            handleAddExpense(expense);
                        }
                        case Message.Type.EDIT_EXPENSE -> {
                            Expense expense = gson.fromJson(gson.toJson(message.payload()), Expense.class);
                            handleEditExpense(expense);
                        }
                        case Message.Type.DELETE_EXPENSE -> {
                            int expenseId = gson.fromJson(gson.toJson(message.payload()), Integer.class);
                            handleDeleteExpense(expenseId);
                        }
                        case Message.Type.GET_EXPENSES -> {
                            int groupId = gson.fromJson(gson.toJson(message.payload()), Integer.class);
                            handleGetExpenses(groupId);
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

    private void handleCreateInvite(Invite invite) {
        String getUserIdSql = "SELECT id FROM users WHERE email = ?";
        String insertInviteSql = "INSERT INTO group_invites (group_id, invited_by, invited_user, status) VALUES (?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            int receiverId;
            try (PreparedStatement getUserStatement = connection.prepareStatement(getUserIdSql)) {
                getUserStatement.setString(1, invite.getReceiverEmail());
                ResultSet rs = getUserStatement.executeQuery();


                if (rs.next()) {
                    receiverId = rs.getInt("id");
                } else {
                    Logger.error("No user found with email: " + invite.getReceiverEmail());
                    sendResponse(new ServerResponse(false, "No user found with the provided email.", null));
                    return;
                }
            }

            try (PreparedStatement insertInviteStatement = connection.prepareStatement(insertInviteSql)) {
                insertInviteStatement.setInt(1, invite.getGroupId());
                insertInviteStatement.setInt(2, invite.getSenderId());
                insertInviteStatement.setInt(3, receiverId);
                insertInviteStatement.setString(4, invite.getStatus().name());

                int rowsAffected = insertInviteStatement.executeUpdate();
                updateVersion();
                String newSQL = "INSERT INTO group_invites (group_id, invited_by, invited_user, status) VALUES (" + invite.getGroupId() + ", " + invite.getSenderId() + ", " + receiverId + ", '" + invite.getStatus().name() + "')";
                sendHeartbeat(newSQL, getVersion());
                if (rowsAffected > 0) {
                    Logger.info("Invite created successfully: " + invite);
                    sendResponse(new ServerResponse(true, "Invite created successfully.", null));
                } else {
                    Logger.error("Failed to create invite: " + invite);
                    sendResponse(new ServerResponse(false, "Failed to create invite.", null));
                }
            }

        } catch (SQLException e) {
            Logger.error("Database error while creating invite: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }

    private void handleAcceptInvite(Invite invite) {
        String updateInviteSQL = "UPDATE group_invites SET status = ? WHERE id = ?";
        String addUserToGroupSQL = "INSERT INTO users_groups (user_id, group_id) VALUES (?, ?)";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            connection.setAutoCommit(false); // Inicia a transação

            try (PreparedStatement updateInviteStmt = connection.prepareStatement(updateInviteSQL); PreparedStatement addUserToGroupStmt = connection.prepareStatement(addUserToGroupSQL)) {

                // Atualiza o status do convite para "ACCEPTED"
                updateInviteStmt.setString(1, Invite.Status.ACCEPTED.name());
                updateInviteStmt.setInt(2, invite.getId());
                updateInviteStmt.executeUpdate();
                updateVersion();
                String newSQL = "UPDATE group_invites SET status = 'ACCEPTED' WHERE id = " + invite.getId();
                sendHeartbeat(newSQL, getVersion());

                // Adiciona o usuário ao grupo
                addUserToGroupStmt.setInt(1, invite.getReceiverId());
                addUserToGroupStmt.setInt(2, invite.getGroupId());
                addUserToGroupStmt.executeUpdate();
                updateVersion();
                newSQL = "INSERT INTO users_groups (user_id, group_id) VALUES (" + invite.getReceiverId() + ", " + invite.getGroupId() + ")";
                sendHeartbeat(newSQL, getVersion());

                connection.commit(); // Confirma a transação
                sendResponse(new ServerResponse(true, "Invite accepted successfully.", null));
                Logger.info("Invite accepted and user added to group: " + invite);

            } catch (SQLException e) {
                connection.rollback(); // Reverte a transação em caso de erro
                Logger.error("Error while accepting invite: " + e.getMessage());
                sendResponse(new ServerResponse(false, "Error while accepting invite: " + e.getMessage(), null));
            }

        } catch (SQLException e) {
            Logger.error("Database connection error while accepting invite: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }

    private void handleDeclineInvite(Invite invite) {
        String updateInviteSQL = "UPDATE group_invites SET status = ? WHERE id = ?";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(updateInviteSQL)) {

            // Atualiza o status do convite para "DENIED"
            statement.setString(1, Invite.Status.DENIED.name());
            statement.setInt(2, invite.getId());
            int rowsAffected = statement.executeUpdate();
            updateVersion();
            String newSQL = "UPDATE group_invites SET status = 'DENIED' WHERE id = " + invite.getId();
            sendHeartbeat(newSQL, getVersion());

            if (rowsAffected > 0) {
                sendResponse(new ServerResponse(true, "Invite declined successfully.", null));
                Logger.info("Invite declined: " + invite);
            } else {
                sendResponse(new ServerResponse(false, "No invite found with the given ID.", null));
                Logger.error("Failed to decline invite - no invite found with ID: " + invite.getId());
            }

        } catch (SQLException e) {
            Logger.error("Database error while declining invite: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Error while declining invite: " + e.getMessage(), null));
        }
    }

    private void handleGetInvites(User user) {
        String sql = "SELECT gi.id, gi.group_id, gi.invited_by, gi.invited_user, gi.status, " + "g.name AS group_name, ub.email AS sender_email, ur.email AS receiver_email " + "FROM group_invites gi " + "INNER JOIN groups g ON gi.group_id = g.id " + "INNER JOIN users ub ON gi.invited_by = ub.id " + "INNER JOIN users ur ON gi.invited_user = ur.id " + "WHERE gi.invited_user = ? AND gi.status = 'PENDING'";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, user.getId());

            ResultSet rs = statement.executeQuery();
            List<Invite> invites = new ArrayList<>();

            while (rs.next()) {
                Invite invite = new Invite(rs.getInt("id"), rs.getInt("group_id"), rs.getInt("invited_by"), rs.getInt("invited_user"), rs.getString("group_name"), rs.getString("sender_email"), rs.getString("receiver_email"), Invite.Status.valueOf(rs.getString("status")) // Enum de status
                );
                invites.add(invite);
            }

            sendResponse(new ServerResponse(true, "Pending invites retrieved successfully.", invites));

        } catch (SQLException e) {
            Logger.error("Database error while retrieving invites: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }

    private void handleAddExpense(Expense expense) {
        if (expense == null) {
            Logger.error("Expense is null.");
            sendResponse(new ServerResponse(false, "Expense data is missing.", null));
            return;
        }

        String insertSql = """
            INSERT INTO expenses (group_id, added_by, paid_by, amount, description, date, shared_with)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        String fetchPaidByNameSql = "SELECT name FROM users WHERE id = ?;";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement insertStatement = connection.prepareStatement(insertSql);
             PreparedStatement fetchPaidByNameStatement = connection.prepareStatement(fetchPaidByNameSql)) {

            if (!groupExists(connection, expense.getGroupId())) {
                Logger.error("Group does not exist. ID: " + expense.getGroupId());
                sendResponse(new ServerResponse(false, "Group does not exist.", null));
                return;
            }

            // Insert the expense into the database
            insertStatement.setInt(1, expense.getGroupId());
            insertStatement.setInt(2, expense.getAddedBy());
            insertStatement.setInt(3, expense.getPaidBy());
            insertStatement.setDouble(4, expense.getAmount());
            insertStatement.setString(5, expense.getDescription());
            insertStatement.setString(6, expense.getDate());
            insertStatement.setString(7, convertListToJson(expense.getSharedWith()));

            int rowsAffected = insertStatement.executeUpdate();
            updateVersion();
            String newSQL = "INSERT INTO expenses (group_id, added_by, paid_by, amount, description, date, shared_with) VALUES (" + expense.getGroupId() + ", " + expense.getAddedBy() + ", " + expense.getPaidBy() + ", " + expense.getAmount() + ", '" + expense.getDescription() + "', '" + expense.getDate() + "', '" + convertListToJson(expense.getSharedWith()) + "')";
            sendHeartbeat(newSQL, getVersion());

            if (rowsAffected > 0) {
                // Fetch the name of the person who paid
                fetchPaidByNameStatement.setInt(1, expense.getPaidBy());
                try (ResultSet resultSet = fetchPaidByNameStatement.executeQuery()) {
                    if (resultSet.next()) {
                        String paidByName = resultSet.getString("name");
                        expense.setPaidByName(paidByName);
                    } else {
                        Logger.error("User with ID " + expense.getPaidBy() + " not found.");
                        expense.setPaidByName("Unknown");
                    }
                }

                Logger.info("Expense added successfully: " + expense);
                sendResponse(new ServerResponse(true, "Expense added successfully.", expense));
            } else {
                Logger.error("Failed to add expense: " + expense);
                sendResponse(new ServerResponse(false, "Failed to add expense.", null));
            }

        } catch (SQLException e) {
            Logger.error("Database error while adding expense: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }

    private void handleGetExpenses(Integer groupId) {
        if (groupId == null) {
            Logger.error("Group ID is null.");
            sendResponse(new ServerResponse(false, "Group ID is missing.", null));
            return;
        }

        String expenseSql = """
        SELECT e.id, e.group_id, e.added_by, e.paid_by, e.amount, e.description, e.date, e.shared_with,
               u1.name AS added_by_name, u2.name AS paid_by_name
        FROM expenses e
        INNER JOIN users u1 ON e.added_by = u1.id
        INNER JOIN users u2 ON e.paid_by = u2.id
        WHERE e.group_id = ?;
    """;

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement statement = connection.prepareStatement(expenseSql)) {

            statement.setInt(1, groupId);
            ResultSet resultSet = statement.executeQuery();

            List<Expense> expenses = new ArrayList<>();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                int group_id = resultSet.getInt("group_id");
                int addedBy = resultSet.getInt("added_by");
                int paidBy = resultSet.getInt("paid_by");
                double amount = resultSet.getDouble("amount");
                String description = resultSet.getString("description");
                String date = resultSet.getString("date");
                String sharedWithJson = resultSet.getString("shared_with");
                String addedByName = resultSet.getString("added_by_name");
                String paidByName = resultSet.getString("paid_by_name");

                // Convert sharedWith JSON array to List<Integer>
                List<Integer> sharedWithIds = gson.fromJson(sharedWithJson, new TypeToken<List<Integer>>() {}.getType());

                // Fetch names of sharedWith users
                String sharedWithNames = fetchSharedWithNames(sharedWithIds, connection);

                expenses.add(new Expense(
                        id, group_id, addedBy, paidBy, amount, description, date, sharedWithIds,
                        addedByName, paidByName, sharedWithNames));
            }

            sendResponse(new ServerResponse(true, "Expenses fetched successfully.", expenses));
        } catch (SQLException e) {
            Logger.error("Database error while fetching expenses: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        } catch (Exception e) {
            Logger.error("Error processing GET_EXPENSES: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Error processing request: " + e.getMessage(), null));
        }
    }

    private String fetchSharedWithNames(List<Integer> userIds, Connection connection) {
        if (userIds == null || userIds.isEmpty()) {
            return "N/A";
        }

        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        String sql = "SELECT name FROM users WHERE id IN (" + placeholders + ")";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < userIds.size(); i++) {
                statement.setInt(i + 1, userIds.get(i));
            }

            ResultSet resultSet = statement.executeQuery();
            List<String> userNames = new ArrayList<>();

            while (resultSet.next()) {
                userNames.add(resultSet.getString("name"));
            }

            return String.join(", ", userNames);
        } catch (SQLException e) {
            Logger.error("Error fetching sharedWith names: " + e.getMessage());
            return "Error fetching names";
        }
    }

    private boolean groupExists(Connection connection, int groupId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM groups WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, groupId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        }
        return false;
    }

    private String convertListToJson(List<Integer> list) {
        return new Gson().toJson(list);
    }

    private void handleEditExpense(Expense expense) {
        if (expense == null) {
            Logger.error("Expense is null.");
            sendResponse(new ServerResponse(false, "Expense data is missing.", null));
            return;
        }

        String sql = """
            UPDATE expenses
            SET paid_by = ?, 
                amount = ?, 
                description = ?, 
                date = ?, 
                shared_with = ?
            WHERE id = ?;
            """;

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, expense.getPaidBy());
            statement.setDouble(2, expense.getAmount());
            statement.setString(3, expense.getDescription());
            statement.setString(4, expense.getDate());
            statement.setString(5, convertListToJson(expense.getSharedWith()));
            statement.setInt(6, expense.getId());

            int rowsAffected = statement.executeUpdate();
            updateVersion();
            String newSQL = "UPDATE expenses SET paid_by = " + expense.getPaidBy() + ", amount = " + expense.getAmount() + ", description = '" + expense.getDescription() + "', date = '" + expense.getDate() + "', shared_with = '" + convertListToJson(expense.getSharedWith()) + "' WHERE id = " + expense.getId();
            sendHeartbeat(newSQL, getVersion());

            if (rowsAffected > 0) {
                Logger.info("Expense updated successfully: " + expense);
                sendResponse(new ServerResponse(true, "Expense updated successfully.", null));
            } else {
                Logger.error("Failed to update expense: " + expense);
                sendResponse(new ServerResponse(false, "Failed to update expense.", null));
            }

        } catch (SQLException e) {
            Logger.error("Database error while updating expense: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }

    private void handleDeleteExpense(int expenseId) {
        String query = "DELETE FROM expenses WHERE id = ?";

        boolean isSuccess = false;
        String message;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, expenseId);
            stmt.executeUpdate();
            updateVersion();
            String newSQL = "DELETE FROM expenses WHERE id = " + expenseId;
            sendHeartbeat(newSQL, getVersion());

            isSuccess = true;
            message = "Expense deleted successfully.";
            Logger.info("Expense deleted from the database. ID: " + expenseId);

        } catch (SQLException e) {
            message = "Failed to delete expense.";
            Logger.error("Database error while deleting expense: " + e.getMessage());
        }

        sendResponse(new ServerResponse(isSuccess, message, null));
    }

    private int getVersion() {
        String url = "jdbc:sqlite:" + databasePath;
        String query = "SELECT version FROM version";
        int version = 0;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement stmt = conn.prepareStatement(query)) {
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

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement stmt = conn.prepareStatement(updateSQL)) {
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
        try (FileInputStream fis = new FileInputStream(databasePath); OutputStream os = clientSocket.getOutputStream()) {
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

    private void handleGetGroupUsers(Group group) {
        String sql = """
                    SELECT u.id, u.name, u.email, u.phone
                    FROM users u
                    INNER JOIN users_groups ug ON u.id = ug.user_id
                    WHERE ug.group_id = ?
                """;

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, group.getId());

            ResultSet resultSet = statement.executeQuery();

            List<User> users = new ArrayList<>();

            while (resultSet.next()) {
                User user = new User(resultSet.getInt("id"), resultSet.getString("name"), resultSet.getString("email"), resultSet.getString("phone"), null // Senha não é necessária aqui
                );
                users.add(user);
            }

            if (!users.isEmpty()) {
                sendResponse(new ServerResponse(true, "Users retrieved successfully.", users));
            } else {
                sendResponse(new ServerResponse(false, "No users found for the specified group.", null));
            }

        } catch (SQLException e) {
            Logger.error("Database error while retrieving users for group: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
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
        String query = "SELECT g.id, g.name, ug.user_id AS owner_id " + "FROM groups g " + "JOIN users_groups ug ON g.id = ug.group_id " + "WHERE ug.user_id = ?";

        boolean isSuccess = false;
        String message;
        List<Group> groups = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                groups.add(new Group(rs.getInt("id"), rs.getString("name"), rs.getInt("owner_id")));
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

    private void handleEditGroup(Group group) {
        String sql = "UPDATE groups SET name = ? WHERE id = ?";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, group.getName());
            statement.setInt(2, group.getId());

            int rowsAffected = statement.executeUpdate();
            updateVersion();
            String newSQL = "UPDATE groups SET name = '" + group.getName() + "' WHERE id = " + group.getId();
            sendHeartbeat(newSQL, getVersion());

            if (rowsAffected > 0) {
                Logger.info("Group name updated successfully: " + group);
                sendResponse(new ServerResponse(true, "Group name updated successfully.", null));
            } else {
                Logger.error("Failed to update group name: Group not found. ID: " + group.getId());
                sendResponse(new ServerResponse(false, "Failed to update group name: Group not found.", null));
            }

        } catch (SQLException e) {
            Logger.error("Database error while updating group name: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }

    private void handleRemoveGroup(Group group) {
        String checkDebtsSql = """
                    SELECT COUNT(*) AS debtCount 
                    FROM payments 
                    WHERE group_id = ? AND (amount > 0)
                """;
        String deleteGroupSql = "DELETE FROM groups WHERE id = ?";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            // Verifica se existem dívidas ou valores pendentes
            try (PreparedStatement checkStatement = connection.prepareStatement(checkDebtsSql)) {
                checkStatement.setInt(1, group.getId());
                ResultSet resultSet = checkStatement.executeQuery();

                if (resultSet.next() && resultSet.getInt("debtCount") > 0) {
                    Logger.error("Cannot remove group: Outstanding debts exist for group ID: " + group.getId());
                    sendResponse(new ServerResponse(false, "Cannot remove group: Outstanding debts exist.", null));
                    return;
                }
            }

            // Remove o grupo
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteGroupSql)) {
                deleteStatement.setInt(1, group.getId());

                int rowsAffected = deleteStatement.executeUpdate();
                updateVersion();
                String newSQL = "DELETE FROM groups WHERE id = " + group.getId();
                sendHeartbeat(newSQL, getVersion());

                if (rowsAffected > 0) {
                    Logger.info("Group removed successfully: " + group);
                    sendResponse(new ServerResponse(true, "Group removed successfully.", null));
                } else {
                    Logger.error("Failed to remove group: Group not found. ID: " + group.getId());
                    sendResponse(new ServerResponse(false, "Failed to remove group: Group not found.", null));
                }
            }
        } catch (SQLException e) {
            Logger.error("Database error while removing group: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }

    private String getGroupNamefromDB(int groupId) {
        String query = "SELECT name FROM groups WHERE id = ?";
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath + "/db.db"); PreparedStatement preparedStatement = connection.prepareStatement(query);) {
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
        String updateSQL = "UPDATE users SET name = ?, email = ?, phone = ?, password = ? WHERE id = ?";

        boolean isSuccess = false;
        String message;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement stmt = conn.prepareStatement(updateSQL)) {
            // Set the parameters for the query
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getPassword());
            stmt.setInt(5, user.getId());

            int rowsAffected = stmt.executeUpdate();
            updateVersion();
            updateSQL = "UPDATE users WHERE id = " + user.getId();
            sendHeartbeat(updateSQL, getVersion());
            if (rowsAffected > 0) {
                isSuccess = true;
                message = "User profile updated successfully";
                Logger.info("User profile updated successfully for email: " + user.getEmail());
            } else {
                message = "User not found!";
                Logger.error("No user found with ID: " + user.getId());
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
                retrievedUser = new User(0, rs.getString("name"), rs.getString("email"), rs.getString("phone"), null);
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

        if (user == null || user.getEmail() == null || user.getEmail().isBlank() || user.getPassword() == null || user.getPassword().isBlank()) {
            Logger.error("Invalid login credentials provided.");
            sendResponse(new ServerResponse(false, "Invalid login credentials.", null));
            return;
        }

        String url = "jdbc:sqlite:" + databasePath;
        String querySQL = "SELECT * FROM users WHERE email = ?";

        boolean isSuccess = false;
        String message;
        User authenticatedUser = null;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement stmt = conn.prepareStatement(querySQL)) {

            // Set the parameter for the query
            stmt.setString(1, user.getEmail());

            // Execute the query and check if a matching record exists
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if (rs.getString("password").equals(user.getPassword())) {
                    isSuccess = true;
                    authenticatedUser = new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("phone"));
                    Logger.info("User authenticated successfully: " + authenticatedUser.getEmail());
                    message = "User authenticated successfully";
                } else {
                    Logger.error("Authentication failed for email: " + user.getEmail());
                    message = "Authentication failed";
                }
            } else {
                Logger.error("Authentication failed for email: " + user.getEmail());
                message = "Authentication failed";
            }
        } catch (SQLException e) {
            Logger.error("Database error during authentication: " + e.getMessage());
            message = "An internal server error occurred.";
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
