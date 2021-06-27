package  Client;

import ServerWorth.RMIServerInterface;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringTokenizer;
import static java.lang.System.exit;

public class ClientTasks extends RemoteServer implements Runnable, ClientInterface{

    //porte per le connessioni
    public static int TCP_CONNECTION_PORT = 6000;
    public static int RMI_CONNECTION_PORT = 7000;
    public static int CHAT_PORT = 4500;

    private String usrName;
    /* ultimo progetto a cui l'utente ha lavorato */
    private String currentProject;
    private Hashtable<String, String> localUsersList;
    private Chat chat;
    private String serverIP;
    private String chatIP;

    SocketChannel c_channel;
    ClientInterface callbackStub;

    //variabili per RMI
    Registry registry;
    RMIServerInterface remoteRMI;

    public ClientTasks(String srvIP){
        usrName = null;
        currentProject = null;
        chatIP = null;
        chat = null;
        localUsersList = new Hashtable<>();
        this.serverIP = srvIP;
    }

    @Override
    public void run() {
        try{

            //setup connessione TCP
           this.c_channel = SocketChannel.open(new InetSocketAddress(this.serverIP, TCP_CONNECTION_PORT));
           this.c_channel.configureBlocking(false);

            //setup server RMI
            this.registry = LocateRegistry.getRegistry(this.serverIP, RMI_CONNECTION_PORT);
            this.remoteRMI = (RMIServerInterface) registry.lookup("WORTHServer");

            //lettore per l'input del client
            BufferedReader clientInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Client successfully connected!");
            System.out.println("Welcome to WORTH!");
            System.out.println("Use command 'help' to learn how to use WORTH");

            while(Thread.currentThread().isAlive()){
                //leggo l'input
                String command = clientInput.readLine().trim();
                if(command.equalsIgnoreCase("help")){
                    help();
                    continue;
                }
                //divido la stringa in token e li aggiungo all'array dei comandi
                StringTokenizer tokenizedCommand = new StringTokenizer(command);
                ArrayList<String> parsedCommand = new ArrayList<>();
                while(tokenizedCommand.hasMoreTokens()){
                    parsedCommand.add(tokenizedCommand.nextToken());
                }
                //utente non registrato/loggato
                if(this.usrName == null){
                    if(parsedCommand.size() == 3){
                        //caso in cui si richiede la registrazione
                        if(parsedCommand.get(0).equalsIgnoreCase("register")){
                            regUser(parsedCommand.get(1), parsedCommand.get(2));
                        }
                        //richiesta di login
                        else if(parsedCommand.get(0).equalsIgnoreCase("login")){
                            loginUser(parsedCommand.get(1), parsedCommand.get(2));
                        }
                    }
                    //qualcosa è andato storto
                    else help();
                }
                //utente loggato
                //NOTA: in caso di errore nello scrivere il comando viene sempre invocato 'help'
                else if(this.currentProject == null){
                    //richiesta di vedere gli utenti registrati a WORTH
                    if(parsedCommand.get(0).equalsIgnoreCase("list_users")){
                        if(parsedCommand.size() == 1){
                            System.out.println(getUsers());
                        }
                        else help();
                    }
                    //richiesta di vedere gli utenti online
                    else if(parsedCommand.get(0).equalsIgnoreCase("list_online_users")){
                        if(parsedCommand.size() == 1){
                            System.out.println(getOnlineUsers());
                        }
                        else help();
                    }
                    //richiesta di vedeere tutti i progetti di cui l'utente è membro
                    else if(parsedCommand.get(0).equalsIgnoreCase("list_projects")){
                        if(parsedCommand.size() == 1) System.out.println(getMyProjects());
                        else help();
                    }
                    //richiesta di creare un progetto
                    else if(parsedCommand.get(0).equalsIgnoreCase("create_project")){
                        if(parsedCommand.size() == 2) newProject(parsedCommand.get(1));
                        else help();
                    }
                    //richiesto di accesso a un progetto
                    else if(parsedCommand.get(0).equalsIgnoreCase("open_project")){
                        if(parsedCommand.size() == 2) openProject(parsedCommand.get(1));
                        else help();
                    }
                    //richiesta di logout
                    else if(parsedCommand.get(0).equalsIgnoreCase("logout")){
                        if(parsedCommand.size() == 1) logoutUser();
                        else help();
                    }
                    else if(parsedCommand.get(0).equalsIgnoreCase("help")){
                        if(parsedCommand.size() == 1) help();
                        else System.out.println("Error in using help command: no parameters needed");
                    }
                    else{
                        System.out.println("Error in writing command: check the 'help' section");
                        help();
                    }
                }
                //utente loggato e con un progetto aperto
                else{
                    //ho preferito implementare uno switch per l'alto numero di casi possibili
                    //questo perché i comandi precedenti sono possibili anche in questo caso
                    switch(parsedCommand.get(0).toLowerCase()){
                        case "list_users":
                            if(parsedCommand.size() == 1) System.out.println(getUsers());
                            else help();
                            break;
                        case "list_online_users":
                            if(parsedCommand.size() == 1) System.out.println(getOnlineUsers());
                            else help();
                            break;
                        case "list_projects":
                            if(parsedCommand.size() == 1) System.out.println(getMyProjects());
                            else help();
                            break;
                        case "create_project":
                            if(parsedCommand.size() == 2) newProject(parsedCommand.get(1));
                            else help();
                            break;
                        case "open_project":
                            if(parsedCommand.size() == 2) openProject(parsedCommand.get(1));
                            else help();
                            break;
                        //aggiunta di un membro a un porgetto
                        case "add_member":
                            if(parsedCommand.size() == 2) insertMember(parsedCommand.get(1));
                            else help();
                            break;
                        //richiesta di visione dei membri
                        case "show_members":
                            if(parsedCommand.size() == 1) System.out.println(listMembers());
                            else help();
                            break;
                        //richiesta di visione delle cards
                        case "show_cards":
                            if(parsedCommand.size() == 1) System.out.println(listCards());
                            else help();
                            break;
                        //richiesta di visione dei dettagli di una card
                        case "show_card":
                            if(parsedCommand.size() == 2) System.out.println(showACard(parsedCommand.get(1)));
                            else help();
                            break;
                        //aggiunta di una card
                        case "add_card":
                            if(parsedCommand.size() == 3) insertCard(parsedCommand.get(1), parsedCommand.get(2));
                            else help();
                            break;
                        //spostamento di una card
                        case "move_card":
                            if(parsedCommand.size() == 4) changeCardPos(parsedCommand.get(1), parsedCommand.get(2), parsedCommand.get(3));
                            else help();
                            break;
                        //richiesta di tracciamento di una card
                        case "get_card_history":
                            if(parsedCommand.size() == 2) System.out.println(getMovements(parsedCommand.get(1)));
                            else help();
                            break;
                        //eliminazione di un progetto
                        case "cancel_project":
                            if(parsedCommand.size() == 1) eraseProject();
                            else help();
                            break;
                        //lettura della chat
                        case "read_chat":
                            if(parsedCommand.size() == 1) readChat();
                            else help();
                            break;
                        //invio di un messaggio
                        case "send_message":
                            if(parsedCommand.size() == 1) help();
                            else {
                                StringBuilder msg = new StringBuilder();
                                for(String s : parsedCommand.subList(1, parsedCommand.size())){
                                    msg.append(s).append(" ");
                                }
                                sendMsgOnChat(msg.toString());
                            }
                            break;
                        case "logout":
                            if(parsedCommand.size() == 1) logoutUser();
                            else help();
                            break;
                        case "help":
                            if(parsedCommand.size() == 1) help();
                            else System.out.println("Error in using help command: no parameters needed");
                            break;
                    }
                }
            }
        }
        catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * comando help, per imparare il funzionamento di WORTH
     */
    public void help(){
        System.out.println("########################");
        System.out.println("Welcome to WORTH commands guide!");
        if(usrName == null){
            System.out.println("If you're new to our service, you must register first!\n" +
                    "Otherwise log into your account with your credentials and get started!");
            System.out.println("register username password");
            System.out.println("login username password");
        }
        else if(currentProject == null){
            System.out.println("Do you need any help " + this.getUsrName() + "?");
            System.out.println("Here is what you can do:");
            System.out.println("list_users -> to show all the users registered to our service");
            System.out.println("list_online_users -> to show all the online users");
            System.out.println("list_projects -> to show all the projects you are part of");
            System.out.println("create_project projectName -> to create a new project");
            System.out.println("open_project projectName -> to open a project");
            System.out.println("logout -> to close your session");
        }
        else{
            System.out.println("Do you need any help " + this.getUsrName() + "?");
            System.out.println("Here is what you can do:");
            System.out.println("(Cards' lists names: TODO | IN_PROGRESS | TO_BE_REVISED | DONE");
            System.out.println("It's not important to write the names in all capital letters, " +
                    "but you must use the '_' where needed)");
            System.out.println("list_users -> to show all the users registered to our service");
            System.out.println("list_online_users -> to show all the online users");
            System.out.println("list_projects -> to show all the projects you are part of");
            System.out.println("create_project projectName -> to create a new project");
            System.out.println("open_project projectName -> to open a project");
            System.out.println("add_member usernameToAdd -> to add a new member to the project");
            System.out.println("show_members -> to show all the members joining the project");
            System.out.println("show_cards -> to show all project cards");
            System.out.println("show_card cardName -> to show one project card");
            System.out.println("add_card cardName description -> to add a new card to the project");
            System.out.println("move_card cardName source dest -> to move a project card " +
                    "from the source list to the dest list");
            System.out.println("get_card_history cardName -> to get all card's movements");
            System.out.println("cancel_project -> to cancel a project");
            System.out.println("read_chat -> to read the chat");
            System.out.println("send_message message -> to send a message on the chat");
            System.out.println("logout -> to close your session");
        }
        System.out.println("########################");
    }

    //NOTA: i metodi di seguito (ad eccezione di regUser) utilizzano dei label per indicare al server
    // il tipo di richiesta da svolgere

    //NOTA: le risposte del server hanno un formato fisso (vedi i vari metodi)

    //NOTA: le risposte del server vengono sempre stampate; se è avvenuto un errore il messaggio di risposta è
    //un messaggio di errore

    /**
     * metodo per la richiesta di registrazione
     * @param username nome utente
     * @param passw password utente
     */
    public void regUser(String username, String passw) throws IOException {
        System.out.println("Please wait for the server to complete the registration...");
        try{
            System.out.println(remoteRMI.registerUser(username, passw));
        }
        catch(RemoteException | IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    /**
     * metodo per la richiesta di login
     * @param username nome utente
     * @param passw password utente
     */
    public void loginUser(String username, String passw){
        try{
            if(c_channel.isConnected()){
                //inivio dati di login al server
                String label = "LOGIN " + username + " " + passw;
                ByteBuffer buff = ByteBuffer.wrap(label.getBytes());
                c_channel.write(buff);
                buff.clear();

                //ricezione risposta server
                StringBuilder sb = new StringBuilder();
                //controllo che il server abbia effettivamente risposto
                while(c_channel.read(buff) == 0){
                    continue;
                }
                buff.flip();
                byte[] bytes = new byte[buff.limit()];
                buff.get(bytes);
                sb.append(new String(bytes));
                String msg = sb.toString();

                //formato: [User: nome]
                if(msg.equals("User: " + username)){
                    //registrazione a callback
                    this.setUsrName(username);
                    callbackStub = (ClientInterface) UnicastRemoteObject.exportObject(this, 0);
                    remoteRMI.registerForCallback(callbackStub);
                    System.out.println("User " + username + " logged in");
                }
                else{
                    System.out.println("Error while trying to log into " + username + " account");
                }
            }
            else c_channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * metodo per la richiesta di logout
     */
    public void logoutUser(){
        String label = "LOGOUT " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                remoteRMI.unregisterForCallback(callbackStub);
                String answer = askToServer(label);

                //formato: [User logged out]
                if(answer.equals("User logged out")){
                    System.out.println(answer);
                    exit(0);
                }
                else System.out.println("Error: " + answer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * metodo per ottenere gli utenti registrati
     * metodo locale in quanto ho una struttura dati del client che tiene traccia degli utenti
     * @return sb, stringa con gli utenti registrati e il loro status
     */
    public String getUsers(){
        StringBuilder sb = new StringBuilder();
        for(String name : localUsersList.keySet()){
            sb.append("User: " + name).append(" ").append("Status: " + localUsersList.get(name)).append("\n");
        }
        return sb.toString();
    }

    /**
     * metodo per ottenere gli utenti online
     * metodo locale in quanto ho una struttura dati del client che tiene traccia degli utenti
     * @return sb, stringa con gli utenti online
     */
    public String getOnlineUsers(){
        StringBuilder sb = new StringBuilder();
        for(String name : localUsersList.keySet()){
            if(localUsersList.get(name).equals("online")){
                sb.append("User: " + name).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * meotodo per la richiesta dei progetti di cui un utente fa parte
     * @return la risposta del server (stringa con specificati i progetti o vuota) o null in caso di errore
     */
    public String getMyProjects(){
        String label = "GETMYPROJECTS " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                return askToServer(label);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * metodo per la richiesta di accesso a un progetto
     * @param prjName nome del progettp
     */
    public void openProject(String prjName){
        String label = "OPENPROJECT " + prjName + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                String answer = askToServer(label);

                //formato: [User: nome Project opened: nome_prj]
                if(answer.startsWith("User: " + this.getUsrName())){
                    this.currentProject = prjName;
                    //recupero l'IP della chat per la sua inizializzazione
                    getIpForChat();
                    this.chat = new Chat(CHAT_PORT, this.getUsrName(), this.chatIP);
                }
                //in questo caso contiene un messaggio di errore
                System.out.println(answer);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * metodo per la richiesta di creazione di un progetto
     * @param prjName nome del progetto
     */
    public void newProject(String prjName){
        String label = "CREATEPROJECT " + prjName + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                String answer = askToServer(label);

                //formato: [User: nome Project created: nome_prj]
                if(answer.startsWith("User: " + this.getUsrName())) {
                    System.out.println(answer);
                    //eseguo l'accesso immediato
                    this.openProject(prjName);
                }
                //in questo caso contine un messaggio di errore
                else System.out.println(answer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * metodo per la richiesta di aggiunta di un utente a un progetto
     * @param userToAdd nome utente da aggiungere
     */
    public void insertMember(String userToAdd){
        String label = "INSERTMEMBER " + this.getCurrentProject() + " " + this.getUsrName() + " " + userToAdd;
        if(c_channel.isConnected()){
            try{
                System.out.println(askToServer(label));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * metodo per la richiesta di visione dei membri
     * @return la risposta del server (stringa con i nomi dei membri o vuota) o null in caso di errore
     */
    public String listMembers(){
        String label = "LISTMEMBERS " + this.getCurrentProject() + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                return askToServer(label);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * metodo per la richiesta di visione delle card di un progetto
     * @return la risposta del server (stringa con i nomi delle cards o vuota) o null in caso di errore
     */
    public String listCards(){
        String label = "LISTCARDS " + this.getCurrentProject() + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                return askToServer(label);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * metodo epr la richiesta di visone delle ifno di una card
     * @param cardName nome della card
     * @return la risposta del server (stringa con i dettagli della card) o null in caso di errore
     */
    public String showACard(String cardName){
        String label = "CARD " + this.getCurrentProject() + " " + cardName + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                return askToServer(label);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * metodo per l'aggiunta di una nuova card
     * @param cardName nome della card
     * @param descr descrizone
     */
    public void insertCard(String cardName, String descr){
        String label = "INSERTCARD " + this.getCurrentProject() + " " + cardName + " " + descr + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                System.out.println(askToServer(label));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * metodo per lo spostamento di una card
     * @param cardName nome della card
     * @param src lista di partenza
     * @param dst lista di arrivo
     */
    public void changeCardPos(String cardName, String src, String dst){
        String label = "CHANGEPOS " + this.getCurrentProject() + " " + cardName + " " + src + " " + dst + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                System.out.println(askToServer(label));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * metodo per la richiesta di visione del tracking di una card
     * @param cardName nome della card
     * @return la risposta del server (stringa con gli spostamenti della card) o null in caso di errore
     */
    public String getMovements(String cardName){
        String label = "CARDHISTORY " + this.getCurrentProject() + " " + cardName + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                return askToServer(label);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * metodo per la richiesta di eliminazione di un progetto
     */
    public void eraseProject(){
        String label = "ERASEPROJECT " + this.getCurrentProject() + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                String answer = askToServer(label);

                //formato: [User: nome Project erased: nome_prj]
                if(answer.startsWith("User: " + this.getUsrName())){
                    System.out.println(answer);
                    this.currentProject = null;
                }
                System.out.println(answer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * metodo per la lettura della chat
     */
    public void readChat(){
        this.chat.readMsg();
    }

    /**
     * metodo per l'invio di un messaggio sulla chat
     * @param msg messaggio
     */
    public void sendMsgOnChat(String msg){
        this.chat.sendMsg(msg);
    }

    /**
     * metodo per la richiesta di recupero dell'IP della chat
     */
    public void getIpForChat(){
        String label = "GETCHATIP " + this.getCurrentProject() + " " + this.getUsrName();
        ArrayList<String> tmp;
        if(c_channel.isConnected()){
            try{
                String answer = askToServer(label);

                //formato risposta: User: username Chat IP: indirizzo
                if(answer.startsWith("User: " + this.getUsrName())){
                    tmp = new ArrayList<>(Arrays.asList(answer.split(" ")));
                    this.chatIP = tmp.get(4);
                }
                System.out.println(answer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * metodo per la comunicazione con il server
     * @param req richiesta del client
     * @return stringa contente la risposta (affermativa o errore)
     */
    private String askToServer(String req) throws IOException {
        //invio richiesta al server
        ByteBuffer buff = ByteBuffer.wrap(req.getBytes());
        c_channel.write(buff);
        buff.clear();

        //ricezione risposta
        ByteBuffer bb = ByteBuffer.allocate(1024);
        StringBuilder sb = new StringBuilder();
        //attendo di aver effettivamente letto qualcosa
        while(c_channel.read(bb) == 0) continue;
        bb.flip();
        byte[] bytes = new byte[bb.limit()];
        bb.get(bytes);
        sb.append(new String(bytes));
        return sb.toString();
    }

    //metodo set
    public void setUsrName(String name){ this.usrName = name; }

    /* metodi ereditati dall'interfaccia */
    public String getUsrName(){ return this.usrName; }
    public String getCurrentProject(){ return this.currentProject; }
    public void notifyEvent(Hashtable<String, String> userStatus) throws RemoteException {
        this.localUsersList = userStatus;
    }

}
