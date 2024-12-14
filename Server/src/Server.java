import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import utils.Logger;

public class Server {
    private static final int MAX_THREADS = 10;
    private static final CopyOnWriteArrayList<PrintWriter> clientWriters = new CopyOnWriteArrayList<>();

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
            Logger.info("Database path: " + dbPath);
            Logger.info("Server running on IP: " + serverSocket.getInetAddress().getHostAddress());

            // Initialize RMI service
            ServerRmiService serverRmiService = new ServerRmiService();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("Server", serverRmiService);
            Logger.info("RMI service started.");

            // Initialize ServerService to handle database setup
            ServerService serverService = new ServerService(dbPath, serverSocket);
            serverRmiService.setServerService(serverService);
            serverService.setServerRmiService(serverRmiService);
            serverService.startServer();  // This will initialize the database if it doesn’t exist



            Logger.info("Server is ready to accept client connections.");

            // Continue accepting client connections
            while (true) {
                try {
                    // Accept incoming client connection
                    Socket clientSocket = serverSocket.accept();
                    Logger.info("New client connected: " + clientSocket.getInetAddress());

                    // Add the client writer to the list
                    PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                    clientWriters.add(clientWriter);

                    // Use the thread pool to handle the client connection
                    threadPool.execute(new ClientHandler(clientSocket, dbPath, serverRmiService));
                } catch (IOException e) {
                    Logger.error("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException | AlreadyBoundException e) {
            Logger.error("Error starting server: " + e.getMessage());
            System.exit(1);
        } finally {
            // Shut down the thread pool gracefully when the server stops
            threadPool.shutdown();

            // Close all active client connections
            for (PrintWriter writer : clientWriters) {
                writer.close();
            }
            Logger.info("All client connections closed. Server shutting down.");
        }
    }

    /**
     * Broadcasts an update to all connected clients.
     *
     * @param updateMessage The message to broadcast.
     */
    public static void broadcastUpdate(String updateMessage) {
        Logger.info("Broadcasting message to all clients: " + updateMessage);
        clientWriters.removeIf(writer -> {
            try {
                writer.println(updateMessage);
                writer.flush();
                return false; // Keep writer in the list
            } catch (Exception e) {
                Logger.error("Removing disconnected client writer: " + e.getMessage());
                return true; // Remove writer from the list
            }
        });
    }

    /**
     * Removes a client writer from the list.
     *
     * @param clientWriter The writer to remove.
     */
    public static void removeClient(PrintWriter clientWriter) {
        if (clientWriters.remove(clientWriter)) {
            Logger.info("Client writer removed from the server.");
        }
    }
}
