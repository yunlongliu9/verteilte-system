package vsue.rpc;

import vsue.faults.VSRequestID;
import vsue.rmi.VSTestMessage;
import java.io.*;
import static vsue.rpc.Encoding.*;

public class Request implements Externalizable {

    private int objectId;
    private int methodNameHash;
    private Object[] parameters;
    private VSRequestID vsRequestID;

    public Request(int objectId, int methodNameHash, Object[] parameters,VSRequestID vsRequestID) {
        this.objectId = objectId;
        this.methodNameHash = methodNameHash;
        this.parameters = parameters;
        this.vsRequestID = vsRequestID;
    }

    // Nutze effiziente implementierung der VSTestMessage statt standard Java
    // Serialisierung
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

    private byte spaceForParameterLength;
    private byte spaceForID;
    private byte spaceForHash;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        if (parameters == null) {
            spaceForParameterLength = ZERO_BYTES;
        } else {
            spaceForParameterLength = requiredSpace(parameters.length);
        }
        spaceForID = requiredSpace(objectId);
        spaceForHash = requiredSpace(methodNameHash);
        byte header = encode();

        out.writeByte(header);
        
        // 1. callid
        out.writeUTF(vsRequestID.getCallID());
        // 2. seq size
        byte seqSize = requiredSpace(vsRequestID.getSequenceNumber());
        out.writeByte(seqSize);
        write(out, vsRequestID.getSequenceNumber(), seqSize);
        
