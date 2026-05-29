package vsue.rmi;

import java.io.*;

public class VSTestMessage implements Externalizable {
    public int integer;
    public String string;
    public Object[] objects;
    public boolean verbose; // Darüber wird unten Systemoutput reguliert.
    private byte spaceForInteger;
    private byte spaceForStringLength;
    private byte spaceForObjectCount;

    public VSTestMessage(int integer, String string, Object[] objects) {
        this.integer = integer;
        this.string = string;
        this.objects = objects;
        verbose = false;
        spaceForStringLength = NONE;
        spaceForObjectCount = NONE;
    }
    
    // Leerer Konstruktor wegen Externalizable
    public VSTestMessage() {}

    private final byte ZERO_BYTES = 0;
    private final byte ONE_BYTE = 1;
    private final byte TWO_BYTES = 2;
    private final byte THREE_BYTES = 3;
    private final byte FOUR_BYTES = 4;
    private final byte NONE = 5;

    private byte requiredSpace(int value)
    {
        if (value == 0) {
            return ZERO_BYTES;
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return ONE_BYTE;
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return TWO_BYTES;
        } else if (value >= -8388608 && value <= 8388607) {
            return THREE_BYTES;
        } else {            
            return FOUR_BYTES;
        }
    }
    
    private void write(ObjectOutput out, int value, byte size) throws IOException
    {
        switch (size) {
        case ONE_BYTE:
            out.writeByte(value);
            break;
        case TWO_BYTES:
            out.writeShort(value);
            break;
        case THREE_BYTES:
            out.writeByte(value);
            out.writeByte(value >> 8);
            out.writeByte(value >> 16);
            break;
        case FOUR_BYTES:
            out.writeInt(value);
            break;
        }
    }

    private int read(ObjectInput in, byte size) throws IOException
    {
        int result = 0;
        switch (size) {
        case ONE_BYTE:
            result = in.readByte();
            break;
        case TWO_BYTES:
            result = in.readShort();
            break;
        case THREE_BYTES:
            result |= in.readByte();
            result |= in.readByte() << 8;
            result |= in.readByte() << 16;
            break;
        case FOUR_BYTES:
            result = in.readInt();
            break;
        }
        return result;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        byte[] stringBytes = null;

        spaceForInteger = requiredSpace(integer);

        if (string != null) {
            stringBytes = string.getBytes();
            spaceForStringLength = requiredSpace(stringBytes.length);
        }

        if (objects != null) {
            spaceForObjectCount = requiredSpace(objects.length);
        }

        byte header = compressSpaceInfo();
        if (verbose) {
            System.out.println("Header value: " + header);
        }
        out.writeByte(header);


        switch (spaceForInteger) {
        case ONE_BYTE:
        case TWO_BYTES:
        case THREE_BYTES:
        case FOUR_BYTES:
            write(out, integer, spaceForInteger);
        }

        switch (spaceForStringLength) {
        case ONE_BYTE:
        case TWO_BYTES:
        case THREE_BYTES:
        case FOUR_BYTES:
            write(out, stringBytes.length, spaceForStringLength);
            out.write(stringBytes);
        }

        switch (spaceForObjectCount) {
        case ONE_BYTE:
        case TWO_BYTES:
        case THREE_BYTES:
        case FOUR_BYTES:
            write(out, objects.length, spaceForObjectCount);
            for (Object obj : objects) {
                out.writeObject(obj); // Use default serialization for each object
            }
        }

        out.flush();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        byte header = in.readByte();
        extractSpaceInfo(header);

        integer = read(in, spaceForInteger);

        switch (spaceForStringLength) {
        case NONE:
            string = null;
            break;
        default:
            int length = read(in, spaceForStringLength);
            byte[] stringBytes = new byte[length];
            in.readFully(stringBytes);
            string = new String(stringBytes);
        }

        switch (spaceForObjectCount) {
        case NONE:
            objects = null;
            break;
        default:
            int length = read(in, spaceForObjectCount);
            objects = new Object[length];
            for (int i = 0; i < length; i++) {
                objects[i] = in.readObject();
            }
        }
    }

