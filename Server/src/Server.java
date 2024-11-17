import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import utils.Logger;

public class Server {
    private static final int MAX_THREADS = 10;

    public static void main(String[] args) {
        // Check command-line arguments
        if (args.length != 2) {
            System.err.println("Usage: java Server <port> <database_path>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String dbPath = args[1];

        // Create a thread pool to handle multiple clients concurrently
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Logger.info("Server started. Listening on port " + port);

            // Initialize ServerService to handle database setup
            ServerService serverService = new ServerService(dbPath, serverSocket);
            serverService.startServer();  // This will initialize the database if it doesn’t exist

            Logger.info("Server is ready to accept client connections.");

            // Continue accepting client connections
            while (true) {
                try {
                    // Accept incoming client connection
                    Socket clientSocket = serverSocket.accept();
                    Logger.info("New client connected: " + clientSocket.getInetAddress());

                    // Use the thread pool to handle the client connection
                    threadPool.execute(new ClientHandler(clientSocket, dbPath));
                } catch (IOException e) {
                    Logger.error("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            Logger.error("Error starting server: " + e.getMessage());
            System.exit(1);
        } finally {
            // Shut down the thread pool gracefully when the server stops
            threadPool.shutdown();
        }
    }
}
