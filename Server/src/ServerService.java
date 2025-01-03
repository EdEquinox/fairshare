import com.google.gson.Gson;
import model.ServerResponse;
import utils.Logger;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.sql.*;
import java.util.List;

public class ServerService {
    private final String databasePath;
    private final ServerSocket serverSocket;
    private final String multicastAddress = "230.44.44.44";
    private final int multicastPort = 4444;
    private final int heartbeatInterval = 10000;
    private int version = 1;
    private final Gson gson = new Gson();
    private ServerRmiService serverRmiService;


    public ServerService(String databasePath, ServerSocket serverSocket) {
        this.databasePath = databasePath;
        this.serverSocket = serverSocket;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    /**
     * Starts the server and listens for incoming client connections.
     */
    public void startServer() throws IOException {
        Logger.info("Starting server with database path: " + databasePath);

        // Check if the database file already exists
        if (!doesDatabaseExist()) {
            Logger.info("Database does not exist. Creating a new database.");
            createNewDatabase();
        } else {
            Logger.info("Database already exists at: " + databasePath);
        }

        // Confirm that the database path is correctly initialized
        Logger.info("Using database located at: " + new File(databasePath).getAbsolutePath());

        new Thread(new HeartbeatSender(serverSocket.getLocalPort())).start();

        // Start listening for client connections
        while (true) {
            Socket clientSocket = serverSocket.accept();
            Logger.info("Client connected: " + clientSocket.getInetAddress().getHostAddress());

            ClientHandler clientHandler = new ClientHandler(clientSocket, databasePath, serverRmiService);
            new Thread(clientHandler).start();
        }
    }

    /**
     * Checks if the database exists at the specified path.
     * @return true if the database file exists, false otherwise.
     */
    private boolean doesDatabaseExist() {
        File dbFile = new File(databasePath);
        return dbFile.exists();
    }

    /**
     * Creates a new SQLite database with tables matching the model classes.
     */
    private void createNewDatabase() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            Logger.info("Creating tables in new database at: " + databasePath);

            // Enable Write-Ahead Logging for better concurrency
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL;");
            }

            // Create tables
            createTable(connection, "users", """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL
                );""");

            createTable(connection, "groups", """
                CREATE TABLE IF NOT EXISTS groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL
                );""");

            createTable(connection, "users_groups", """
                CREATE TABLE IF NOT EXISTS users_groups (
                    user_id INTEGER NOT NULL,
                    group_id INTEGER NOT NULL,
                    PRIMARY KEY(user_id, group_id),
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
                );""");

            createTable(connection, "group_invites", """
                CREATE TABLE IF NOT EXISTS group_invites (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    invited_by INTEGER NOT NULL,
                    invited_user INTEGER NOT NULL,
                    status TEXT CHECK(status IN ('PENDING', 'ACCEPTED', 'DENIED')) NOT NULL DEFAULT 'pending',
                    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE,
                    FOREIGN KEY(invited_by) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(invited_user) REFERENCES users(id) ON DELETE CASCADE
                );""");

            createTable(connection, "expenses", """
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    added_by INTEGER NOT NULL,
                    paid_by INTEGER NOT NULL,
                    amount REAL NOT NULL CHECK(amount > 0),
                    description TEXT,
                    date TEXT NOT NULL,
                    shared_with TEXT NOT NULL, -- JSON array of user IDs
                    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE,
                    FOREIGN KEY(added_by) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(paid_by) REFERENCES users(id) ON DELETE CASCADE
                );""");

            createTable(connection, "payments", """
                CREATE TABLE IF NOT EXISTS payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    from_user_id INTEGER NOT NULL,
                    to_user_id INTEGER NOT NULL,
                    amount REAL NOT NULL CHECK(amount > 0),
                    date TEXT NOT NULL,
                    group_id INTEGER NOT NULL,
                    FOREIGN KEY(from_user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(to_user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
                );""");

