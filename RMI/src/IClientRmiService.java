import model.Message;
import model.ServerResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientRmiService  extends Remote {
    void recieveEvent(String event) throws RemoteException;
    boolean connectToServer() throws RemoteException;
    ServerResponse sendRequest(Message message) throws RemoteException;
    ServerResponse receiveResponse() throws RemoteException;
}
