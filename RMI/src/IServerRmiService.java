import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IServerRmiService extends Remote {
    void registerClient(IClientRmiService client) throws RemoteException;
    void unregisterClient(IClientRmiService client) throws RemoteException;
    void notifyEvent(String event) throws RemoteException;

}