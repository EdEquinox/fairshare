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


    protected ClientRmiService() throws Exception {
        super();
    }

    @Override
    public void recieveEvent(String event) {
        System.out.println("Event: " + event);
    }
}
