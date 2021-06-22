package ServerWorth;

import Client.ClientInterface;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * classe per la definizione delle operazioni lato server
 * definisce un database, caricato con le informazioni precedentemente serializzate
 */
public class WorthDB extends RemoteServer implements RMIServerInterface {

    //porta per RMI
    private static int RMI_PORT = 7000;

    //lista degli utenti registrati
    private List<Utente> usersList;

    //lista degli utentu online
    private List<String> onlineUsers;

    //lista dei progetti creati
    private List<Progetto> projectsList;

    //lista dei progetti aperti
    private List<String> openedProjects;

    //lista di clients
    private List<ClientInterface> clients;

    //manager della serializzazione/deserializzazione
    private DataSerialization manageSerialization;
    private String servIP;

    public WorthDB() throws IOException {
        //recupero l'indirizzo del server (locale)
        this.servIP = localAddress();
        System.out.println("Server IP: " + servIP);

        this.manageSerialization = new DataSerialization();
        //inizializzazione delle liste, rese synchronized per gestire la concorrenza
        //userList e projectList sono rese synchronized sulle liste che contengono i dati deserializzati
        this.usersList = Collections.synchronizedList(manageSerialization.deserializeUsers());
        this.projectsList = Collections.synchronizedList(manageSerialization.deserializeProjects());
        this.onlineUsers = Collections.synchronizedList(new LinkedList<>());
        this.openedProjects = Collections.synchronizedList(new LinkedList<>());
        this.clients = Collections.synchronizedList(new LinkedList<>());

        /* pubblicazione delle risorse RMI */
        RMIServerInterface stub = (RMIServerInterface) UnicastRemoteObject.exportObject(this, 10000);
        LocateRegistry.createRegistry(RMI_PORT);
        Registry register = LocateRegistry.getRegistry(this.servIP, RMI_PORT);
        register.rebind("WORTHServer", stub);
        System.out.println("RMI Server now ready to work");

        //avvio della connessione TCP
        TCPManagement tcpMng = new TCPManagement(this);
        tcpMng.start();
    }

    /**
     * metodo per il recupero dell'indirizzo locale del server quando comunica con google.com (sito di test)
     * questo metodo non è mio: credits to
     * https://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java/41822127#41822127
     * @return l'indirizzo locale del server
     */
    public String localAddress(){
        Socket sck = new Socket();
        try{
            sck.connect(new InetSocketAddress("google.com", 80));
        }
        catch(IOException e){
            return "localhost";
        }
        return sck.getLocalAddress().getHostAddress();
    }

    //metodo synchronized per gestire la concorrenza
    @Override
    public synchronized String registerUser(String username, String passw) throws IOException, IllegalArgumentException {
        //controllo che nessuno si registri con il nome riservato al server
        if(username == null || passw == null || username.equalsIgnoreCase("WORTH")) throw new IllegalArgumentException("Parametri non validi");
        //aggiorno la lista degli utenti
        this.usersList = manageSerialization.deserializeUsers();
        for(Utente tmp : usersList){
            if(tmp.getUsername().equals(username)) {
                return "REGISTER_FAILURE";
            }
        }
        Utente u = new Utente(username, passw);
        usersList.add(u);
        manageSerialization.serializeUsers(u);
        return "REGISTER_SUCCESS";
    }

    //metodo synchronized per gestire la concorrenza
    @Override
    public synchronized void registerForCallback(ClientInterface client) throws  RemoteException{
        if(!clients.contains(client)){
            //aggiungo il nuovo client alla lista dei clients connessi
            clients.add(client);
            System.out.println("Registration for callback succeeded");
            //avviso gli altri clients
            doCallbacks();
        }
        else{
            System.out.println("Cannot register client for callback");
        }
    }

    //metodo synchronized per gestire la concorrenza
    @Override
    public synchronized void unregisterForCallback(ClientInterface client) throws IOException {
        //il client non ha mai aperto un progetto
        if(client.getCurrentProject() == null){
            if(clients.remove(client)) System.out.println(client.getUsrName() + " unregistered");
            else System.out.println("Cancellation from callback failed");
            return;
        }
        //rimozione del client e notifico sulla chat di progetto l'avvenuta disconnessione
        if(clients.remove(client)) updateChat(client.getUsrName() + " left", client.getCurrentProject());
        else System.out.println("Cancellation from callback failed");
    }