            createTable(connection, "version", """
                CREATE TABLE IF NOT EXISTS version (
                    version INTEGER NOT NULL
                );""");

            // Insert initial version if not present
            ensureVersionExists(connection);

            Logger.info("New database created successfully at: " + databasePath);
        } catch (Exception e) {
            Logger.error("Error creating new database: " + e.getMessage());
        }
    }

    /**
     * Helper method to create a table if it doesn't exist.
     */
    private void createTable(Connection connection, String tableName, String createSQL) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSQL);
            Logger.info("Created table: " + tableName);
        } catch (Exception e) {
            Logger.error("Error creating table " + tableName + ": " + e.getMessage());
        }
    }

    /**
     * Ensure the version table has an initial entry.
     */
    private void ensureVersionExists(Connection connection) {
        String checkVersionSQL = "SELECT COUNT(*) AS count FROM version;";
        String insertVersionSQL = "INSERT INTO version (version) VALUES (0);";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(checkVersionSQL)) {
            if (rs.next() && rs.getInt("count") == 0) {
                statement.executeUpdate(insertVersionSQL);
                Logger.info("Inserted initial version number 0 into version table.");
            }
        } catch (Exception e) {
            Logger.error("Error ensuring version entry: " + e.getMessage());
        }
    }

    public List<String> getUsers() {
        String query = "SELECT name FROM users";
        List<String> users = new java.util.ArrayList<>(List.of());
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String name;
                name = resultSet.getString("name");
                Logger.info("User: " + resultSet.getString("name"));
                users.add(name);
            }
            Logger.info("Users fetched successfully.");
            return users;
        } catch (Exception e) {
            Logger.error("Error fetching users: " + e.getMessage());
        }
        return List.of();
    }

    public List<String> getGroups() {
        String query = "SELECT name FROM groups";
        List<String> groups = new java.util.ArrayList<>(List.of());
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                groups.add(resultSet.getString("name"));
                Logger.info("Group: " + resultSet.getString("name"));
            }
            Logger.info("Groups fetched successfully.");
            return groups;
        } catch (Exception e) {
            Logger.error("Error fetching groups: " + e.getMessage());
        }
        return List.of();
    }

    private int getVersion() {
        String query = "SELECT version FROM version";
        try(Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath + "/db.db");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);) {
            version = resultSet.getInt("version");
            connection.close();

        } catch (Exception e) {
            Logger.error("Error getting version: " + e.getMessage());
        }
        return version;
    }


    /**
     * Stops the server and closes the server socket.
     */
    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                Logger.info("Server stopped.");
            }
        } catch (IOException e) {
            Logger.error("Error stopping server: " + e.getMessage());
        }
    }

    public void setServerRmiService(ServerRmiService serverRmiService) {
        this.serverRmiService = serverRmiService;
    }

    private class HeartbeatSender implements Runnable {
        private final int tcpPort;

        public HeartbeatSender(int tcpPort) {
            this.tcpPort = tcpPort;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress group = InetAddress.getByName(multicastAddress);

                while (true) {
                    int currentVersion = getVersion(); // Fetch the current version dynamically
                    String message = "HEARTBEAT, version " + currentVersion + ", port " + tcpPort;
                    ServerResponse response = new ServerResponse(true, message, null);
                    byte[] data = gson.toJson(response).getBytes();

                    DatagramPacket packet = new DatagramPacket(data, data.length, group, multicastPort);
                    socket.send(packet);

                    Logger.info("Heartbeat sent: " + message);
                    Thread.sleep(heartbeatInterval); // Send heartbeat every 10 seconds
                }
            } catch (Exception e) {
                Logger.error("Error sending heartbeat: " + e.getMessage());
            }
        }

        private int getVersion() {
            String query = "SELECT version FROM version";
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
                 PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                return preparedStatement.executeQuery().getInt("version");
            } catch (Exception e) {
                Logger.error("Error fetching version: " + e.getMessage());
                return -1;
            }
        }
    }


}