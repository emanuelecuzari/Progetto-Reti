package  Client;

import ServerWorth.RMIServerInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
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

            BufferedReader clientInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Client successfully connected!");
            System.out.println("Welcome to WORTH!");
            System.out.println("Use command 'help' to learn how to use WORTH");

            while(Thread.currentThread().isAlive()){
                String command = clientInput.readLine().trim();
                if(command.equalsIgnoreCase("help")){
                    help();
                    continue;
                }
                StringTokenizer tokenizedCommand = new StringTokenizer(command);
                ArrayList<String> parsedCommand = new ArrayList<>();
                while(tokenizedCommand.hasMoreTokens()){
                    parsedCommand.add(tokenizedCommand.nextToken());
                }
                if(this.usrName == null){
                    //utente non registrato o non loggato
                    if(parsedCommand.size() == 3){
                        //caso in cui si richiede la registrazione
                        if(parsedCommand.get(0).equalsIgnoreCase("register")){
                            regUser(parsedCommand.get(1), parsedCommand.get(2));
                        }
                        else if(parsedCommand.get(0).equalsIgnoreCase("login")){
                            loginUser(parsedCommand.get(1), parsedCommand.get(2));
                        }
                    }
                    else help();
                }
                else if(this.currentProject == null){
                    if(parsedCommand.get(0).equalsIgnoreCase("list_users")){
                        if(parsedCommand.size() == 1){
                            System.out.println(getUsers());
                        }
                        else help();
                    }
                    else if(parsedCommand.get(0).equalsIgnoreCase("list_online_users")){
                        if(parsedCommand.size() == 1){
                            System.out.println(getOnlineUsers());
                        }
                        else help();
                    }
                    else if(parsedCommand.get(0).equalsIgnoreCase("list_projects")){
                        if(parsedCommand.size() == 1) System.out.println(getMyProjects());
                        else help();
                    }
                    else if(parsedCommand.get(0).equalsIgnoreCase("create_project")){
                        if(parsedCommand.size() == 2) newProject(parsedCommand.get(1));
                        else help();
                    }
                    else if(parsedCommand.get(0).equalsIgnoreCase("open_project")){
                        if(parsedCommand.size() == 2) openProject(parsedCommand.get(1));
                        else help();
                    }
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
                else{
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
                        case "add_member":
                            if(parsedCommand.size() == 2) insertMember(parsedCommand.get(1));
                            else help();
                            break;
                        case "show_members":
                            if(parsedCommand.size() == 1) System.out.println(listMembers());
                            else help();
                            break;
                        case "show_cards":
                            if(parsedCommand.size() == 1) System.out.println(listCards());
                            else help();
                            break;
                        case "show_card":
                            if(parsedCommand.size() == 2) System.out.println(showACard(parsedCommand.get(1)));
                            else help();
                            break;
                        case "add_card":
                            if(parsedCommand.size() == 3) insertCard(parsedCommand.get(1), parsedCommand.get(2));
                            else help();
                            break;
                        case "move_card":
                            if(parsedCommand.size() == 4) changeCardPos(parsedCommand.get(1), parsedCommand.get(2), parsedCommand.get(3));
                            else help();
                            break;
                        case "get_card_history":
                            if(parsedCommand.size() == 2) System.out.println(getMovements(parsedCommand.get(1)));
                            else help();
                            break;
                        case "cancel_project":
                            if(parsedCommand.size() == 1) eraseProject();
                            else help();
                            break;
                        case "read_chat":
                            if(parsedCommand.size() == 1) readChat();
                            else help();
                            break;
                        case "send_message":
                            if(parsedCommand.size() == 2) sendMsgOnChat(parsedCommand.get(1));
                            else help();
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

    public void regUser(String username, String passw) throws IOException {
        System.out.println("Please wait for the server to complete the registration...");
        try{
            System.out.println(remoteRMI.registerUser(username, passw));
        }
        catch(RemoteException | IllegalArgumentException e){
            e.printStackTrace();
        }
    }

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
                while(c_channel.read(buff) == 0){
                    continue;
                }
                buff.flip();
                byte[] bytes = new byte[buff.limit()];
                buff.get(bytes);
                sb.append(new String(bytes));
                String msg = sb.toString();

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

    public void logoutUser(){
        String label = "LOGOUT " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                remoteRMI.unregisterForCallback(callbackStub);
                String answer = askToServer(label);

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

    public String getUsers(){
        StringBuilder sb = new StringBuilder();
        for(String name : localUsersList.keySet()){
            sb.append("User: " + name).append(" ").append("Status: " + localUsersList.get(name)).append("\n");
        }
        return sb.toString();
    }

    public String getOnlineUsers(){
        StringBuilder sb = new StringBuilder();
        for(String name : localUsersList.keySet()){
            if(localUsersList.get(name).equals("online")){
                sb.append("User: " + name).append("\n");
            }
        }
        return sb.toString();
    }

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

    public void openProject(String prjName){
        String label = "OPENPROJECT " + prjName + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                String answer = askToServer(label);

                if(answer.startsWith("User: " + this.getUsrName())){
                    this.currentProject = prjName;
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

    public void newProject(String prjName){
        String label = "CREATEPROJECT " + prjName + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                String answer = askToServer(label);

                if(answer.startsWith("User: " + this.getUsrName())) {
                    System.out.println(answer);
                    this.openProject(prjName);
                }
                //in questo caso contine un messaggio di errore
                else System.out.println(answer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

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

    public void eraseProject(){
        String label = "ERASEPROJECT " + this.getCurrentProject() + " " + this.getUsrName();
        if(c_channel.isConnected()){
            try{
                String answer = askToServer(label);

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

    public void readChat(){
        this.chat.readMsg();
    }

    public void sendMsgOnChat(String msg){
        this.chat.sendMsg(msg);
    }

    public void getIpForChat(){
        String label = "GETCHATIP " + this.getCurrentProject() + " " + this.getUsrName();
        ArrayList<String> tmp;
        if(c_channel.isConnected()){
            try{
                String answer = askToServer(label);

                //formato risposta: User: username IPChat: indirizzo
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

    private String askToServer(String req) throws IOException {
        //invio richiesta al server
        ByteBuffer buff = ByteBuffer.wrap(req.getBytes());
        c_channel.write(buff);
        buff.clear();

        //ricezione risposta
        ByteBuffer bb = ByteBuffer.allocate(1024);
        StringBuilder sb = new StringBuilder();
        while(c_channel.read(bb) == 0) continue;
        bb.flip();
        byte[] bytes = new byte[bb.limit()];
        bb.get(bytes);
        sb.append(new String(bytes));
        return sb.toString();
    }

    public void setUsrName(String name){ this.usrName = name; }

    /* metodi ereditati dall'interfaccia */
    public String getUsrName(){ return this.usrName; }
    public String getCurrentProject(){ return this.currentProject; }
    public void notifyEvent(Hashtable<String, String> userStatus) throws RemoteException {
        this.localUsersList = userStatus;
    }

}
