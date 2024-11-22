import java.io.*;
import java.net.Socket;

public class BackupHandler implements Runnable{

    private final String databasePath;
    private final Socket backupSocket;
    private OutputStream out;
    private boolean initialized = false;


    public BackupHandler(String databasePath, Socket backupSocket) {
        this.databasePath = databasePath;
        this.backupSocket = backupSocket;
    }

    @Override
    public void run() {
        if (!initialized) {
            // Send the database file to the backup server
            sendDatabaseFile();
        }
        // TODO Send heartbeat messages to the backup server



    }

    private void sendDatabaseFile() {
        try {
            out = backupSocket.getOutputStream();
            File dbFile = new File(databasePath);
            if (!dbFile.exists()) {
                throw new RuntimeException("Database file does not exist at: " + databasePath);
            } else {
                // Send the database file to the backup server
                byte[] buffer = new byte[1024];
                // Read the database file and write it to the output stream
                FileInputStream fis = new FileInputStream(dbFile);
                // Wrap the FileInputStream in a BufferedInputStream for better performance
                BufferedInputStream bis = new BufferedInputStream(fis);
                // Read the file in chunks of 1024 bytes
                int bytesRead;
                // Write the file to the output stream
                while ((bytesRead = bis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                // Flush the output stream to ensure all data is sent
                out.flush();
                bis.close();
                fis.close();
                initialized = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
