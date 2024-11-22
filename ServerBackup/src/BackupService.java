import utils.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BackupService {
    private final String databasePath;
    private final ServerSocket serverSocket;

    public BackupService(String databasePath, ServerSocket serverSocket) {
        this.databasePath = databasePath;
        this.serverSocket = serverSocket;
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