    public boolean equals(Object other)
    {
        if (other instanceof VSTestMessage) {
                VSTestMessage otherVSTestMessage = (VSTestMessage) other;
                boolean integersMatch = false;
                if (integer == otherVSTestMessage.integer) {
                    integersMatch = true;
                }
                boolean stringsMatch = false;
                if ((string == null && otherVSTestMessage.string == null)
                    || (string.equals(otherVSTestMessage.string))
                ) {
                    stringsMatch = true;
                }
                boolean objectsMatch = false;
                if ((objects == null && otherVSTestMessage.objects == null)
                    || (objects.equals(otherVSTestMessage.string))
                ) {
                    objectsMatch = true;
                }
                return integersMatch && stringsMatch && objectsMatch;
        } else {
            return false;
        }
    }

    private byte compressSpaceInfo()
    {
        // Der switch ist automatisch generiert.
        // Für Anpassungen und weitere Infos siehe vsue.tools.SpaceInfoSwitchGenerator
        switch (spaceForInteger) {
        case ZERO_BYTES:
            switch (spaceForStringLength) {
            case ZERO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 0;
                case ONE_BYTE:
                    return 1;
                case TWO_BYTES:
                    return 2;
                case THREE_BYTES:
                    return 3;
                case FOUR_BYTES:
                    return 4;
                case NONE:
                    return 5;
                }
            case ONE_BYTE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 6;
                case ONE_BYTE:
                    return 7;
                case TWO_BYTES:
                    return 8;
                case THREE_BYTES:
                    return 9;
                case FOUR_BYTES:
                    return 10;
                case NONE:
                    return 11;
                }
            case TWO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 12;
                case ONE_BYTE:
                    return 13;
                case TWO_BYTES:
                    return 14;
                case THREE_BYTES:
                    return 15;
                case FOUR_BYTES:
                    return 16;
                case NONE:
                    return 17;
                }
            case THREE_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 18;
                case ONE_BYTE:
                    return 19;
                case TWO_BYTES:
                    return 20;
                case THREE_BYTES:
                    return 21;
                case FOUR_BYTES:
                    return 22;
                case NONE:
                    return 23;
                }
            case FOUR_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 24;
                case ONE_BYTE:
                    return 25;
                case TWO_BYTES:
                    return 26;
                case THREE_BYTES:
                    return 27;
                case FOUR_BYTES:
                    return 28;
                case NONE:
                    return 29;
                }
            case NONE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 30;
                case ONE_BYTE:
                    return 31;
                case TWO_BYTES:
                    return 32;
                case THREE_BYTES:
                    return 33;
                case FOUR_BYTES:
                    return 34;
                case NONE:
                    return 35;
                }
            }
        case ONE_BYTE:
            switch (spaceForStringLength) {
            case ZERO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 36;
                case ONE_BYTE:
                    return 37;
                case TWO_BYTES:
                    return 38;
                case THREE_BYTES:
                    return 39;
                case FOUR_BYTES:
                    return 40;
                case NONE:
                    return 41;
                }
            case ONE_BYTE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 42;
                case ONE_BYTE:
                    return 43;
                case TWO_BYTES:
                    return 44;
                case THREE_BYTES:
                    return 45;
                case FOUR_BYTES:
                    return 46;
                case NONE:
                    return 47;
                }
            case TWO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 48;
                case ONE_BYTE:
                    return 49;
                case TWO_BYTES:
                    return 50;
                case THREE_BYTES:
                    return 51;
                case FOUR_BYTES:
                    return 52;
                case NONE:
                    return 53;
                }
            case THREE_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 54;
                case ONE_BYTE:
                    return 55;
                case TWO_BYTES:
                    return 56;
                case THREE_BYTES:
                    return 57;
                case FOUR_BYTES:
                    return 58;
                case NONE:
                    return 59;
                }
            case FOUR_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 60;
                case ONE_BYTE:
                    return 61;
                case TWO_BYTES:
                    return 62;
                case THREE_BYTES:
                    return 63;
                case FOUR_BYTES:
                    return 64;
                case NONE:
                    return 65;
                }
            case NONE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 66;
                case ONE_BYTE:
                    return 67;
                case TWO_BYTES:
                    return 68;
                case THREE_BYTES:
                    return 69;
                case FOUR_BYTES:
                    return 70;
                case NONE:
                    return 71;
                }
            }
        case TWO_BYTES:
            switch (spaceForStringLength) {
            case ZERO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 72;
                case ONE_BYTE:
                    return 73;
                case TWO_BYTES:
                    return 74;
                case THREE_BYTES:
                    return 75;
                case FOUR_BYTES:
                    return 76;
                case NONE:
                    return 77;
                }
            case ONE_BYTE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 78;
                case ONE_BYTE:
                    return 79;
                case TWO_BYTES:
                    return 80;
                case THREE_BYTES:
                    return 81;
                case FOUR_BYTES:
                    return 82;
                case NONE:
                    return 83;
                }
            case TWO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 84;
                case ONE_BYTE:
                    return 85;
                case TWO_BYTES:
                    return 86;
                case THREE_BYTES:
                    return 87;
                case FOUR_BYTES:
                    return 88;
                case NONE:
                    return 89;
                }
            case THREE_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 90;
                case ONE_BYTE:
                    return 91;
                case TWO_BYTES:
                    return 92;
                case THREE_BYTES:
                    return 93;
                case FOUR_BYTES:
                    return 94;
                case NONE:
                    return 95;
                }
            case FOUR_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 96;
                case ONE_BYTE:
                    return 97;
                case TWO_BYTES:
                    return 98;
                case THREE_BYTES:
                    return 99;
                case FOUR_BYTES:
                    return 100;
                case NONE:
                    return 101;
                }
            case NONE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 102;
                case ONE_BYTE:
                    return 103;
                case TWO_BYTES:
                    return 104;
                case THREE_BYTES:
                    return 105;
                case FOUR_BYTES:
                    return 106;
                case NONE:
                    return 107;
                }
            }
        case THREE_BYTES:
            switch (spaceForStringLength) {
            case ZERO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 108;
                case ONE_BYTE:
                    return 109;
                case TWO_BYTES:
                    return 110;
                case THREE_BYTES:
                    return 111;
                case FOUR_BYTES:
                    return 112;
                case NONE:
                    return 113;
                }
            case ONE_BYTE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 114;
                case ONE_BYTE:
                    return 115;
                case TWO_BYTES:
                    return 116;
                case THREE_BYTES:
                    return 117;
                case FOUR_BYTES:
                    return 118;
                case NONE:
                    return 119;
                }
            case TWO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 120;
                case ONE_BYTE:
                    return 121;
                case TWO_BYTES:
                    return 122;
                case THREE_BYTES:
                    return 123;
                case FOUR_BYTES:
                    return 124;
                case NONE:
                    return 125;
                }
            case THREE_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return 126;
                case ONE_BYTE:
                    return 127;
                case TWO_BYTES:
                    return -128;
                case THREE_BYTES:
                    return -127;
                case FOUR_BYTES:
                    return -126;
                case NONE:
                    return -125;
                }
            case FOUR_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return -124;
                case ONE_BYTE:
                    return -123;
                case TWO_BYTES:
                    return -122;
                case THREE_BYTES:
                    return -121;
                case FOUR_BYTES:
                    return -120;
                case NONE:
                    return -119;
                }
            case NONE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return -118;
                case ONE_BYTE:
                    return -117;
                case TWO_BYTES:
                    return -116;
                case THREE_BYTES:
                    return -115;
                case FOUR_BYTES:
                    return -114;
                case NONE:
                    return -113;
                }
            }
        case FOUR_BYTES:
            switch (spaceForStringLength) {
            case ZERO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return -112;
                case ONE_BYTE:
                    return -111;
                case TWO_BYTES:
                    return -110;
                case THREE_BYTES:
                    return -109;
                case FOUR_BYTES:
                    return -108;
                case NONE:
                    return -107;
                }
            case ONE_BYTE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return -106;
                case ONE_BYTE:
                    return -105;
                case TWO_BYTES:
                    return -104;
                case THREE_BYTES:
                    return -103;
                case FOUR_BYTES:
                    return -102;
                case NONE:
                    return -101;
                }
            case TWO_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return -100;
                case ONE_BYTE:
                    return -99;
                case TWO_BYTES:
                    return -98;
                case THREE_BYTES:
                    return -97;
                case FOUR_BYTES:
                    return -96;
                case NONE:
                    return -95;
                }
            case THREE_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return -94;
                case ONE_BYTE:
                    return -93;
                case TWO_BYTES:
                    return -92;
                case THREE_BYTES:
                    return -91;
                case FOUR_BYTES:
                    return -90;
                case NONE:
                    return -89;
                }
            case FOUR_BYTES:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return -88;
                case ONE_BYTE:
                    return -87;
                case TWO_BYTES:
                    return -86;
                case THREE_BYTES:
                    return -85;
                case FOUR_BYTES:
                    return -84;
                case NONE:
                    return -83;
                }
            case NONE:
                switch (spaceForObjectCount) {
                case ZERO_BYTES:
                    return -82;
                case ONE_BYTE:
                    return -81;
                case TWO_BYTES:
                    return -80;
                case THREE_BYTES:
                    return -79;
                case FOUR_BYTES:
                    return -78;
                case NONE:
                    return -77;
                }
            }
        }
        throw new RuntimeException("compressSpaceInfo: invalid spaceInfo");
    }

    private void extractSpaceInfo(byte header)
    {
        // Der switch ist automatisch generiert.
        // Für Anpassungen und weitere Infos siehe vsue.tools.SpaceInfoSwitchGenerator
        switch (header) {
        case 0:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 1:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 2:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 3:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 4:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 5:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 6:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 7:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 8:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 9:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 10:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 11:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = NONE;
            break;
        case 12:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 13:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 14:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 15:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 16:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 17:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 18:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 19:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 20:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 21:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 22:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 23:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 24:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 25:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 26:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 27:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 28:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 29:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 30:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 31:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 32:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 33:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 34:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 35:
            spaceForInteger = ZERO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = NONE;
            break;
        case 36:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 37:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 38:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 39:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 40:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 41:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 42:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 43:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 44:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 45:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 46:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 47:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = NONE;
            break;
        case 48:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 49:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 50:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 51:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 52:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 53:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 54:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 55:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 56:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 57:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 58:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 59:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 60:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 61:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 62:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 63:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 64:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 65:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 66:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = NONE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 67:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = NONE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 68:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = NONE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 69:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = NONE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 70:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = NONE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 71:
            spaceForInteger = ONE_BYTE;
            spaceForStringLength = NONE;
            spaceForObjectCount = NONE;
            break;
        case 72:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 73:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 74:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 75:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 76:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 77:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 78:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 79:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 80:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 81:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 82:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 83:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = NONE;
            break;
        case 84:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 85:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 86:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 87:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 88:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 89:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 90:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 91:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 92:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 93:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 94:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 95:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 96:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 97:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 98:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 99:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 100:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 101:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 102:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 103:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 104:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 105:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 106:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 107:
            spaceForInteger = TWO_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = NONE;
            break;
        case 108:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 109:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 110:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 111:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 112:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 113:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 114:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 115:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 116:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 117:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 118:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 119:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = NONE;
            break;
        case 120:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 121:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case 122:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case 123:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case 124:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case 125:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case 126:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case 127:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case -128:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case -127:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case -126:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case -125:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = NONE;
            break;
        case -124:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case -123:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case -122:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case -121:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case -120:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case -119:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = NONE;
            break;
        case -118:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case -117:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case -116:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case -115:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case -114:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case -113:
            spaceForInteger = THREE_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = NONE;
            break;
        case -112:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case -111:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case -110:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case -109:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case -108:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case -107:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ZERO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case -106:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case -105:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case -104:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case -103:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case -102:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case -101:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = ONE_BYTE;
            spaceForObjectCount = NONE;
            break;
        case -100:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case -99:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case -98:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case -97:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case -96:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case -95:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = TWO_BYTES;
            spaceForObjectCount = NONE;
            break;
        case -94:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case -93:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case -92:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case -91:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case -90:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case -89:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = THREE_BYTES;
            spaceForObjectCount = NONE;
            break;
        case -88:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case -87:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = ONE_BYTE;
            break;
        case -86:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = TWO_BYTES;
            break;
        case -85:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = THREE_BYTES;
            break;
        case -84:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case -83:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = FOUR_BYTES;
            spaceForObjectCount = NONE;
            break;
        case -82:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = ZERO_BYTES;
            break;
        case -81:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = ONE_BYTE;
            break;
        case -80:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = TWO_BYTES;
            break;
        case -79:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = THREE_BYTES;
            break;
        case -78:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = FOUR_BYTES;
            break;
        case -77:
            spaceForInteger = FOUR_BYTES;
            spaceForStringLength = NONE;
            spaceForObjectCount = NONE;
            break;
        default:
            throw new RuntimeException(
                "extractSpaceInfo: invalid header value: " + header);
        }
    }
}