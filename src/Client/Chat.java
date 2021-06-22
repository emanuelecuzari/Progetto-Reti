package Client;

import ServerWorth.MSGHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * classe per definizione di una chat di progetto
 */
public class Chat {
    MulticastSocket socket;
    MSGHandler handler;
    int port;
    InetAddress addr;
    String username;

    //Costruttore
    public Chat(int port, String username, String host) throws IOException {
        this.port = port;
        this.username = username;
        this.addr = InetAddress.getByName(host);
        this.socket = new MulticastSocket(this.port);
        /* aggiunta al gruppo Multicast */
        this.socket.joinGroup(addr);
        this.handler = new MSGHandler(this.socket, this.username);
        Thread handlerT = new Thread(handler);
        handlerT.start();
    }

    /**
     *
     * metodo per l'invio di un messaggio da parte del client sulla chat
     * @param msg messaggio da inviare
     */
    public void sendMsg(String msg) throws NullPointerException{
        if(msg == null) throw new NullPointerException();
        byte[] sendBuf = (this.username + ": " + msg).getBytes();
        //datagrampacket contenete il messaggio da inviare sull'indirizzo di multicast
        DatagramPacket dpSend = new DatagramPacket(sendBuf, sendBuf.length, this.addr, this.port);
        try{
            this.socket.send(dpSend);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * metodo per la lettura dei messaggi
     */
    public void readMsg(){
        for(String str : this.handler.getMessages()){
            System.out.println(str);
        }
    }

}
