import model.Message;
import model.ServerResponse;
import utils.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class BackupService {
    private final String databasePath;
    private static BackupService instance;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isConnected = false;

    final static int PORT = 4444;
    final static int TIMEOUT = 30000;

    public BackupService(String databasePath) {
        this.databasePath = databasePath;
    }

    public static BackupService getInstance(String databasePath) {
        if (instance == null) {
            throw new IllegalStateException("BackupService not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static void initialize(String dbPath) {
        if (instance == null) {
            instance = new BackupService(dbPath);
        } else {
            throw new IllegalStateException("BackupService has already been initialized.");
        }
    }

    public boolean connectToServer() {
        try {
            socket = new Socket("127.0.0.1",PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            Logger.info("Connected to server at " + socket.getInetAddress());
            isConnected = true;
            return true;
        } catch (IOException e) {
            Logger.error("Error starting backup server: " + e.getMessage());
        }
        return false;
    }

    private ServerResponse sendRequest(Message request) {
        if (!isConnected) {
            Logger.error("Not connected to server. Cannot send request.");
            return null;
        }

        try {
            out.println(request);
            String response = in.readLine();
            return ServerResponse.fromString(response);
        } catch (IOException e) {
            Logger.error("Error sending request to server: " + e.getMessage());
        }
        return null;
    }

    private ServerResponse recieveRequest() {
        if (!isConnected) {
            Logger.error("Not connected to server. Cannot recieve request.");
            return null;
        }

        try {
            String request = in.readLine();
            return ServerResponse.fromString(request);
        } catch (IOException e) {
            Logger.error("Error recieving request from server: " + e.getMessage());
        }
        return null;
    }

    public void startServer() throws IOException {
        Logger.info("Backup server started on port " + serverSocket.getLocalPort());

        if (!doesDatabaseExist()) {
            Logger.info("Database does not exist. Creating a new database.");
            createNewDatabase();
        } else {
            Logger.info("Database already exists at: " + databasePath);
        }

        Logger.info("Database created at: " + databasePath);

        Socket backupSocket = serverSocket.accept();
        Logger.info("Received connection from " + backupSocket.getInetAddress());
        new ServerHandler(backupSocket, databasePath).run();

    }

    private void createNewDatabase() {
        // Recieve the database file from the main server
        try {
            Socket backupSocket = serverSocket.accept();
            Logger.info("Received connection to recieve database from " + backupSocket.getInetAddress());
            new ServerHandler(backupSocket, databasePath).createDatabase();
        } catch (IOException e) {
            Logger.error("Error receiving database file: " + e.getMessage());
        }
    }

    private boolean doesDatabaseExist() {
        File dbFile = new File(databasePath);
        return dbFile.exists();
    }
}
