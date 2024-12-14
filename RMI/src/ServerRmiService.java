import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class ServerRmiService extends UnicastRemoteObject implements IServerRmiService {

    private List<IClientRmiService> clients;
    ServerService serverService;

    public ServerRmiService() throws RemoteException {
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

    @Override
    public List<String> listUsers() throws RemoteException {
        return serverService.getUsers();
    }

    @Override
    public List<String> listGroups() throws RemoteException {
        return serverService.getGroups();
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

    public void setServerService(ServerService serverService) {
        this.serverService = serverService;
    }
}
