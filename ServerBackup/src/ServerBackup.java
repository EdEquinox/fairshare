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
            if (folder.mkdirs()) {
                Logger.info("Backup folder created at: " + folder.getAbsolutePath());
            } else {
                Logger.error("Failed to create backup folder at: " + folder.getAbsolutePath());
                System.exit(1);
            }
        } else if (folder.listFiles().length > 0) {
            Logger.error("Backup folder is not empty. Please provide an empty folder.");
            System.exit(1);
        }

        // Start the backup server
        BackupService backupService = new BackupService(dbPath);
        backupService.start();

    }
}