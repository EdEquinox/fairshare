import utils.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerBackup {
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: java Server <backupDatabaseFolder_path>");
            System.exit(1);
        }

        String dbPath = args[0];

        // Check if folder is empty
        File folder = new File(dbPath);
        if (!folder.exists()) {
            Logger.error("Folder does not exist: " + dbPath);
            System.exit(1);
        }
        if (folder.isDirectory() && folder.list().length > 0) {
            Logger.error("Folder is not empty: " + dbPath);
            System.exit(1);
        }

        String databasePath = dbPath + "/backup.db";
        ServerSocket serverSocket = null;

        // Waits for the main server's haertbeat for 30 seconds using tcp
        try {
            serverSocket = new ServerSocket(0);
            serverSocket.setSoTimeout(30000);
        } catch (IOException e) {
            Logger.error("Error creating server socket: " + e.getMessage());
            System.exit(1);
        }

        // Start the backup server
        try {
            Logger.info("Backup server started on port " + serverSocket.getLocalPort());
            Logger.info("Database path: " + databasePath);
            Logger.info("Backup server running on ip: " + serverSocket.getInetAddress().getHostAddress());

            BackupService serverService = new BackupService(databasePath, serverSocket);
            serverService.startServer();

        } catch (IOException e) {
            Logger.error("Error starting backup server: " + e.getMessage());
            System.exit(1);
        }

    }
}