        write(out, objectId, spaceForID);
        write(out, methodNameHash, spaceForHash);
        switch (spaceForParameterLength) {
        case ONE_BYTE:
        case TWO_BYTES:
        case THREE_BYTES:
        case FOUR_BYTES:
            write(out, parameters.length, spaceForParameterLength);
            for (Object p : parameters) {
                out.writeObject(p);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException
    {
        byte header = in.readByte();
        decode(header);
 
        // vielleicht gibt es neu problem in req/res externalize
        String callID = (String) in.readUTF();
        byte seqSize = in.readByte(); 
        int seq = read(in, seqSize);
        this.vsRequestID = new VSRequestID();
        this.vsRequestID.setCallID(callID);
        this.vsRequestID.setSequenceNumber(seq);
        
        objectId = read(in, spaceForID);
        methodNameHash = read(in, spaceForHash);
        switch (spaceForParameterLength) {
        case ZERO_BYTES:
            parameters = null;
        case ONE_BYTE:
        case TWO_BYTES:
        case THREE_BYTES:
        case FOUR_BYTES:
            int length = read(in, spaceForParameterLength);
            parameters = new Object[length];
            for (int i = 0; i < length; i++) {
                parameters[i] = in.readObject();
            }
        }
    }

    private void decode(byte header)
    {
        switch (header) {
        case 0:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = ONE_BYTE;
            break;
        case 1:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = TWO_BYTES;
            break;
        case 2:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = THREE_BYTES;
            break;
        case 3:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = FOUR_BYTES;
            break;
        case 4:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 5:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 6:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 7:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 8:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 9:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 10:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 11:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 12:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 13:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 14:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 15:
            spaceForParameterLength = ZERO_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 16:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = ONE_BYTE;
            spaceForID = ONE_BYTE;
            break;
        case 17:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = ONE_BYTE;
            spaceForID = TWO_BYTES;
            break;
        case 18:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = ONE_BYTE;
            spaceForID = THREE_BYTES;
            break;
        case 19:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = ONE_BYTE;
            spaceForID = FOUR_BYTES;
            break;
        case 20:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = TWO_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 21:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = TWO_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 22:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = TWO_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 23:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = TWO_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 24:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = THREE_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 25:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = THREE_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 26:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = THREE_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 27:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = THREE_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 28:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = FOUR_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 29:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = FOUR_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 30:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = FOUR_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 31:
            spaceForParameterLength = ONE_BYTE;
            spaceForHash = FOUR_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 32:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = ONE_BYTE;
            break;
        case 33:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = TWO_BYTES;
            break;
        case 34:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = THREE_BYTES;
            break;
        case 35:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = FOUR_BYTES;
            break;
        case 36:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 37:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 38:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 39:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 40:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 41:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 42:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 43:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 44:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 45:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 46:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 47:
            spaceForParameterLength = TWO_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 48:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = ONE_BYTE;
            break;
        case 49:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = TWO_BYTES;
            break;
        case 50:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = THREE_BYTES;
            break;
        case 51:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = FOUR_BYTES;
            break;
        case 52:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 53:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 54:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 55:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 56:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 57:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 58:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 59:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 60:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 61:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 62:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 63:
            spaceForParameterLength = THREE_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 64:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = ONE_BYTE;
            break;
        case 65:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = TWO_BYTES;
            break;
        case 66:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = THREE_BYTES;
            break;
        case 67:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = ONE_BYTE;
            spaceForID = FOUR_BYTES;
            break;
        case 68:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 69:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 70:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 71:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = TWO_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 72:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 73:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 74:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 75:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = THREE_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        case 76:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = ONE_BYTE;
            break;
        case 77:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = TWO_BYTES;
            break;
        case 78:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = THREE_BYTES;
            break;
        case 79:
            spaceForParameterLength = FOUR_BYTES;
            spaceForHash = FOUR_BYTES;
            spaceForID = FOUR_BYTES;
            break;
        default:
            throw new RuntimeException("invalid header value");
        }
    }

    private byte encode()
    {
        switch (spaceForParameterLength) {
        case ZERO_BYTES:
            switch (spaceForHash) {
            case ONE_BYTE:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 0;
                case TWO_BYTES:
                    return 1;
                case THREE_BYTES:
                    return 2;
                case FOUR_BYTES:
                    return 3;
                }
            case TWO_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 4;
                case TWO_BYTES:
                    return 5;
                case THREE_BYTES:
                    return 6;
                case FOUR_BYTES:
                    return 7;
                }
            case THREE_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 8;
                case TWO_BYTES:
                    return 9;
                case THREE_BYTES:
                    return 10;
                case FOUR_BYTES:
                    return 11;
                }
            case FOUR_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 12;
                case TWO_BYTES:
                    return 13;
                case THREE_BYTES:
                    return 14;
                case FOUR_BYTES:
                    return 15;
                }
            }
        case ONE_BYTE:
            switch (spaceForHash) {
            case ONE_BYTE:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 16;
                case TWO_BYTES:
                    return 17;
                case THREE_BYTES:
                    return 18;
                case FOUR_BYTES:
                    return 19;
                }
            case TWO_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 20;
                case TWO_BYTES:
                    return 21;
                case THREE_BYTES:
                    return 22;
                case FOUR_BYTES:
                    return 23;
                }
            case THREE_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 24;
                case TWO_BYTES:
                    return 25;
                case THREE_BYTES:
                    return 26;
                case FOUR_BYTES:
                    return 27;
                }
            case FOUR_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 28;
                case TWO_BYTES:
                    return 29;
                case THREE_BYTES:
                    return 30;
                case FOUR_BYTES:
                    return 31;
                }
            }
        case TWO_BYTES:
            switch (spaceForHash) {
            case ONE_BYTE:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 32;
                case TWO_BYTES:
                    return 33;
                case THREE_BYTES:
                    return 34;
                case FOUR_BYTES:
                    return 35;
                }
            case TWO_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 36;
                case TWO_BYTES:
                    return 37;
                case THREE_BYTES:
                    return 38;
                case FOUR_BYTES:
                    return 39;
                }
            case THREE_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 40;
                case TWO_BYTES:
                    return 41;
                case THREE_BYTES:
                    return 42;
                case FOUR_BYTES:
                    return 43;
                }
            case FOUR_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 44;
                case TWO_BYTES:
                    return 45;
                case THREE_BYTES:
                    return 46;
                case FOUR_BYTES:
                    return 47;
                }
            }
        case THREE_BYTES:
            switch (spaceForHash) {
            case ONE_BYTE:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 48;
                case TWO_BYTES:
                    return 49;
                case THREE_BYTES:
                    return 50;
                case FOUR_BYTES:
                    return 51;
                }
            case TWO_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 52;
                case TWO_BYTES:
                    return 53;
                case THREE_BYTES:
                    return 54;
                case FOUR_BYTES:
                    return 55;
                }
            case THREE_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 56;
                case TWO_BYTES:
                    return 57;
                case THREE_BYTES:
                    return 58;
                case FOUR_BYTES:
                    return 59;
                }
            case FOUR_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 60;
                case TWO_BYTES:
                    return 61;
                case THREE_BYTES:
                    return 62;
                case FOUR_BYTES:
                    return 63;
                }
            }
        case FOUR_BYTES:
            switch (spaceForHash) {
            case ONE_BYTE:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 64;
                case TWO_BYTES:
                    return 65;
                case THREE_BYTES:
                    return 66;
                case FOUR_BYTES:
                    return 67;
                }
            case TWO_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 68;
                case TWO_BYTES:
                    return 69;
                case THREE_BYTES:
                    return 70;
                case FOUR_BYTES:
                    return 71;
                }
            case THREE_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 72;
                case TWO_BYTES:
                    return 73;
                case THREE_BYTES:
                    return 74;
                case FOUR_BYTES:
                    return 75;
                }
            case FOUR_BYTES:
                switch (spaceForID) {
                case ONE_BYTE:
                    return 76;
                case TWO_BYTES:
                    return 77;
                case THREE_BYTES:
                    return 78;
                case FOUR_BYTES:
                    return 79;
                }
            }
        }
        throw new RuntimeException("invalid space info");
    }
}