    /**
     * metodo che aggiorna le strutture locali dei client connessi qaundo un utente si registra o effettua il login
     * metodo synchronized per gestire la concorrenza
     * @throws RemoteException quando un client si disconnette
     */
    public synchronized void doCallbacks() throws RemoteException{
        Hashtable<String, String> toNotify = new Hashtable<>();
        for(Utente u : usersList){
            //inserisco nell'hashtable tutti gli utenti della lista con il loro stato
            toNotify.put(u.getUsername(), u.getStato());
        }
        //uso un iteratore per selezionare i clients a cui inviare gli aggornamenti
        Iterator<ClientInterface> itr = clients.iterator();
        while(itr.hasNext()){
            ClientInterface client = itr.next();
            try{
                //invio gli aggiornamenti
                client.notifyEvent(toNotify);
            }
            //disconnesione
            catch(RemoteException e){
                clients.remove(client);
                return;
            }
        }
    }

    /**
     * metodo per effettuare il login
     * metodo synchronized per gestire la concorrenza
     * @param username nome dell'utente che si vuole loggare
     * @param passw password con cui l'utente si vuole loggare
     * @return true se l'operazione va a buon fine
     */
    public synchronized boolean login(String username, String passw) throws IllegalArgumentException, IOException {
        if(username == null || passw == null || username.equals("") || passw.equals("")){
            throw new IllegalArgumentException("Invalid login");
        }
        //aggiorno la lista degli uetnti
        this.usersList = manageSerialization.deserializeUsers();
        for(Utente u : usersList){
            if(username.equals(u.getUsername())){
                //utente già loggato
                if(onlineUsers.contains(u.getUsername())) throw new IllegalArgumentException("User " + username + " already logged");
                //password errata
                if(!passw.equals(u.getPassw())){
                    throw new IllegalArgumentException("Wrong password");
                }
                else{
                    u.setStato("online");
                    onlineUsers.add(u.getUsername());
                    manageSerialization.serializeUsers(u);
                    doCallbacks();
                    return true;
                }
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * metodo per effettuare il logout
     * metodo synchronized per gestire la concorrenza
     * @param username nome dell'utente che vuole eseguire il logout
     * @return true se l'operazione va a buon fine, false altrimenti
     */
    public synchronized boolean logout(String username) throws IllegalArgumentException, IOException {
        if(username == null) throw new IllegalArgumentException("Invalid parameter");
        //aggiorno la lista degli utenti
        this.usersList = manageSerialization.deserializeUsers();
        for(Utente u : usersList){
            if(username.equals(u.getUsername())){
                //utente non loggato
                if(!onlineUsers.contains(u.getUsername())) throw new IllegalArgumentException();
                u.setStato("offline");
                onlineUsers.remove(u.getUsername());
                manageSerialization.serializeUsers(u);
                doCallbacks();
                return true;
            }
        }
        return false;
    }

    /**
     * metodo per accedere a un progetto di cui un utente fa parte
     * @param prjName nome del progetto
     * @param username nome dell'utente che esgue l'operazione
     * @return true se l'operazione va a buon fine
     */
    public boolean openPrj(String prjName, String username) throws NullPointerException, IOException, IllegalArgumentException {
        if(prjName == null || username == null) throw new NullPointerException();
        //recupero il progetto
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new IllegalArgumentException();
        //check dei permessi
        if(!prjTmp.getMemberList().contains(username))
            throw new IllegalArgumentException("Permission denied");
        if(!openedProjects.contains(prjName)) {
            openedProjects.add(prjName);
        }
        updateChat("User " + username + " opened the project " + prjName, prjName);
        return true;
    }

    /**
     * metodo per visualizzare tutti i progetti di cui un utente fa parte
     * @param username nome delll'utente che esegue l'operazione
     * @return out, stringa che contiene i progetti di cui l'utente fa parte, o nessuno
     * @throws NullPointerException se username non valido
     */
    public String listProjects(String username) throws NullPointerException{
        if(username == null) throw  new NullPointerException();
        LinkedList<String> out = new LinkedList<>();
        for(Progetto p : projectsList){
            if(p.getMemberList().contains(username)){
                out.add(p.getProjectName());
            }
        }
        return out.toString();
    }

    /**
     * metodo per la creazione di un progetto
     * @param name nome del progetto da creare
     * @param username nome delll'utente che esegue l'operazione
     * @return true se l'operazione va a buon fine
     * @throws IOException se occorrono degli errori nella serializzazione
     */
    public boolean createProject(String name, String username) throws IllegalArgumentException, IOException {
        if(name == null || username == null || name.equals("")) throw new IllegalArgumentException("Invalid project name");
        //controllo che il progetto non esista già
        for(Progetto p : projectsList){
            if(name.equals(p.getProjectName())) throw new IllegalArgumentException("Name already used");
        }
        Progetto new_p = new Progetto(name, username);
        new_p.setCreator(username);
        projectsList.add(new_p);
        //creo l'indirizzo IP della chat
        new_p.setChatIP(createChatIP(name));
        manageSerialization.serializeProject(new_p);
        return true;
    }

    /**
     * metodo per aggiungere un utente a un progetto
     * @param prjName nome del progetto
     * @param username nome delll'utente che esegue l'operazione
     * @param userToAdd utente da aggiungere
     * @return true se l'operazione va a buon fine
     * @throws IOException se occorrono dei problemi con la serializzazione
     */
    public boolean addMember(String prjName, String username, String userToAdd) throws IllegalArgumentException, IOException {
        if(prjName == null || username == null || userToAdd == null)
            throw new IllegalArgumentException("Something went wrong");
        /* cehck per permesso */
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new IllegalArgumentException();
        if(!prjTmp.getMemberList().contains(username))
            throw new IllegalArgumentException("Permission denied");
        /* check su userToAdd */
        Utente toAdd = getUser(userToAdd);
        if(toAdd == null) throw new IllegalArgumentException();
        //controllo che l'utente sia registrato e non sia già membro
        if(usersList.contains(toAdd) && !prjTmp.getMemberList().contains(userToAdd)){
            prjTmp.getMemberList().add(userToAdd);
            updateChat("User " + username + " added " + userToAdd + " to the project", prjName);
            manageSerialization.serializeProject(prjTmp);
            return true;
        }
        else if(prjTmp.getMemberList().contains(userToAdd)){
            throw new IllegalArgumentException("User " + userToAdd + " already a member");
        }
        else{
            throw new IllegalArgumentException("User " + userToAdd + " not registered");
        }
    }

    /**
     * metodo per mostrare i membri di un progetto
     * @param prjName nome del progetto
     * @param username nome delll'utente che esegue l'operazione
     * @return sb, stringa che contiene i membri (ne conterrà sempre almeno uno, il creator)
     */
    public String showMembers(String prjName, String username) throws IllegalArgumentException{
        if(prjName == null || username == null) throw new IllegalArgumentException("Something went wrong");
        /* cehck per permesso */
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new IllegalArgumentException();
        if(!prjTmp.getMemberList().contains(username))
            throw new IllegalArgumentException("Permission denied");
        StringBuilder sb = new StringBuilder();
        for(String str : prjTmp.getMemberList()){
            sb.append(str).append("\n");
        }
        return sb.toString();
    }

    /**
     * metodo per mostrare tutte le cards di un progetto
     * @param prjName ome del progetto
     * @param username nome delll'utente che esegue l'operazione
     * @return sb, stringa contenete tutte le cards del progetto, o nessuna
     */
    public String showCards(String prjName, String username) throws IllegalArgumentException{
        if(prjName == null || username == null) throw new IllegalArgumentException("Something went wrong");
        /* cehck per permesso */
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new IllegalArgumentException();
        if(!prjTmp.getMemberList().contains(username))
            throw new IllegalArgumentException("Permission denied");
        StringBuilder sb = new StringBuilder();
        for(Card c : prjTmp.getCardTracking()){
            sb.append(c.getName() + "\n");
        }
        return sb.toString();
    }

    /**
     * metodo per mostrare i dettagli di una card di un progetto
     * @param prjName nome del progetto
     * @param cardName nome della card
     * @param username nome delll'utente che esegue l'operazione
     * @return sb, stringa con tutte le info relative alla card
     */
    public String showCard(String prjName, String cardName, String username) throws NullPointerException, IllegalArgumentException{
        if(prjName == null || cardName == null || username == null)
            throw new NullPointerException();
        /* cehck per permesso */
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new NullPointerException();
        if(!prjTmp.getMemberList().contains(username))
            throw new IllegalArgumentException("Permission denied");
        StringBuilder sb = new StringBuilder();
        Card cardTmp = prjTmp.getCard(cardName);
        // check sulla card
        if(cardTmp == null) throw new NullPointerException();
        sb.append("Nome: " + cardTmp.getName() + "\n").append("Descrzione: " + cardTmp.getDescrption() + "\n").append(
                "Lista attuale: " + cardTmp.getLastPosition());
        return sb.toString();
    }

    /**
     * metodo per aggiungere una nuova card a un progetto
     * @param prjName nome del progetto
     * @param cardName nome della card
     * @param descr descrizione della card
     * @param username nome delll'utente che esegue l'operazione
     * @return true se l'operazione va a buon fine
     * @throws IOException se occorrono erroi nella serializzazione
     */
    public boolean addCard(String prjName, String cardName, String descr, String username) throws NullPointerException, IllegalArgumentException, IOException {
        if(prjName == null || cardName == null || descr == null || username == null)
            throw new NullPointerException();
        /* cehck per permesso */
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new NullPointerException();
        if(!prjTmp.getMemberList().contains(username))
            throw new IllegalArgumentException("Permission denied");
        Card tmp = new Card(cardName, descr);
        //controllo che la carta non esista già
        for(Card c : prjTmp.getCardTracking()) {
            if(c.getName().equals(cardName))
                throw new IllegalArgumentException();
        }
        prjTmp.getTodo_list().add(tmp);
        prjTmp.getCardTracking().add(tmp);
        manageSerialization.serializeProject(prjTmp);
        updateChat("Card " + cardName + " added by " + username, prjName);
        return true;
    }

    /**
     * metodo per lo spostamento di una card da una lista di lavoro a un'altra
     * @param prjName nome del progetto
     * @param cardName nome della card
     * @param src lista di partenza
     * @param dest lista di arrivo
     * @param username nome delll'utente che esegue l'operazione
     * @return true se l'operazione va a buon fine, false altrimenti
     * @throws IOException se occorrono errori nella serializzazione
     */
    public boolean moveCard(String prjName, String cardName, String src, String dest, String username) throws NullPointerException, IllegalArgumentException, IOException {
        if(prjName == null || cardName == null || src == null || dest == null || username == null)
            throw new NullPointerException();
        /* cehck per permesso */
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new NullPointerException();
        if(!prjTmp.getMemberList().contains(username))
            throw new IllegalArgumentException("Permission denied");
        Card card = prjTmp.getCard(cardName);
        //check sulla card
        if(card == null) throw new NullPointerException();
        //spostamento
        if(prjTmp.getListByName(src).remove(card)){
            prjTmp.getListByName(dest).add(card);
            card.getHistory().add(dest);
            manageSerialization.serializeProject(prjTmp);
            updateChat("Card " + cardName + " moved by " + username + " into " + dest + " list", prjName);
            return true;
        }
        return false;
    }

    /**
     * metodo per recuperare tutti gli spostamenti di una card
     * @param prjName nome del progetto
     * @param cardName nome della card
     * @param username nome delll'utente che esegue l'operazione
     * @return sb, sttringa con tutti gli spostamenti
     */
    public String getCardHistory(String prjName, String cardName, String username) throws NullPointerException, IllegalArgumentException{
        if(prjName == null || cardName == null || username == null) throw new NullPointerException();
        /* cehck per permesso */
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new NullPointerException();
        if(!prjTmp.getMemberList().contains(username))
            throw new IllegalArgumentException("Permission denied");
        Card card = prjTmp.getCard(cardName);
        //check sulla card
        if(card == null) throw new NullPointerException();
        //controllo che la card sia presente nel progetto
        if(!prjTmp.getCardTracking().contains(card))
            throw new IllegalArgumentException();
        StringBuilder sb = new StringBuilder();
        for(String str : card.getHistory()){
            sb.append(str + "\n");
        }
        return sb.toString();
    }

    /**
     * metodo per eliminare un progetto
     * @param prjName nome del progetto
     * @param username nome delll'utente che esegue l'operazione
     * @return true se l'operazione va a buon fine, false altrimenti
     * @throws IOException se occorrono errori nella serializzazione
     */
    public boolean cancelProject(String prjName, String username) throws NullPointerException, IllegalArgumentException, IOException {
        if(prjName == null || username == null) throw new NullPointerException();
        /* cehck per permesso */
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new NullPointerException();
        if(!prjTmp.getMemberList().contains(username))
            throw new IllegalArgumentException("Permission denied");
        //controllo che tutte le carte si trovino nella done list
        if(prjTmp.getCardTracking().size() == prjTmp.getDone_list().size()){
            updateChat("User " + username + " deleted the project", prjName);
            projectsList.remove(prjTmp);
            manageSerialization.endProject(prjTmp);
            return true;
        }
        return false;
    }

    /**
     * metodo per recuperare un progetto sapendo il nome
     * @param prjName nome del progetto
     * @return un progetto, null se non esiste
     */
    public Progetto getPrj(String prjName) {
        for (Progetto p : projectsList) {
            if (prjName.equals(p.getProjectName()))
                return p;
        }
        return null;
    }

    /**
     * metodo per recuperare un utente sapendo il nome
     * @param username nome dell'utente
     * @return un utente, null se non esiste
     */
    public Utente getUser(String username) {
        for (Utente u : usersList) {
            if (username.equals(u.getUsername()))
                return u;
        }
        return null;
    }

    /**
     * metodo per creare l'IP di una chat di progetto
     * @param prjName nome del progetto
     * @return sb, IP della chat; null se occorrono errori
     */
    public String createChatIP(String prjName) throws NullPointerException{
        if(prjName == null) throw new NullPointerException();
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new NullPointerException();
        StringBuilder sb = new StringBuilder();
        //gli indirizzi multicast vanno da 224.0.0.0 a 239.255.255.255, quindi scelgo randomicamente un numero tra 224 e 239
        int min = 224;
        int max = 239;
        int range = max - min + 1;
        int rand = (int)(Math.random() * range) + min;
        sb.append(rand).append(".");
        int r;
        int unique;
        for(int i = 0; i < 3; i++){
            r = (int)(Math.random() * 255);
            //rendo l'indirizzo dipendente dall'indice del progetto per garantirne l'unicità
            unique = (r + projectsList.indexOf(prjTmp)) % 256;
            if(i == 2) sb.append(unique);
            else sb.append(unique).append(".");
        }
        try{
            //controllo che l'indirizzo generato sia effettivamente di multicast
            if(InetAddress.getByName(sb.toString()).isMulticastAddress()){
                return sb.toString();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * metodo per il recupero dell'IP della chat
     * @param prjName nome del progetto
     * @param username nome dell'utente che esegue l'operazione
     * @return l'IP della chat
     */
    public String getChatIP(String prjName, String username) throws  NullPointerException, IllegalArgumentException{
        if(prjName == null || username == null) throw new NullPointerException();
        //check dei permessi
        Progetto prjTmp = getPrj(prjName);
        if(prjTmp == null) throw new NullPointerException();
        //WORTH "richiede" l'indirizzo IP della chat quando deve inviare degli aggiornamenti, ma non è nella lista degli utenti
        if(prjTmp.getMemberList().contains(username) || username.equals("WORTH")){
            return prjTmp.getChatIP();
        }
        throw new IllegalArgumentException("Permission denied");
    }

    /**
     * metodo per aggiornare la chat (da parte di WORTH)
     * @param msg messaggio da inviare
     * @param prjName nome del progetto
     */
    private void updateChat(String msg, String prjName) throws IOException {
        MulticastSocket sck = new MulticastSocket();
        //prendo i byte del messaggio
        String finalMsg = "WORTH: " + msg;
        byte[] dataToChat = finalMsg.getBytes();
        //datagrampacket per l'invio del messaggio verso l'indirizzo di multicast
        DatagramPacket dp = new DatagramPacket(dataToChat, dataToChat.length, InetAddress.getByName(getChatIP(prjName, "WORTH")), 4500);
        //effettuo l'invio solo se il progetto è aperto
        if(openedProjects.contains(prjName)) sck.send(dp);
    }

}
