package ServerWorth;

/**
 * classe per la definizione di un utente WORTH
 */
public class Utente {
    private String username;
    private String passw;
    private String stato;

    public Utente(String username, String passw){
        this.username = username;
        this.passw = passw;
        stato = "offline";
    }

    //default constructor for Jackson
    public Utente(){
        super();
    }

                                                /* METODI GET e SET */

    public String getStato(){
        return this.stato;
    }

    public String getUsername(){
        return this.username;
    }

    public String getPassw(){
        return this.passw;
    }

    public void setStato(String stato){
        this.stato = stato;
    }
}
