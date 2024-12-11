import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class ServerRmiService extends UnicastRemoteObject implements IServerRmiService {

    private List<IClientRmiService> clients;

    public ServerRmiService(String serverIp, int serverPort) throws RemoteException {
        clients = new ArrayList<>();

    }

    @Override
    public synchronized void registerClient(IClientRmiService client) throws RemoteException {
        clients.add(client);
    }

    @Override
    public synchronized void unregisterClient(IClientRmiService client) throws RemoteException {
        clients.remove(client);
    }

    @Override
    public synchronized void notifyEvent(String event) throws RemoteException {
        for (IClientRmiService client : clients) {
            client.recieveEvent(event);
        }
    }

    public void registerUser(String user) throws RemoteException {
        notifyEvent("New register user: " + user);
    }

    public void loggedInUser(String user) throws RemoteException {
        notifyEvent("Logged in user: " + user);
    }

    public void addExpense(String expense, Double value) throws RemoteException {
        notifyEvent("New expense from user: " + expense + " with value: " + value);
    }

    public void removeExpense(int expense) throws RemoteException {
        notifyEvent("Removed expense: " + expense);
    }
}
