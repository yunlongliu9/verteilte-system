package vsue.faults;

import java.io.Serializable;

public class RequestMessage implements Serializable {
    private final String methodName;
    private final Object[] parameters;
    private final int objectId;
    private final VSRequestID requestID;

    public RequestMessage(String methodName, Object[] parameters, int objectId, VSRequestID requestID) {
        this.methodName = methodName;
        this.parameters = parameters;
        this.objectId = objectId;
        this.requestID = requestID;
    }

    public String getMethodName() {
        return methodName;
    }
    public Object[] getParameters() {
        return parameters;
    }
    public VSRequestID getRequestID() {
        return requestID;
    }
    public int getObjectId() {
        return objectId;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.requestID.setSequenceNumber(sequenceNumber);
    }
}
