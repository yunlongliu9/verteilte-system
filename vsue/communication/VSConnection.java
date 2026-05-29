package vsue.communication;

import java.io.*;
import java.net.Socket;

public class VSConnection
{
    private Socket socket;
    private final OutputStream drain;
    private final InputStream source;

    public VSConnection(Socket s) throws IOException
    {
        drain = s.getOutputStream();
        source = s.getInputStream();
        socket = s;
    }

    public Socket getSocket(){
        return socket;
    }

    private byte[] varInt(int x)
    {
        // Verschiebe den Wertebereich um eins.
        // Da wir nie 0 bekommen können, wäre der Wert sonst verschwendet.
        x = x - 1;
        byte[] bytes = new byte[5];
        bytes[0] |= x & 127; // Nimm nur die untersten sieben Bits
        int i = 1;
        while (i < 5 && x > 127) {
            bytes[i - 1] |= 128; // Setze höchstes Bit auf 1
            x >>= 7;
            bytes[i] |= x & 127;
            i++;
        }
        if (i < 5) {
            byte[] tmp = bytes;
            bytes = new byte[i];
            System.arraycopy(tmp, 0, bytes, 0, i);
        }
        return bytes;
    }

    public void sendChunk(byte[] chunk) throws IOException
    {
        byte[] header = varInt(chunk.length);
        byte[] message = new byte[header.length + chunk.length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(chunk, 0, message, header.length, chunk.length);
        drain.write(message);
        drain.flush();
    }

    // Reads and decodes the VarInt from the InputStream
    private int getLength()
    throws IOException, VSConnectionException, VSConnectionEndOfFile
    {
        int length = 0;
        for (int i = 0; i < 5; i++) {
            int result = source.read();
            if (result == -1) {
                if (i == 0) {
                    throw new VSConnectionEndOfFile();
                } else {
                    throw new VSConnectionException("VSConnection.getLength(): unexpected end of stream");
                }
            }
            length |= (result & 127) << (7 * i);
            // VarInt zuende
            if ((result & 128) == 0) {
                break;
            }
        }
        return length + 1;
    }

    public byte[] receiveChunk()
    throws VSConnectionException, VSConnectionEndOfFile, IOException
    {
        int length = getLength();
        byte[] chunk = new byte[length];
        int offset = 0;
        do {
            int bytesRead = source.read(chunk, offset, length - offset);
            // Die maximale Zahl von bytesRead scheint 131072 zu sein (Test mit
            // Riesentext, der wird auf mehrere Male gelesen).
            // Das sind 2 hoch 17 Bytes bzw. 128 KiB.
            if (bytesRead == -1) {
                throw new VSConnectionException("VSConnection.receiveChunk(): unexpected end of stream");
            } else {
                offset += bytesRead;
            }
        } while (offset < length);
        return chunk;
    }
}
