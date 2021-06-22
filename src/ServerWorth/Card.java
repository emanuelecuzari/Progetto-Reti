package ServerWorth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedList;

/**
 * classe per la definzione di una card di un porgetto
 */
public class Card {
    private String name;
    private String descrption;
    /* storico degli spostamenti della card */
    private LinkedList<String> history;

    public Card(String name, String description){
        this.name = name;
        this.descrption = description;
        this.history  = new LinkedList<>();
        //ogni card all'inizio si trovo nella lista dei compiti da svolgere
        history.add("TODO");
    }

    //default constructor for Jackson
    public Card(){
        this.name = null;
        this.descrption = null;
        this.history  = new LinkedList<>();
    }

                                                    /* METODI GET */
    public String getName(){
        return this.name;
    }

    public String getDescrption(){
        return this.descrption;
    }

    @JsonIgnore
    public String getLastPosition(){
        return this.history.getLast();
    }

    public LinkedList<String> getHistory(){
        return this.history;
    }
}
