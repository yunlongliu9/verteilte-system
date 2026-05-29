package vsue.communication;

import java.io.*;
import java.net.Socket;
import java.util.HexFormat;

public class VSObjectConnection
{
	private Socket connection;
	private final ObjectOutputStream out;
	private final ObjectInputStream in;
	
	public VSObjectConnection(Socket socket) throws IOException
	{
		this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		this.out.flush();
		this.connection = socket;
		this.in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
	}
	public Socket getSocket(){
		return connection;
	}
    
    private final byte[] javaHeader = { -84, -19, 0, 5 };
	
	
	public void sendObject(Serializable s) throws IOException
	{
		out.writeObject(s);
		out.reset();
		out.flush();
	}
	
	public Serializable receiveObject()
	throws
	    IOException,
	    ClassNotFoundException,
	    VSConnectionException,
	    VSConnectionEndOfFile
	{
		try {
			return (Serializable) in.readObject();
		} catch (EOFException e) {
			throw new VSConnectionEndOfFile();
		}
	}
}