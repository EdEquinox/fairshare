import model.Message;
import model.ServerResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientRmiService  extends Remote {
    void recieveEvent(String event) throws RemoteException;
}
