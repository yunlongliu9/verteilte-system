package vsue.faults;

import java.io.Serializable;

public class ReplyMessage implements Serializable {
    private final Object result;
    private final Throwable exception;
    private final VSRequestID requestID;   


    public ReplyMessage(Object result, Throwable exception, VSRequestID requestID) {
        this.result = result;
        this.exception = exception;
        this.requestID = requestID;
    }

    public Object getResult() {
        return result;
    }

    public VSRequestID getRequestID() {
        return requestID;
    }

    public Throwable getException() {
        return exception;
    }
}
