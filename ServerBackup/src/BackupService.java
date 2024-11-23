import com.google.gson.Gson;
import model.Message;
import model.ServerResponse;
import org.apache.commons.logging.Log;
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
    private final String databasePath;
    private static BackupService instance;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isConnected = false;
    private boolean begin = true;
    private int version = 0;


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

            instance.connectToServer();


        } else {
            throw new IllegalStateException("BackupService has already been initialized.");
        }
    }

    public boolean connectToServer() {
        try {
            socket = new Socket("127.0.0.1",8000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Logger.info("Connected to server at " + socket.getInetAddress());
            isConnected = true;

            if (instance.begin) {
                ServerResponse response = instance.sendRequest(new Message(Message.Type.BACKUP_INIT, "Backup"));
                if (response.isSuccess()) {
                    instance.getDatabaseFile(response.payload());
                    instance.begin = false;
                } else {
                    Logger.error("Failed to initialize backup server.");
                }
            }

            new Thread(new HeartbeatReciever()).start();

            return false;
        } catch (IOException e) {
            Logger.error("Error starting backup server: " + e.getMessage());
        }
        isConnected = false;
        return true;
    }

    private void litenForHeartbeat() {
        Logger.info("Connected to server at " + socket.getInetAddress());
        while(isConnected) {
            Logger.info("Listening for heartbeat again.");
            try {
                String message = in.readLine();
                if (message != null) {
                    Logger.info("Received heartbeat from server.");
                    ServerResponse response = gson.fromJson(message, ServerResponse.class);
                    if (response.isSuccess()) {
                        Logger.info("Main server is alive.");
                        ArrayList payload = gson.fromJson(gson.toJson(response.payload()), ArrayList.class);
                        String query = gson.fromJson(gson.toJson(payload.get(0)), String.class);
                        int version = gson.fromJson(gson.toJson(payload.get(1)), Integer.class);
                        if (query == null && version == this.version+1) {
                            Logger.info("Something went wrong.");
                            System.exit(0);
                        } else if (query != null && version != this.version+1) {
                            Logger.error("Something went wrong.");
                            System.exit(0);
                        } else if (query != null && version == this.version+1) {
                            this.version = version;
                            updateDatabase(query);
                        } else {
                            Logger.info("Nothing to update.");
                        }
                    } else {
                        Logger.error("Main server is not alive.");
                        stopBackupServer();
                    }
                }
            } catch (Exception e) {
                Logger.error("Error listening for heartbeat: " + e.getMessage());
            }
        }
        ServerResponse response = sendRequest(new Message(Message.Type.HEARTBEAT, "Backup"));

    }

    private void updateDatabase(String query) {
        try(Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            PreparedStatement preparedStatement = connection.prepareStatement(query);) {

            preparedStatement.setInt(1, 1);
            preparedStatement.setInt(2, 1);
            preparedStatement.setInt(3,1);
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

    private void getDatabaseFile(Object payload) {
        byte[] data = gson.fromJson(gson.toJson(payload), byte[].class);
        Logger.info("Received database file from server.");
        Logger.info("file size: " + data.length);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(databasePath + "db.db");
            fileOutputStream.write(data);
            fileOutputStream.close();
            Logger.info("Database file received successfully.");
        } catch (IOException e) {
            Logger.error("Error writing database file: " + e.getMessage());
        }
    }


    private ServerResponse receiveRequest() {
        if (!isConnected) {
            Logger.error("Not connected to server. Cannot recieve request.");
            return null;
        }

        try {
            if (in != null) {
                return gson.fromJson(in.readLine(), ServerResponse.class);
            }
        } catch (IOException e) {
            Logger.error("Error recieving request from server: " + e.getMessage());
        }
        return null;
    }

    private ServerResponse sendRequest(Message request) {
        try {
            if (socket == null || socket.isClosed()) {
                if (connectToServer()) {
                    return new ServerResponse(false, "Unable to send request.", null);
                }
            }

            String jsonMessage = gson.toJson(request);

            if (out != null) {
                out.println(jsonMessage);
                out.flush();
                Logger.info("Sent request to server: " + request.toString());
            } else {
                Logger.error("Output stream is not initialized.");
                return new ServerResponse(false, "Output stream is not initialized.", null);
            }
            return receiveRequest();

        } catch (Exception e) {
            Logger.error("Error sending request to server: " + e.getMessage());
            return new ServerResponse(false, "Error sending request to server.", null);
        }
    }

    public boolean isBackupConnect() {
        return isConnected;
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
            isConnected = false;
        } catch (IOException e) {
            Logger.error("Error closing connection: " + e.getMessage());
        }
    }

    private class HeartbeatReciever implements Runnable {

        private final String multicastAddress = "230.44.44.44";
        private final int multicastPort = 4444;
        private final int timeout = 30000;
        private int version = 0;

        @Override
        public void run() {
            try(MulticastSocket socket = new MulticastSocket(multicastPort)) {
                InetAddress group = InetAddress.getByName(multicastAddress);
                socket.joinGroup(group);
                socket.setSoTimeout(timeout);

                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    ServerResponse response = gson.fromJson(new String(packet.getData(), 0, packet.getLength()), ServerResponse.class);

                    if (response.payload()==null){
                        Logger.info("Received heartbeat from server.");
                    } else {
                        if (response.isSuccess()) {
                            Logger.info("Main server is alive.");
                            ArrayList payload = gson.fromJson(gson.toJson(response.payload()), ArrayList.class);
                            String query = gson.fromJson(gson.toJson(payload.get(0)), String.class);
                            int version = gson.fromJson(gson.toJson(payload.get(1)), Integer.class);
                            Logger.info("Query: " + query);
                            if (query == null && version == this.version+1) {
                                Logger.info("Something went wrong.");
                                System.exit(0);
                            } else if (query != null && version != this.version+1) {
                                Logger.error("Something went wrong.");
                                System.exit(0);
                            } else if (query != null && version == this.version+1) {
                                this.version = version;
                                updateDatabase(query);
                            } else {
                                Logger.info("Nothing to update.");
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
