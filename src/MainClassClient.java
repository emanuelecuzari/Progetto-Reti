import Client.ClientTasks;

/**
 * classe per far partire l'esecuzione del client
 */
public class MainClassClient {

    public static void main(String[] args) {
        String ip = "localhost";
        try {
            //è possibile specificare l'ip del server da linea di comando
            ip = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: java -cp [list of jars] MainClassClient [optional server ip]");
            System.out.println("No server ip found: server runs on localhost");
        }
        //ogni client è un thread diverso
        Thread clientThread = new Thread(new ClientTasks(ip));
        clientThread.start();
    }
}

