package Client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Hashtable;

public interface ClientInterface extends Remote {

    /**
     *
     * @param userStatus è una tabella hash dove sono salvati nome utente e relativo stato (online/offline)
     * @throws RemoteException
     */
     void notifyEvent(Hashtable<String, String> userStatus) throws RemoteException;

     String getUsrName() throws RemoteException;
     String getCurrentProject() throws RemoteException;
}
