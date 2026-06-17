package vsue.rpc;

import vsue.faults.VSRequestID;
import vsue.faults.VSRPCSemanticType;
import java.io.*;

public class Request implements Serializable {

    private int objectId;
    private int methodNameHash;
    private Object[] parameters;
    private VSRequestID vsRequestID;
    public VSRPCSemanticType semantic = null;
    // user für AMO
    public String user;
    public int AMOcallID;

    public Request(int objectId, int methodNameHash, Object[] parameters,VSRequestID vsRequestID, String user) {
        this.objectId = objectId;
        this.methodNameHash = methodNameHash;
        this.parameters = parameters;
        this.vsRequestID = vsRequestID;
        this.user = user;
    }

    public Request(int objectId, int methodNameHash, Object[] parameters) {
        this.objectId = objectId;
        this.methodNameHash = methodNameHash;
        this.parameters = parameters;
    }

    public Request() {
        // No-arg constructor for Externalizable
    }

    public int getMethodName() {
        return this.methodNameHash;
    }

    public VSRequestID getRequestID() {
        return vsRequestID;
    }

    public Object[] getParameters() {
        return this.parameters;
    }

    public int getObjectId() {
        return this.objectId;
    }
    
    public void setRequestID(VSRequestID newID) {
        vsRequestID = newID;
    }
}
