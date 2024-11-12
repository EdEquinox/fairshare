import utils.Logger;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private PrintWriter out;
    private String databasePath;

    public ClientHandler(Socket clientSocket, String databasePath) {
        this.clientSocket = clientSocket;
        this.databasePath = databasePath;
    }

    @Override
    public void run() {
        try {
            // Inicializa os streams de entrada e saída para comunicação com o cliente
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Processa a comunicação com o cliente
            String message;
            while ((message = in.readLine()) != null) {
                Logger.info("Received from client: " + message);

                // Aqui você pode processar a mensagem, como armazenar dados no banco de dados
                processMessage(message);
            }
        } catch (IOException e) {
            Logger.error("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    Logger.info("Client connection closed.");
                }
            } catch (IOException e) {
                Logger.error("Error closing client connection: " + e.getMessage());
            }
        }
    }

    /**
     * Processes the received message and sends a response back to the client.
     * @param message The message from the client.
     */
    private void processMessage(String message) {
        // Processa a mensagem recebida, por exemplo, salva no banco de dados
        String response = "Message received: " + message;
        out.println(response);
        Logger.info("Sent response to client: " + response);
    }
}