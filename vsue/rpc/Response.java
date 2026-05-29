package vsue.rpc;

import java.io.*;

import vsue.faults.VSRequestID;

public class Response implements Externalizable {
    private Object result;
    private Throwable exception;
    private VSRequestID vsRequestID;

    public Response(Object result, Throwable exception, VSRequestID vsRequestID) {
        this.result = result;
        this.exception = exception;
        this.vsRequestID = vsRequestID;
    }

    public Response() {
        this.result = null;
        this.exception = null;
        this.vsRequestID = null;
    }

    public Object getResult() {
        return result;
    }

    public VSRequestID getRequestID(){
        return vsRequestID;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        // VSRequestID = callid + seq
        out.writeUTF(vsRequestID.getCallID());
        out.writeInt(vsRequestID.getSequenceNumber());

        // First byte indicates if there is an exception (1) or not (0)
        out.writeByte(exception != null ? 1 : 0);
        if (exception != null) {
            out.writeObject(exception);
        } else {
            out.writeObject(result);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        String callID = in.readUTF();
        int seq = in.readInt();
        this.vsRequestID = new VSRequestID(callID,seq);

        byte hasException = in.readByte();
        if (hasException == 1) {
            this.exception = (Throwable) in.readObject();
            this.result = null;
        } else {
            this.result = in.readObject();
            this.exception = null;
        }
    }
}