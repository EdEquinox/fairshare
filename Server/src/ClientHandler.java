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
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final String databasePath;
    private static final Gson gson = new Gson();
    private final static String MULTICAST_ADDRESS = "230.44.44.44";
    private final static int MULTICAST_PORT = 4444;
    private int version;
    ServerRmiService serverRmiService;

    public ClientHandler(Socket clientSocket, String databasePath , ServerRmiService serverRmiService) {
        this.clientSocket = clientSocket;
        this.databasePath = databasePath;
        this.serverRmiService = serverRmiService;
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
                        case Message.Type.GET_GROUP_STATS -> {
                            try {
                                Map<String, Number> payload = gson.fromJson(gson.toJson(message.payload()), new TypeToken<Map<String, Number>>() {
                                }.getType());
                                int groupId = payload.get("groupId").intValue(); // Garantindo que seja convertido para int
                                int userId = payload.get("userId").intValue();
                                handleGetGroupStats(groupId, userId);
                            } catch (Exception e) {
                                Logger.error("Error deserializing GET_GROUP_STATS payload: " + e.getMessage());
                                sendResponse(new ServerResponse(false, "Invalid payload format for GET_GROUP_STATS.", null));
                            }
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
                        case Message.Type.ADD_PAYMENT -> {
                            Payment payment = gson.fromJson(gson.toJson(message.payload()), Payment.class);
                            handleAddPayment(payment);
                        }
                        case Message.Type.EDIT_PAYMENT -> {
                            Payment payment = gson.fromJson(gson.toJson(message.payload()), Payment.class);
                            handleEditPayment(payment);
                        }
                        case Message.Type.DELETE_PAYMENT -> {
                            int paymentId = gson.fromJson(gson.toJson(message.payload()), Integer.class);
                            handleDeletePayment(paymentId);
                        }
                        case Message.Type.GET_PAYMENTS -> {
                            int groupId = gson.fromJson(gson.toJson(message.payload()), Integer.class);
                            handleGetPayments(groupId);
                        }
                        case Message.Type.GET_USERS_RMI -> {
                            Logger.info("Fetching users...");
                            handleGetUsers();
                        }
                        case Message.Type.GET_GROUPS_RMI -> {
                            Logger.info("Fetching groups...");
                            handleGetGroupsRmi();
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
            // Close the client socket if open
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    Logger.info("Client connection closed.");
                }
            } catch (IOException e) {
                Logger.error("Error closing client connection: " + e.getMessage());
            }

            // Remove this client's writer from the server's list of connected writers
            if (out != null) {
                Server.removeClient(out);
                Logger.info("Client writer removed from the server.");
            }
        }

    }

    private void handleGetGroupStats(int groupId, int userId) {
        String totalSpentSql = """
            SELECT IFNULL(SUM(amount), 0) AS totalSpent
            FROM expenses
            WHERE group_id = ?;
        """;

        String totalToPaySql = """
            SELECT IFNULL(SUM(amount / (
                SELECT COUNT(*)
                FROM users_groups
                WHERE group_id = expenses.group_id AND user_id IN (SELECT value FROM json_each(expenses.shared_with))
            )), 0) AS totalToPay
            FROM expenses
            WHERE group_id = ? AND ? IN (SELECT value FROM json_each(shared_with));
        """;

        String totalToReceiveSql = """
            SELECT IFNULL(SUM(amount), 0) AS totalToReceive
            FROM payments
            WHERE group_id = ? AND to_user_id = ?;
        """;

        String fetchExpensesSql = """
            SELECT e.id, e.group_id, e.added_by, e.paid_by, e.amount, e.description, e.date, e.shared_with,
                   u1.name AS paid_by_name
            FROM expenses e
            INNER JOIN users u1 ON e.paid_by = u1.id
            WHERE e.group_id = ?;
        """;

        String fetchPaymentsSql = """
            SELECT p.id, p.group_id, p.from_user_id, p.to_user_id, p.amount, p.date,
                   u1.name AS paid_by_name, u2.name AS received_by_name
            FROM payments p
            INNER JOIN users u1 ON p.from_user_id = u1.id
            INNER JOIN users u2 ON p.to_user_id = u2.id
            WHERE p.group_id = ?;
        """;

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            double totalSpent = 0;
            double totalToPay = 0;
            double totalToReceive = 0;
            List<Expense> expenses = new ArrayList<>();
            List<Payment> payments = new ArrayList<>();

            // Calculate total spent
            try (PreparedStatement statement = connection.prepareStatement(totalSpentSql)) {
                statement.setInt(1, groupId);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    totalSpent = resultSet.getDouble("totalSpent");
                }
            }

            // Calculate total to pay
            try (PreparedStatement statement = connection.prepareStatement(totalToPaySql)) {
                statement.setInt(1, groupId);
                statement.setInt(2, userId);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    totalToPay = resultSet.getDouble("totalToPay");
                }
            }

            // Calculate total to receive
            try (PreparedStatement statement = connection.prepareStatement(totalToReceiveSql)) {
                statement.setInt(1, groupId);
                statement.setInt(2, userId);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    totalToReceive = resultSet.getDouble("totalToReceive");
                }
            }

            // Fetch expenses
            try (PreparedStatement statement = connection.prepareStatement(fetchExpensesSql)) {
                statement.setInt(1, groupId);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    Expense expense = new Expense(
                            resultSet.getInt("id"),
                            resultSet.getInt("group_id"),
                            resultSet.getInt("paid_by"),
                            resultSet.getInt("added_by"),
                            resultSet.getDouble("amount"),
                            resultSet.getString("description"),
                            resultSet.getString("date"),
                            gson.fromJson(resultSet.getString("shared_with"), new TypeToken<List<Integer>>() {}.getType())
                    );
                    expense.setPaidByName(resultSet.getString("paid_by_name")); // Nome do pagador
                    expenses.add(expense);
                }
            }

            // Fetch payments
            try (PreparedStatement statement = connection.prepareStatement(fetchPaymentsSql)) {
                statement.setInt(1, groupId);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    Payment payment = new Payment(
                            resultSet.getInt("id"),
                            resultSet.getInt("group_id"),
                            resultSet.getInt("from_user_id"),
                            resultSet.getInt("to_user_id"),
                            resultSet.getDouble("amount"),
                            resultSet.getString("date")
                    );
                    payment.setPaidByName(resultSet.getString("paid_by_name")); // Nome do pagador
                    payment.setReceivedByName(resultSet.getString("received_by_name")); // Nome do destinatário
                    payments.add(payment);
                }
            }

            // Send response
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSpent", totalSpent);
            stats.put("totalToPay", totalToPay);
            stats.put("totalToReceive", totalToReceive);
            stats.put("expenses", expenses);
            stats.put("payments", payments);

            sendResponse(new ServerResponse(true, "Group stats fetched successfully.", stats));

        } catch (SQLException e) {
            Logger.error("Database error while fetching group stats: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        } catch (Exception e) {
            Logger.error("Error processing group stats: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Error processing group stats: " + e.getMessage(), null));
        }
    }

    private void handleGetGroupsRmi() {
        List<String> groups = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT name FROM groups");
            while (resultSet.next()) {
                groups.add(resultSet.getString("name"));
            }
            sendResponse(new ServerResponse(true, "Groups retrieved successfully.", groups));
        } catch (SQLException e) {
            Logger.error("Database error while retrieving groups: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }

    private void handleGetUsers() {
        String sql = "SELECT id, name, email, phone FROM users";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(sql)) {

            ResultSet resultSet = statement.executeQuery();
            List<User> users = new ArrayList<>();

            while (resultSet.next()) {
                User user = new User(resultSet.getInt("id"), resultSet.getString("name"), resultSet.getString("email"), resultSet.getString("phone"), null // Senha não é necessária aqui
                );
                users.add(user);
            }

            List<String> usersList = new ArrayList<>();
            for (User user : users) {
                usersList.add(user.getName());
            }

            sendResponse(new ServerResponse(true, "Users retrieved successfully.", usersList));

        } catch (SQLException e) {
            Logger.error("Database error while retrieving users: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
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

                    // Broadcast update to all connected clients
                    Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "Invite created", invite)));
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
            connection.setAutoCommit(false); // Start the transaction

            try (PreparedStatement updateInviteStmt = connection.prepareStatement(updateInviteSQL);
                 PreparedStatement addUserToGroupStmt = connection.prepareStatement(addUserToGroupSQL)) {

                // Update the status of the invite to "ACCEPTED"
                updateInviteStmt.setString(1, Invite.Status.ACCEPTED.name());
                updateInviteStmt.setInt(2, invite.getId());
                updateInviteStmt.executeUpdate();
                updateVersion();
                String newSQL = "UPDATE group_invites SET status = 'ACCEPTED' WHERE id = " + invite.getId();
                sendHeartbeat(newSQL, getVersion());

                // Add the user to the group
                addUserToGroupStmt.setInt(1, invite.getReceiverId());
                addUserToGroupStmt.setInt(2, invite.getGroupId());
                addUserToGroupStmt.executeUpdate();
                updateVersion();
                newSQL = "INSERT INTO users_groups (user_id, group_id) VALUES (" + invite.getReceiverId() + ", " + invite.getGroupId() + ")";
                sendHeartbeat(newSQL, getVersion());

                connection.commit(); // Commit the transaction

                // Send response to the current client
                sendResponse(new ServerResponse(true, "Invite accepted successfully.", null));
                Logger.info("Invite accepted and user added to group: " + invite);

                // Broadcast update to all connected clients
                Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "Invite accepted", invite)));

            } catch (SQLException e) {
                connection.rollback(); // Roll back the transaction in case of error
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

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement statement = connection.prepareStatement(updateInviteSQL)) {

            // Update the status of the invite to "DENIED"
            statement.setString(1, Invite.Status.DENIED.name());
            statement.setInt(2, invite.getId());
            int rowsAffected = statement.executeUpdate();
            updateVersion();
            String newSQL = "UPDATE group_invites SET status = 'DENIED' WHERE id = " + invite.getId();
            sendHeartbeat(newSQL, getVersion());

            if (rowsAffected > 0) {
                // Send response to the client
                sendResponse(new ServerResponse(true, "Invite declined successfully.", null));
                Logger.info("Invite declined: " + invite);

                // Broadcast update to all connected clients
                Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "Invite declined", invite)));
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

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement insertStatement = connection.prepareStatement(insertSql); PreparedStatement fetchPaidByNameStatement = connection.prepareStatement(fetchPaidByNameSql)) {

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
            String newSQL = "INSERT INTO expenses (group_id, added_by, paid_by, amount, description, date, shared_with) VALUES ("
                    + expense.getGroupId() + ", " + expense.getAddedBy() + ", " + expense.getPaidBy() + ", "
                    + expense.getAmount() + ", '" + expense.getDescription() + "', '" + expense.getDate()
                    + "', '" + convertListToJson(expense.getSharedWith()) + "')";
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
                
                serverRmiService.addExpense("despesa", expense.getAmount());
                sendResponse(new ServerResponse(true, "Expense added successfully.", null));
                
                // Broadcast update to all clients
                Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "New expense added", expense)));
            } else {
                Logger.error("Failed to add expense: " + expense);
                sendResponse(new ServerResponse(false, "Failed to add expense.", null));
            }

        } catch (Exception e) {
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

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(expenseSql)) {

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
                List<Integer> sharedWithIds = gson.fromJson(sharedWithJson, new TypeToken<List<Integer>>() {
                }.getType());

                // Fetch names of sharedWith users
                String sharedWithNames = fetchSharedWithNames(sharedWithIds, connection);

                expenses.add(new Expense(id, group_id, addedBy, paidBy, amount, description, date, sharedWithIds, addedByName, paidByName, sharedWithNames));
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

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, expense.getPaidBy());
            statement.setDouble(2, expense.getAmount());
            statement.setString(3, expense.getDescription());
            statement.setString(4, expense.getDate());
            statement.setString(5, convertListToJson(expense.getSharedWith()));
            statement.setInt(6, expense.getId());

            int rowsAffected = statement.executeUpdate();
            updateVersion();
            String newSQL = "UPDATE expenses SET paid_by = " + expense.getPaidBy() + ", amount = " + expense.getAmount()
                    + ", description = '" + expense.getDescription() + "', date = '" + expense.getDate()
                    + "', shared_with = '" + convertListToJson(expense.getSharedWith())
                    + "' WHERE id = " + expense.getId();
            sendHeartbeat(newSQL, getVersion());

            if (rowsAffected > 0) {
                Logger.info("Expense updated successfully: " + expense);
                sendResponse(new ServerResponse(true, "Expense updated successfully.", expense));

                // Broadcast update to all clients
                Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "Expense updated", expense)));
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

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, expenseId);
            int rowsAffected = stmt.executeUpdate();

            isSuccess = true;
            message = "Expense deleted successfully.";
            serverRmiService.removeExpense(expenseId);
            Logger.info("Expense deleted from the database. ID: " + expenseId);

        } catch (Exception e) {
            message = "Failed to delete expense.";
            Logger.error("Database error while deleting expense: " + e.getMessage());
        }

        sendResponse(new ServerResponse(isSuccess, message, null));
    }


    private void handleAddPayment(Payment payment) {
        if (payment == null) {
            Logger.error("Payment is null.");
            sendResponse(new ServerResponse(false, "Payment data is missing.", null));
            return;
        }

        String insertSql = """
                    INSERT INTO payments (group_id, from_user_id, to_user_id, amount, date)
                    VALUES (?, ?, ?, ?, ?);
                """;

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {

            // Add the new payment to the database
            insertStatement.setInt(1, payment.getGroupId());
            insertStatement.setInt(2, payment.getPaidBy());
            insertStatement.setInt(3, payment.getReceivedBy());
            insertStatement.setDouble(4, payment.getAmount());
            insertStatement.setString(5, payment.getDate());

            int rowsAffected = insertStatement.executeUpdate();
            if (rowsAffected == 0) {
                Logger.error("Failed to add payment to the database.");
                sendResponse(new ServerResponse(false, "Failed to add payment.", null));
                return;
            }

            // Fetch the names of users involved
            String paidByName = fetchSharedWithNames(Collections.singletonList(payment.getPaidBy()), connection);
            String receivedByName = fetchSharedWithNames(Collections.singletonList(payment.getReceivedBy()), connection);

            if (paidByName.equals("Error fetching names") || receivedByName.equals("Error fetching names")) {
                Logger.error("Error fetching user names for payment.");
                sendResponse(new ServerResponse(false, "Error fetching user names for payment.", null));
                return;
            }

            payment.setPaidByName(paidByName);
            payment.setReceivedByName(receivedByName);

            // Update the version and broadcast the new payment
            updateVersion();
            String newSQL = """
            INSERT INTO payments (group_id, from_user_id, to_user_id, amount, date) 
            VALUES (%d, %d, %d, %.2f, '%s')
        """.formatted(payment.getGroupId(), payment.getPaidBy(), payment.getReceivedBy(), payment.getAmount(), payment.getDate());
            sendHeartbeat(newSQL, getVersion());

            Logger.info("Payment added successfully: " + payment);
            sendResponse(new ServerResponse(true, "Payment added successfully.", payment));

            // Broadcast the update
            Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "New payment added", payment)));
        } catch (SQLException e) {
            Logger.error("Database error while adding payment: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }


    private void handleEditPayment(Payment payment) {
        if (payment == null) {
            Logger.error("Payment is null.");
            sendResponse(new ServerResponse(false, "Payment data is missing.", null));
            return;
        }

        String sql = """
                    UPDATE payments
                    SET date = ?, amount = ?, from_user_id = ?, to_user_id = ?
                    WHERE id = ?;
                """;

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(sql)) {

            // Update payment data in the database
            statement.setString(1, payment.getDate());
            statement.setDouble(2, payment.getAmount());
            statement.setInt(3, payment.getPaidBy());
            statement.setInt(4, payment.getReceivedBy());
            statement.setInt(5, payment.getId());

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated > 0) {
                Logger.info("Payment updated successfully: " + payment);

                // Update the version and send a heartbeat
                updateVersion();
                String newSQL = """
                UPDATE payments SET date = '%s', amount = %.2f, from_user_id = %d, to_user_id = %d 
                WHERE id = %d
            """.formatted(payment.getDate(), payment.getAmount(), payment.getPaidBy(), payment.getReceivedBy(), payment.getId());
                sendHeartbeat(newSQL, getVersion());

                // Respond to the client
                sendResponse(new ServerResponse(true, "Payment updated successfully.", payment));

                // Broadcast the update to all clients
                Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "Payment updated", payment)));
            } else {
                Logger.error("Failed to update payment: " + payment);
                sendResponse(new ServerResponse(false, "Failed to update payment.", null));
            }
        } catch (SQLException e) {
            Logger.error("Database error while updating payment: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        } catch (Exception e) {
            Logger.error("Error processing EDIT_PAYMENT: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Error processing request: " + e.getMessage(), null));
        }
    }


    private void handleDeletePayment(int paymentId) {
        if (paymentId <= 0) {
            Logger.error("Invalid Payment ID.");
            sendResponse(new ServerResponse(false, "Invalid Payment ID.", null));
            return;
        }

        String sql = "DELETE FROM payments WHERE id = ?";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, paymentId);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                Logger.info("Payment deleted successfully: ID " + paymentId);

                // Update version and send heartbeat
                updateVersion();
                String newSQL = "DELETE FROM payments WHERE id = " + paymentId;
                sendHeartbeat(newSQL, getVersion());

                // Respond to the client
                sendResponse(new ServerResponse(true, "Payment deleted successfully.", null));

                // Broadcast update to all clients
                Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "Payment deleted", paymentId)));
            } else {
                Logger.error("Failed to delete payment: ID " + paymentId);
                sendResponse(new ServerResponse(false, "Payment not found or already deleted.", null));
            }

        } catch (SQLException e) {
            Logger.error("Database error while deleting payment: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        }
    }


    private void handleGetPayments(Integer groupId) {
        if (groupId == null) {
            Logger.error("Group ID is null.");
            sendResponse(new ServerResponse(false, "Group ID is missing.", null));
            return;
        }

        String sql = """
                    SELECT p.id, p.group_id, p.from_user_id, p.to_user_id, p.amount, p.date,
                           u1.name AS paid_by_name, u2.name AS received_by_name
                    FROM payments p
                    INNER JOIN users u1 ON p.from_user_id = u1.id
                    INNER JOIN users u2 ON p.to_user_id = u2.id
                    WHERE p.group_id = ?;
                """;

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, groupId);
            ResultSet resultSet = statement.executeQuery();

            List<Payment> payments = new ArrayList<>();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                int group_id = resultSet.getInt("group_id");
                int paidBy = resultSet.getInt("from_user_id");
                int receivedBy = resultSet.getInt("to_user_id");
                double amount = resultSet.getDouble("amount");
                String date = resultSet.getString("date");
                String paidByName = resultSet.getString("paid_by_name");
                String receivedByName = resultSet.getString("received_by_name");

                payments.add(new Payment(id, group_id, paidBy, receivedBy, amount, date, paidByName, receivedByName));
            }

            sendResponse(new ServerResponse(true, "Payments fetched successfully.", payments));
        } catch (SQLException e) {
            Logger.error("Database error while fetching payments: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Database error: " + e.getMessage(), null));
        } catch (Exception e) {
            Logger.error("Error processing GET_PAYMENTS request: " + e.getMessage());
            sendResponse(new ServerResponse(false, "Error processing request: " + e.getMessage(), null));
        }
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

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, group.getName());
            statement.setInt(2, group.getId());

            int rowsAffected = statement.executeUpdate();
            updateVersion();

            // Broadcast update if the group name was successfully changed
            if (rowsAffected > 0) {
                Logger.info("Group name updated successfully: " + group);

                // Broadcast the update to all clients
                Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "Group name updated successfully", group)));

                // Send heartbeat for replication
                String newSQL = "UPDATE groups SET name = '" + group.getName() + "' WHERE id = " + group.getId();
                sendHeartbeat(newSQL, getVersion());

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
            // Check for outstanding debts
            try (PreparedStatement checkStatement = connection.prepareStatement(checkDebtsSql)) {
                checkStatement.setInt(1, group.getId());
                ResultSet resultSet = checkStatement.executeQuery();

                if (resultSet.next() && resultSet.getInt("debtCount") > 0) {
                    Logger.error("Cannot remove group: Outstanding debts exist for group ID: " + group.getId());
                    sendResponse(new ServerResponse(false, "Cannot remove group: Outstanding debts exist.", null));
                    return;
                }
            }

            // Remove the group
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteGroupSql)) {
                deleteStatement.setInt(1, group.getId());

                int rowsAffected = deleteStatement.executeUpdate();
                updateVersion();

                if (rowsAffected > 0) {
                    Logger.info("Group removed successfully: " + group);

                    // Send heartbeat for replication
                    String newSQL = "DELETE FROM groups WHERE id = " + group.getId();
                    sendHeartbeat(newSQL, getVersion());

                    // Broadcast the removal to all clients
                    Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "Group removed", group)));

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
            updateSQL = "UPDATE users SET name = '" + user.getName() + "', email = '" + user.getEmail() +
                    "', phone = '" + user.getPhone() + "', password = '" + user.getPassword() + "' WHERE id = " + user.getId();
            sendHeartbeat(updateSQL, getVersion());

            if (rowsAffected > 0) {
                isSuccess = true;
                message = "User profile updated successfully";
                Logger.info("User profile updated successfully for email: " + user.getEmail());

                // Broadcast the profile update to all clients
                Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "User profile updated", user)));
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
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, user.getName());
                insertStmt.setString(2, user.getEmail());
                insertStmt.setString(3, user.getPhone());
                insertStmt.setString(4, user.getPassword());
                insertStmt.executeUpdate();

                // Get the generated user ID
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int newUserId = generatedKeys.getInt(1);
                    user.setId(newUserId);  // Update the user object with the new ID
                }

                updateVersion();
                sendHeartbeat(insertSQL, getVersion());
                isSuccess = true;
                message = "User registered successfully";
                serverRmiService.registerUser(user.getEmail());
                Logger.info("User added successfully to the database: " + user.getName());

                // Broadcast the registration to all clients with the full user details
                Server.broadcastUpdate(gson.toJson(new ServerResponse(true, "New user registered", user)));
            }

        } catch (SQLException e) {
            message = "Error while creating a new user";
            Logger.error("Database error: " + e.getMessage());
        }

        ServerResponse response = new ServerResponse(isSuccess, message, user); // Send the full user details in the payload
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
                    serverRmiService.loggedInUser(user.getEmail());
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
