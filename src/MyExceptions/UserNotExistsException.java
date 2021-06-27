package MyExceptions;

public class UserNotExistsException extends Exception{
    public UserNotExistsException(String msg){
        super(msg);
    }
}
