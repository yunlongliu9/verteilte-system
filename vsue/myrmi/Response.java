package vsue.myrmi;
import java.io.Serializable;

public class Response implements Serializable {
    public Object result;
    public Exception exception;
}