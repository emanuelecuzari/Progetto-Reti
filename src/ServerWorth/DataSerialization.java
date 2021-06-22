package ServerWorth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import static java.lang.System.exit;

/**
 * classe per la serializzazione e la deserializzazione dei dati
 */
public class DataSerialization {

    //Paths delle cartelle che memorizzano i dati importanti di WORTH
    private static String usersData = "/UsersData";
    private static String projectsData = "/ProjectsData";
    private static String data = "../Data";

    private File dataPath;
    private File usersPath;
    private File projectsPath;
    //ObjectMapper per effettuare la serializzazione e la deserializzazione
    private ObjectMapper mapper;

    public DataSerialization(){
        this.dataPath = new File(data);
        this.projectsPath = new File(data + projectsData);
        this.usersPath = new File(data + usersData);
        this.mapper = new ObjectMapper();
        //creo la directory prinicipale per il salvataggio dei dati, se non esiste già
        if(!this.dataPath.exists()){
            if(!createStorage()){
                System.out.println("Error in creating the storage paths");
                exit(0);
            }
        }
    }

    //metodo per la creazione delle cartelle di memorizzazione dei dati
    public boolean createStorage(){
        return this.dataPath.mkdir() && this.usersPath.mkdir() && this.projectsPath.mkdir();
    }

    /**
     * metodo per serializzare le informazioni dell'utente
     * @param user utente di cui eseguo la serializzazione
     * @throws IOException se occore un errore in writeValue
     */
    public void serializeUsers(Utente user) throws IOException {
        File uFile = new File((usersPath + "/" + user.getUsername()) + ".json");
        if(!uFile.exists()){
            boolean newFile = uFile.createNewFile();
        }
        mapper.writeValue(uFile, user);
    }

    /**
     * metodo per la serializzazione delle informazioni del progetto
     * @param prj progetto di cui eseguo la serializzazione
     * @throws IOException se occore un errore in writeValue
     */
    public void serializeProject(Progetto prj) throws IOException {
        File project = new File( projectsPath + "/" + prj.getProjectName() + ".json");
        //se il file del porgetto esiste già, aggiorno solo; altrimenti lo devo prima creare
        if(!project.exists()) {
            boolean newFilePrj = project.createNewFile();
        }
        mapper.writeValue(project, prj);
        //quando serializzo il progetto serializzo pure le card, ma lo faccio in una directory a parte che ha lo stesso
        //nome del progetto
        File card;
        File cardDir = new File(projectsPath + "/" + prj.getProjectName());
        if(!cardDir.exists()){
            boolean mkdir = cardDir.mkdir();
        }
        //per ogni card creo un nuovo file se non esiste già, poi serializzo
        for (Card c : prj.getCardTracking()) {
            card = new File(cardDir + "/" + c.getName() + ".json");
            if (!card.exists()) {
                boolean newFile = card.createNewFile();
            }
            mapper.writeValue(card, c);
        }
    }

    /**
     * metodo per deserializzare un progetto
     * @return projects, lista con tutti i progetti creati
     * @throws IOException se occore un errore in readValue
     */
    public ArrayList<Progetto> deserializeProjects() throws IOException {
        ArrayList<Progetto> projects = new ArrayList<>();
        if (this.projectsPath.isDirectory()) {
            //recupero tutti i paths nella directory dove memorizzo le informazioni dei progetti e delle relative cards
            String[] projectsPaths = this.projectsPath.list();
            if (projectsPaths != null) {
                File pFile;
                File cFile;
                File cDir;
                Progetto proj;
                //lista che conterrà tutte le cards di un progetto
                ArrayList<Card> cards = new ArrayList<>();
                Arrays.sort(projectsPaths);
                for (String s : projectsPaths) {
                    pFile = new File(projectsPath + "/" + s);
                    //se trovo un directory, potrebbe essere relativa alle cards, per ora la salto
                    if (pFile.isDirectory()) continue;
                    proj = mapper.readValue(pFile, Progetto.class);
                    cDir = new File(projectsPath + "/" + proj.getProjectName());
                    //deserializzazione delle cards
                    if (cDir.isDirectory()) {
                        String[] cPaths = cDir.list();
                        if (cPaths != null) {
                            Arrays.sort(cPaths);
                            for (String str : cPaths) {
                                cFile = new File(cDir + "/" + str);
                                cards.add(mapper.readValue(cFile, Card.class));
                            }
                        }
                    }
                    //aggiungo tutte le cards nelle liste di lavoro corrette
                    proj.addAllCards(cards);
                    //svuoto l'array di cards per la deserializzazione successiva
                    cards.clear();
                    projects.add(proj);
                }
            }
        }
        return projects;
    }

    /**
     *  metodo per deserializzazione dei dati relativi agli utenti
     * @return users, lista con tutti gli utenti registrati
     */
    public ArrayList<Utente> deserializeUsers(){
        ArrayList<Utente> users = new ArrayList<>();
        if(this.usersPath.isDirectory()){
            String[] usernamePaths = this.usersPath.list();
            if(usernamePaths != null){
                Arrays.sort(usernamePaths);
                File file;
                for(String s : usernamePaths){
                    file = new File(usersPath + "/" + s);
                    try{
                        users.add(mapper.readValue(file, Utente.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return users;
    }

    /**
     * metodo per eliminare un progetto e le sue cards
     * @param prj progetto da eliminare
     */
    public void endProject(Progetto prj){
        //recupero i file e controllo che esistano
        File pFile = new File(projectsPath + "/" + prj.getProjectName() + ".json");
        File cDir = new File(projectsPath + "/" + prj.getProjectName());
        if(pFile.exists()){
            //elimino iterativamente tutte le carte prima di eliminare il progetto
            if(cDir.exists()) deleteAll(cDir);
            if(pFile.delete()) System.out.println("Project ended!");
            else System.out.println("Error in terminating the project");
        }
    }

    /**
     * metodo per eliminare iterativamente tutte le carte
     * @param dir directory delle cards da eliminare
     */
    public void deleteAll(File dir){
        String[] paths = dir.list();
        File card;
        if(paths != null){
            //elimino tutti i file delle cards
            for(String s : paths){
                card = new File(dir + "/" + s);
                if(!card.delete()) System.out.println("Error in cancelling " + card.getName());
            }
            //per ultima elimino la directory
            if(!dir.delete()) System.out.println("Error in cancelling " + dir.getName());
        }
    }
}