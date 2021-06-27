import ServerWorth.WorthDB;
import java.io.IOException;

/**
 * classe per far partire l'esecuzione del server
 */
public class MainClassServer {

    public static void main(String[] args) throws IOException {
        //Usage: java -cp [list of jars] MainClassServer
        WorthDB db = new WorthDB();
    }
}
