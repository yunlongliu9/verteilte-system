package vsue.rpc;

import java.io.Serializable;

public class RequestMessage implements Serializable {
    private final String methodName;
    private final Object[] parameters;
    private final int objectId;

    public RequestMessage(String methodName, Object[] parameters, int objectId) {
        this.methodName = methodName;
        this.parameters = parameters;
        this.objectId = objectId;
    }

    public String getMethodName() {
        return methodName;
    }
    public Object[] getParameters() {
        return parameters;
    }
    public int getObjectId() {
        return objectId;
    }
}
