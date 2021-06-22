package ServerWorth;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.*;

/**
 * classe per la gestione del TCP
 */
public class TCPManagement {

    //porta su cui è in esecuzione il server
    private int PORT = 6000;

    private Selector selec;
    private WorthDB serverDB;

    //buffer per lo scambio di messaggi
    private ByteBuffer buff;
    private int BUFFER_DIMENSION = 1024;

    public TCPManagement(WorthDB db) throws IOException {
        this.serverDB = db;
        this.selec = Selector.open();
        this.buff = ByteBuffer.allocate(BUFFER_DIMENSION);
    }

    public void start() {
        try(
            ServerSocketChannel server_channel = ServerSocketChannel.open();
        ){
            server_channel.socket().bind(new InetSocketAddress(PORT));
            server_channel.configureBlocking(false);
            server_channel.register(selec, SelectionKey.OP_ACCEPT);
            System.out.println("Server running at port " + PORT);
            while(server_channel.isOpen()){
                selec.select();
                Set<SelectionKey> selectedKeys = selec.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    try {
                        if (key.isAcceptable()) this.manageAccept(key);
                        if (key.isReadable()) this.manageRead(key);
                        if (key.isWritable()) this.manageWrite(key);
                    }catch(IOException e){
                        String error = (new String(((ByteBuffer) key.attachment()).array()));
                        if(error.startsWith("User:")){
                            ArrayList<String> tmp = new ArrayList<>(Arrays.asList(error.split(" ")));
                            this.serverDB.logout(tmp.get(1));
                        }
                        System.out.println("Client disconnected");
                        key.channel().close();
                        key.cancel();
                    }
                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void manageAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client_channel = server.accept();
        client_channel.configureBlocking(false);
        client_channel.register(selec, SelectionKey.OP_READ);
        System.out.println("Accepted new connection with client: " + client_channel.getRemoteAddress());
    }

    private void manageRead(SelectionKey key) throws IOException {
        SocketChannel c_channel = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();
        buff.clear();
        int bytesRead = c_channel.read(buff);
        buff.flip();
        byte[] bytes = new byte[buff.limit()];
        buff.get(bytes);
        sb.append(new String(bytes));
        buff.clear();

        String msg;
        if(bytesRead < 0){
            msg = "No message sent from client\n";
            c_channel.close();
        }
        else{
            msg = sb.toString();
            ArrayList<String> msgParsing = new ArrayList<>(Arrays.asList(msg.split(" ")));
            switch(msgParsing.get(0)){
                case "LOGIN":
                    try{
                        if(this.serverDB.login(msgParsing.get(1), msgParsing.get(2))) {
                            msg = "User: " + msgParsing.get(1);
                        }

                    }
                    catch(IllegalArgumentException | RemoteException e){
                        msg = "Error in login";
                    }
                    break;
                case "LOGOUT":
                    try{
                        if(this.serverDB.logout(msgParsing.get(1))) msg = "User logged out";
                    }
                    catch(IllegalArgumentException e){
                        msg = "User not logged";
                    }

                    break;
                case "GETMYPROJECTS":
                    try{
                        String pList = this.serverDB.listProjects(msgParsing.get(1));
                        if(pList == null) msg = "Error in listProjects";
                        if(pList.equals("")) msg = "No projects has been found";
                        msg = "User: " + msgParsing.get(1) + "\n" + pList;
                    }
                    catch(NullPointerException e){
                        msg = "Error";
                    }
                    break;
                case "OPENPROJECT":
                    try{
                        if(this.serverDB.openPrj(msgParsing.get(1), msgParsing.get(2)))
                            msg = "User: " + msgParsing.get(2) + " Project opened: " + msgParsing.get(1);
                    }
                    catch(NullPointerException | IOException | IllegalArgumentException e){
                        msg = "Error in openPrj";
                    }
                    break;
                case "CREATEPROJECT":
                    try{
                        if(this.serverDB.createProject(msgParsing.get(1), msgParsing.get(2)))
                            msg = "User: " + msgParsing.get(2) + "\nProject created: " + msgParsing.get(1);
                    }
                    catch(IllegalArgumentException e){
                        msg = "Error in createProject";
                    }
                    break;
                case "INSERTMEMBER":
                    try{
                        if(this.serverDB.addMember(msgParsing.get(1), msgParsing.get(2), msgParsing.get(3)))
                            msg = "User: " + msgParsing.get(2) + " added " + msgParsing.get(3);
                    }
                    catch(IllegalArgumentException | IOException e){
                        msg = "Error in addMember";
                    }
                    break;
                case "LISTMEMBERS":
                    try{
                        String mList = this.serverDB.showMembers(msgParsing.get(1), msgParsing.get(2));
                        if(mList == null || mList.equals("")) msg = "Error in showMembers";
                        msg = "User: " + msgParsing.get(2) + "\n" + mList;
                    }
                    catch(IllegalArgumentException e){
                        msg = "Error";
                    }
                    break;
                case "LISTCARDS":
                    try{
                        String cList = this.serverDB.showCards(msgParsing.get(1), msgParsing.get(2));
                        if(cList == null) msg = "Error in showCards";
                        if(cList.equals("")) msg = "The project " + msgParsing.get(1) + " has no cards";
                        msg = "User: " + msgParsing.get(2) + "\n" + cList;
                    }
                    catch(IllegalArgumentException e){
                        msg = "Error";
                    }
                    break;
                case "CARD":
                    try{
                        String out = this.serverDB.showCard(msgParsing.get(1), msgParsing.get(2), msgParsing.get(3));
                        if(out == null || out.equals("")) msg = "Error in showCard";
                        msg = "User: " + msgParsing.get(3) + "\n" + out;
                    }
                    catch(NullPointerException | IllegalArgumentException e){
                        msg = "Error";
                    }
                    break;
                case "INSERTCARD":
                    try{
                        if(this.serverDB.addCard(msgParsing.get(1), msgParsing.get(2), msgParsing.get(3), msgParsing.get(4)))
                            msg = "User: " + msgParsing.get(4) + " created card " + msgParsing.get(2);
                    }
                    catch(IllegalArgumentException | NullPointerException | IOException e){
                        e.printStackTrace();
                        msg = "Error in addCard";
                    }
                    break;
                case "CHANGEPOS":
                    try{
                        if(this.serverDB.moveCard(msgParsing.get(1), msgParsing.get(2), msgParsing.get(3), msgParsing.get(4), msgParsing.get(5)))
                            msg = "User: " + msgParsing.get(5) + " moved card " + msgParsing.get(2) + " from " + msgParsing.get(3) + " to " + msgParsing.get(4);
                        else msg = "Couldn't complete the movement";
                    }
                    catch(IllegalArgumentException | NullPointerException | IOException e){
                        msg = "Error in moveCard";
                    }
                    break;
                case "CARDHISTORY":
                    try{
                        String out = this.serverDB.getCardHistory(msgParsing.get(1), msgParsing.get(2), msgParsing.get(3));
                        if(out == null || out.equals("")) msg = "Error in getCardHistory";
                        msg = "User: " + msgParsing.get(3) + "\n" + "Card: " + msgParsing.get(2) + "\n" + out;
                    }
                    catch(IllegalArgumentException | NullPointerException e){
                        msg = "Error";
                    }
                    break;
                case "ERASEPROJECT":
                    try{
                        if(this.serverDB.cancelProject(msgParsing.get(1), msgParsing.get(2)))
                            msg = "User: " + msgParsing.get(2) + " Project erased: " + msgParsing.get(1);
                        else msg = "Error in cancelling the project";
                    }
                    catch(IllegalArgumentException | NullPointerException | IOException e){
                        e.printStackTrace();
                        msg = "Error in cancelProject";
                    }
                    break;
                case "GETCHATIP":
                    try{
                        String out = this.serverDB.getChatIP(msgParsing.get(1), msgParsing.get(2));
                        if(out == null || out.equals("")) msg = "Error in getChatIP";
                        msg = "User: " + msgParsing.get(2) + " IP Chat: " + out;
                    }
                    catch(IllegalArgumentException | NullPointerException e){
                        msg = "Error";
                    }
                    break;
            }
        }
        c_channel.register(selec, SelectionKey.OP_WRITE, ByteBuffer.wrap(msg.getBytes(StandardCharsets.ISO_8859_1)));
    }

    private void manageWrite(SelectionKey key) throws IOException {
        SocketChannel c_channel = (SocketChannel) key.channel();
        ByteBuffer bf = (ByteBuffer) key.attachment();
        c_channel.write(bf);
        if(bf.hasRemaining()){
            System.out.println("Error occurred while writing");
            return;
        }
        c_channel.register(selec, SelectionKey.OP_READ, key.attachment());
    }
}
