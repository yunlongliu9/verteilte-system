package vsue.faults;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.UUID;

public class VSRequestID implements Externalizable {

    private String callID;  // [temporary] modify später
    private int sequenceNumber; // versuch mal

    public VSRequestID(){}

    public VSRequestID(String username,String methodName) {
        this.callID = username + methodName + UUID.randomUUID();
        this.sequenceNumber = 1;
    }
    
    public VSRequestID(String callID, int seq ) {
        this.callID = callID;
        this.sequenceNumber = seq;
    }

    public void setCallID(String callID){
        this.callID = callID;
    }

    public String getCallID() {
        return callID;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int neuVersuchAnzahl){
        sequenceNumber = neuVersuchAnzahl;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(callID != null ? callID : "");
        out.writeInt(sequenceNumber);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.callID = in.readUTF();
        this.sequenceNumber = in.readInt();
    }
}