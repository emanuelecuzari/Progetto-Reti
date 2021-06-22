package ServerWorth;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.LinkedList;

/**
 * classe per definire il ricevitore di un messaggio della chat
 */
public class MSGHandler implements  Runnable{

    /* socket per la ricezione di messaggi */
    MulticastSocket socket;

    /* lista dei messaggi ricevuti */
    LinkedList<String> messages;

    String name;
    byte[] buffer;

    //costruttore
    public MSGHandler(MulticastSocket socket, String name){
        this.socket = socket;
        this.name = name;
        this.messages = new LinkedList<>();
        this.buffer = new byte[1024];
    }

    @Override
    public void run() {
        /* creazione del pacchetto trmamite cui inviare il messaggio */
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        /* ricevo fino a che il ricevitore è attivo */
        while(Thread.currentThread().isAlive()){
            try{
                socket.receive(dp);
                String msgReceived = new String(dp.getData(), 0, dp.getLength());
                messages.add(msgReceived);
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public LinkedList<String> getMessages(){ return this.messages; }
}
