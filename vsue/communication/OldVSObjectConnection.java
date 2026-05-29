package vsue.communication;

import java.io.*;
import java.net.Socket;
import java.util.HexFormat;

public class OldVSObjectConnection
{
	private final VSConnection connection;
	// Auf true setzen für Analyseoutput in sendObject().
	public boolean analyse;
	
	public OldVSObjectConnection(Socket socket) throws IOException
	{
		connection = new VSConnection(socket);
		analyse = false;
	}
	
	private String hex(byte[] a)
	{
	    HexFormat b = HexFormat.ofDelimiter(" ");
	    return b.formatHex(a);
    }
    
    private String string(byte[] a)
    {
        StringBuilder b = new StringBuilder();
	    for (byte c : a) {
	        // Nur printable ASCII, sonst kommen da sehr wilde Dinge raus
	        if (c >= 33 && c <= 176) {
	        	b.append(" ");
	            b.append((char) c);
            } else {
            	b.append("  ");
            }
            b.append(" ");
        }
        return b.toString();
    }
	
	public void sendObject(Serializable s) throws IOException
	{
	    // Der Puffer im ByteArrayOutputStream wird bei Bedarf automatisch vergrößert
		ByteArrayOutputStream storage = new ByteArrayOutputStream(1024);
		ObjectOutputStream converter = new ObjectOutputStream(storage);
		converter.writeObject(s);
		byte[] raw = storage.toByteArray();
		if (analyse) {
		    System.out.println("----------- Analyse ----------");
		    System.out.println(hex(raw));
		    System.out.println(string(raw));
			System.out.println("Size: " + raw.length + " bytes");
		    System.out.println("------------------------------");
	    }
		connection.sendChunk(raw);
	}
	
	public Serializable receiveObject()
	throws
	    IOException,
	    ClassNotFoundException,
	    VSConnectionException,
	    VSConnectionEndOfFile
	{
		byte[] raw = connection.receiveChunk();
		ByteArrayInputStream layer1 = new ByteArrayInputStream(raw);
		ObjectInputStream layer2 = new ObjectInputStream(layer1);
		return (Serializable) layer2.readObject();
	}
}