import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import model.Message;
import model.ServerResponse;
import utils.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.server.UnicastRemoteObject;

public class ClientRmiService extends UnicastRemoteObject implements IClientRmiService {

    private static final Gson gson = new Gson();
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MILLIS = 5000;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    boolean isConnected;
    private final String serverIp;
    private final int serverPort;


    protected ClientRmiService(String serverIp, int serverPort) throws Exception {
        super();
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    @Override
    public void recieveEvent(String event) {
        System.out.println("Event: " + event);
    }

    @Override
    public boolean connectToServer() {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                socket = new Socket(serverIp, serverPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Logger.info("Connected to server at " + serverIp + ":" + serverPort);
                isConnected = true;
                return true;
            } catch (IOException e) {
                attempts++;
                Logger.error("Error connecting to server (Attempt " + attempts + "): " + e.getMessage());
                if (attempts < MAX_RETRIES) {
                    Logger.info("Retrying in " + RETRY_DELAY_MILLIS / 1000 + " seconds...");
                    try {
                        Thread.sleep(RETRY_DELAY_MILLIS);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    Logger.info("Max retries reached. Could not connect to the server.");
                    break;
                }
            }
        }
        isConnected = false;
        return false;
    }

    @Override
    public ServerResponse sendRequest(Message message) {
        try {
            if (socket == null || socket.isClosed()) {
                if (!connectToServer()) {
                    return new ServerResponse(false, "Unable to connect to server", null);
                }
            }

            String jsonMessage = gson.toJson(message);

            if (out != null) {
                out.println(jsonMessage);
                out.flush();
                Logger.info("Sent message to server: " + jsonMessage);
            } else {
                Logger.error("Output stream is not initialized.");
                return new ServerResponse(false, "Output stream is not initialized", null);
            }

            return receiveResponse();
        } catch (Exception e) {
            Logger.error("Error during command execution: " + e.getMessage());
            return new ServerResponse(false, "Error during command execution: " + e.getMessage(), null);
        }
    }

    @Override
    public ServerResponse receiveResponse() {
        try {
            if (in != null) {
                String line = in.readLine();
                if (line == null || line.isBlank()) {
                    Logger.error("Received empty or null response from server.");
                    return null; // Pode ser personalizado para lançar exceção ou retornar outro valor.
                }

                try {
                    return gson.fromJson(line, ServerResponse.class);
                } catch (JsonSyntaxException e) {
                    Logger.error("Error parsing JSON response: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            Logger.error("Error receiving message: " + e.getMessage());
        }
        return null; // Retornar null se ocorrer erro ou resposta inválida
    }
}
