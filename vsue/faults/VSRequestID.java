package vsue.faults;

import java.io.Serializable;

public class VSRequestID implements Serializable {

    private final String callID;
    private int sequenceNumber;

    public VSRequestID(String callID, int sequenceNumber) {
        this.callID = callID;
        this.sequenceNumber = sequenceNumber;
    }

    public String getCallID() {
        return callID;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}