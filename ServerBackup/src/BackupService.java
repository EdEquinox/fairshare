import com.google.gson.Gson;
import model.ServerResponse;
import utils.Logger;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;

public class BackupService {

    private static final Gson gson = new Gson();
    private final String multicastAddress = "230.44.44.44";
    private final int multicastPort = 4444;
    private final int timeout = 30000;
    private int version = 1;
    private final String databasePath;
    private static BackupService instance;
    private Socket socket;
    private InputStream in;
    private PrintWriter out;


    public BackupService(String databasePath) {
        this.databasePath = databasePath;
    }

    public void start() {

        try {
            socket = new Socket("127.0.0.1", 8000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Thread(new HeartbeatReciever()).start();

    }

    private void updateDatabase(String query) {
        try(Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath + "/db.db");
            PreparedStatement preparedStatement = connection.prepareStatement(query);) {
            preparedStatement.executeUpdate();

            connection.close();
            Logger.info("Database updated successfully.");
        } catch (Exception e) {
            Logger.error("Error updating database: " + e.getMessage());
        }
    }

    private void stopBackupServer() {
        closeConnection();
        System.exit(0);
    }

    private void getDatabaseFile() {
        try(Socket socket = new Socket("127.0.0.1", 8000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            InputStream in = socket.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(databasePath + "/db.db");) {

            out.println("GET_DATABASE_FILE");
            Logger.info("Requesting database file from main server.");

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                Logger.info("bytesRead: " + bytesRead);
            }
            Logger.info("buffer: " + buffer.length);
            Logger.info("Database file received successfully.");
        } catch (IOException e) {
            Logger.error("Error writing database file: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.error("Error closing connection: " + e.getMessage());
        }
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    private void getMainServerVersion() {
        String query = "SELECT version FROM version";
        try(Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath + "/db.db");
            PreparedStatement preparedStatement = connection.prepareStatement(query);) {
            version = preparedStatement.executeQuery().getInt("version");
            connection.close();
        } catch (Exception e) {
            Logger.error("Error getting main server version: " + e.getMessage());
        }
    }

    private class HeartbeatReciever implements Runnable {

        private boolean first = true;

        @Override
        public void run() {
            try(MulticastSocket socket = new MulticastSocket(multicastPort)) {
                InetAddress group = InetAddress.getByName(multicastAddress);
                socket.joinGroup(group);
                socket.setSoTimeout(timeout);

                if (first) {
                    getDatabaseFile();
                    getMainServerVersion();
                    first = false;
                    Logger.info("Backup server started.");
                }

                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    ServerResponse response = gson.fromJson(new String(packet.getData(), 0, packet.getLength()), ServerResponse.class);

                    if (response.payload()==null){
                        Logger.info("Received heartbeat from server with message: " + response.message());
                    } else {
                        if (response.isSuccess()) {
                            Logger.info("Main server is alive.");
                            ArrayList payload = gson.fromJson(gson.toJson(response.payload()), ArrayList.class);
                            String query = gson.fromJson(gson.toJson(payload.get(0)), String.class);
                            int mainVersion = gson.fromJson(gson.toJson(payload.get(1)), Integer.class);
                            Logger.info("Query: " + query);
                            if (query == null && version == getVersion()+1) {
                                Logger.error("query is null and version is not updated.");
                                System.exit(0);
                            } else if (query != null && version != getVersion()) {
                                Logger.info("version: " + version + " this.version: " + getVersion());
                                Logger.error("query is not null and version is not updated.");
                                System.exit(0);
                            } else if (query != null && version == getVersion()) {
                                setVersion(mainVersion);
                                updateDatabase(query);
                            } else if (query == null && version == getVersion()) {
                                Logger.info("Nothing to update.");
                            } else {
                                Logger.error("Unknown error.");
                                System.exit(0);
                            }
                        } else {
                            Logger.error("Main server is not alive.");
                            stopBackupServer();
                        }
                    }
                }
            } catch (Exception e) {
                Logger.error("Error listening for heartbeat: " + e.getMessage());
            }
        }
    }

}
