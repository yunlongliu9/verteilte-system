package vsue.rpc;

import java.io.Serializable;

public class ReplyMessage implements Serializable {
    private final Object result;
    private final Throwable exception;


    public ReplyMessage(Object result, Throwable exception) {
        this.result = result;
        this.exception = exception;
    }

    public Object getResult() {
        return result;
    }

    public Throwable getException() {
        return exception;
    }
}
