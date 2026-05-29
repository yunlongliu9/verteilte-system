package vsue.communication;

import java.net.Socket;
import java.io.IOException;
import java.io.Serializable;

public class VSBuggyObjectConnection extends VSObjectConnection {
    private final Socket socket;

    public VSBuggyObjectConnection(Socket socket) throws java.io.IOException {
        this.socket = socket;
        socket.setSoTimeout(3000); // No timeout by default
        super(socket);
    }


	public void sendObject(Serializable s) throws IOException
	{
        // Randomly discard the object with a 5% chance
        if (Math.random() < 0.05) {
            try {
                socket.close(); // Simulate a socket timeout by closing the socket
            } catch(IOException ioe) {
                throw new IOException("Simulated socket timeout", ioe);
            }
        }
        // Otherwise, send the object normally
        super.sendObject(s);
    }


	public Serializable receiveObject() throws IOException, ClassNotFoundException, VSConnectionException, VSConnectionEndOfFile
    {
        // Randomly discard the object with a 5% chance
        if (Math.random() < 0.05) {
            socket.close(); // Simulate a socket timeout by closing the socket
        }
        return super.receiveObject();
    }

}