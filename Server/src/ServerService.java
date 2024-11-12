import utils.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ServerService {
    private final int serverPort;
    private final String databasePath;
    private ServerSocket serverSocket;

    public ServerService(int serverPort, String databasePath) {
        this.serverPort = serverPort;
        this.databasePath = databasePath;
    }

    /**
     * Starts the server and listens for incoming client connections.
     */
    public void startServer() throws IOException {
        // Verifica se o arquivo da base de dados já existe
        if (!doesDatabaseExist()) {
            createNewDatabase();
        }

        serverSocket = new ServerSocket(serverPort);
        Logger.info("Server started and waiting for clients on port " + serverPort);

        // Usando o caminho da base de dados fornecido
        Logger.info("Using database located at: " + databasePath);

        while (true) {
            // Aguardar por uma conexão de cliente
            Socket clientSocket = serverSocket.accept();
            Logger.info("Client connected: " + clientSocket.getInetAddress().getHostAddress());

            // Lidar com o cliente em uma nova thread
            ClientHandler clientHandler = new ClientHandler(clientSocket, databasePath);
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
     * Creates a new SQLite database with an initial structure and version 0 if it does not exist.
     */
    private void createNewDatabase() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            Statement statement = connection.createStatement();

            // Criação da tabela principal para armazenar dados
            String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "phone_number TEXT NOT NULL, " +
                    "email TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL" +
                    ");";
            statement.execute(createTableSQL);

            // Criação da tabela de versão para armazenar o número da versão
            String createVersionTableSQL = "CREATE TABLE IF NOT EXISTS version (" +
                    "version INTEGER NOT NULL" +
                    ");";
            statement.execute(createVersionTableSQL);

            // Definir o número da versão como 0
            String setVersionSQL = "INSERT INTO version (version) VALUES (0);";
            statement.executeUpdate(setVersionSQL);

            Logger.info("New database created with version 0 at " + databasePath);
        } catch (Exception e) {
            Logger.error("Error creating new database: " + e.getMessage());
        }
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
}