package ServerWorth;

import Client.ClientInterface;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * RMIServerInterface è la classe che rappresenta l'interfaccia per la registrazione a WORTH
 *
 */
public interface RMIServerInterface extends Remote {

    /**
     *
     * @param username è il nome utente che s'intende registrare
     * @param passw è la password legata all'utente
     * @throws RemoteException se avvengono errori durante l'esecuzione della chiamata remota
     * @throws IllegalArgumentException se lo username o la password non sono validi
     * se il nome utente è già registrato stampa un messaggio di errore, altrimenti un codice di successo
     */
     String registerUser(String username, String passw) throws IOException, IllegalArgumentException;

    /**
     *
     * metodo per la registrazione al servizio di callback
     * @param client
     * @throws RemoteException
     */
     void registerForCallback(ClientInterface client) throws RemoteException;

    /**
     *
     * metodo per la cancellazione dal servizio di callback
     * @param client
     * @throws IOException
     */
     void unregisterForCallback(ClientInterface client) throws IOException;

}
