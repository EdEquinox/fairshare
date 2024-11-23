import utils.Logger;

import java.io.File;

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
            folder.mkdirs();
        }
        if (folder.isDirectory() && folder.list().length > 0) {
            Logger.error("Folder is not empty: " + dbPath);
            System.exit(1);
        }

        // Start the server
        BackupService.initialize(dbPath);

    }
}