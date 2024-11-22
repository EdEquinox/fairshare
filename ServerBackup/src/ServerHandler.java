import com.google.gson.Gson;
import utils.Logger;
import model.*;
import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ServerHandler implements Runnable {

    private final Socket serversocket;
    private final String databasePath;
    private BufferedReader in;
    private static final Gson gson = new Gson();

    public ServerHandler(Socket serversocket, String databasePath) {
        this.serversocket = serversocket;
        this.databasePath = databasePath;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(serversocket.getInputStream()));
            serversocket.setSoTimeout(30000);
            String command = in.readLine();
            if (command == null) {
                Logger.error("Main server is not alive.");
                stopBackupServer();
            }
            while ((command = in.readLine()) != null) {

                Message msg = gson.fromJson(command, Message.class);
                Logger.info("Received message: " + msg.toString());
                Logger.info("Message type: " + msg.type());

                switch (msg.type()) {
                    case Message.Type.HEARTBEAT:
                        Logger.info("Main server is alive.");
                        break;
                    case Message.Type.UPDATE_BACKUP:
                        String payload = gson.toJson(msg.payload());
                        updateDatabase(payload);
                        break;
                    case Message.Type.STOP_BACKUP:
                        stopBackupServer();
                        break;
                    default:
                        Logger.error("Invalid message type: " + msg.type());
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void updateDatabase(String payload) {
        // Update the database with the recieved query
        try(Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(payload);
            Logger.info("Database updated successfully.");
        } catch (Exception e) {
            Logger.error("Error updating database: " + e.getMessage());
        }
    }


    // Creates recieves database from main server
    public void createDatabase() {
        try (InputStream in = serversocket.getInputStream()) {

            File dbFile = new File(databasePath);
            // Recieve the database file
            try {
                // Create a new database file
                dbFile.createNewFile();
                Logger.info("Database file created at: " + dbFile.getAbsolutePath());

                // Read the database file from the socket
                try (FileOutputStream fos = new FileOutputStream(dbFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                Logger.info("Database file received successfully.");
            } catch (IOException e) {
                Logger.error("Error receiving database file: " + e.getMessage());
            }
        } catch (IOException e) {
            Logger.error("Error receiving database file: " + e.getMessage());
        }

    }

    public void stopBackupServer() {
        try {
            serversocket.close();
        } catch (IOException e) {
            Logger.error("Error stopping backup server: " + e.getMessage());
        }
    }

}
