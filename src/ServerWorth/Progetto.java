package ServerWorth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * classe per la definizione di un progetto
 */
public class Progetto {

    /* nome del progetto */
    private String projectName;

    /* creatore del progetto */
    private String creator;

    /* ip della chat */
    private String chatIP;

    /* lista dei membri */
    private LinkedList<String> memberList;

    /* lista che traccia tutte le carte del progetto */
    private transient LinkedList<Card> trackingCards;

    /* LISTE DI LAVORO */
    private LinkedList<Card> todo_list;
    private LinkedList<Card> inProgress_list;
    private LinkedList<Card> toBeRevised_list;
    private LinkedList<Card> done_list;

    public Progetto(String name, String creator){
        if(name == null || creator == null)
            throw new NullPointerException();
        this.projectName = name;
        this.creator = creator;
        this.chatIP = null;
        this.memberList = new LinkedList<>();
        this.memberList.add(creator);
        this.trackingCards = new LinkedList<>();
        this.todo_list = new LinkedList<>();
        this.inProgress_list = new LinkedList<>();
        this.toBeRevised_list = new LinkedList<>();
        this.done_list = new LinkedList<>();
    }

    //default constructor for Jackson
    public Progetto(){
        this.projectName = null;
        this.creator = null;
        this.chatIP = null;
        this.memberList = new LinkedList<>();
        this.trackingCards = new LinkedList<>();
        this.todo_list = new LinkedList<>();
        this.inProgress_list = new LinkedList<>();
        this.toBeRevised_list = new LinkedList<>();
        this.done_list = new LinkedList<>();
    }

                                                    /* METODI GET */
    /* Alcuni metodi sono segnato JsonIgnore perché riferiscono dei campi che non voglio serializzare */

    public LinkedList<String> getMemberList(){
        return this.memberList;
    }

    public String getProjectName(){
        return this.projectName;
    }

    @JsonIgnore
    public LinkedList<Card> getCardTracking(){
        return this.trackingCards;
    }

    @JsonIgnore
    public LinkedList<Card> getTodo_list(){
        return this.todo_list;
    }

    @JsonIgnore
    public LinkedList<Card> getInProgress_list() { return this.inProgress_list; }

    @JsonIgnore
    public LinkedList<Card> getToBeRevised_list() { return this.toBeRevised_list; }

    @JsonIgnore
    public LinkedList<Card> getDone_list(){
        return this.done_list;
    }

    public String getCreator() { return this.creator; }

    public String getChatIP() { return this.chatIP; }

                                                    /* METODI SET */
    public void setCreator(String creator) { this.creator = creator; }

    public void setChatIP(String ip) { this.chatIP = ip; }

    /**
     *
     * metodo per il recupero di una carta di un progetto
     * @param cardName nome della carta da recuperare
     * @return la carta se esiste nel progetto, aòtrimenti null
     */
    public Card getCard(String cardName) throws NullPointerException{
        if(cardName == null) throw new NullPointerException();
        for(Card c : trackingCards){
            if(cardName.equals(c.getName()))
                return c;
        }
        return null;
    }

    /**
     *
     * metodo per il recupero di una delle liste di lavoro tramite nome
     * @param name nome della lista da recuperare
     * @return una LinkedList relativo alla lista di nome name
     * @throws IllegalArgumentException se name non corrisponde al nome di alcuna lista
     */
    public LinkedList<Card> getListByName(String name) throws NullPointerException, IllegalArgumentException{
        if(name == null) throw new NullPointerException();
        if(name.equalsIgnoreCase("TODO")) return this.todo_list;
        if(name.equalsIgnoreCase("IN_PROGRESS")) return this.inProgress_list;
        if(name.equalsIgnoreCase("TO_BE_REVISED")) return this.toBeRevised_list;
        if(name.equalsIgnoreCase("DONE")) return this.done_list;
        throw new IllegalArgumentException();
    }

    /**
     * metodo per aggiungere le cards alla lista in cui si trovavno l'ultima volta
     * metodo usato per la deserializzazione
     * @param lst lista di tutte le carte di un progetto
     */
    public void addAllCards(ArrayList<Card> lst){
        for(Card c : lst){
            this.getCardTracking().add(c);
            if(c.getLastPosition().equals("TODO")) this.getTodo_list().add(c);
            if(c.getLastPosition().equals("IN_PROGRESS")) this.getInProgress_list().add(c);
            if(c.getLastPosition().equals("TO_BE_REVISED")) this.getToBeRevised_list().add(c);
            if(c.getLastPosition().equals("DONE")) this.getDone_list().add(c);
        }
    }
}
