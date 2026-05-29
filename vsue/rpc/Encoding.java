package vsue.rpc;

import java.io.*;

public class Encoding
{
    public static final byte ZERO_BYTES = 0;
    public static final byte ONE_BYTE = 1;
    public static final byte TWO_BYTES = 2;
    public static final byte THREE_BYTES = 3;
    public static final byte FOUR_BYTES = 4;

    public static byte requiredSpace(int value)
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

    public static void write(ObjectOutput out, int value, byte size) throws IOException
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

    public static int read(ObjectInput in, byte size) throws IOException
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
}