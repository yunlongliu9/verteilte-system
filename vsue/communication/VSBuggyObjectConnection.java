package vsue.communication;

import java.net.Socket;
import java.io.IOException;
import java.io.Serializable;

public class VSBuggyObjectConnection extends VSObjectConnection {
    private final Socket socket;

    public static double dropProbability = 0.05;
    public static double delayProbability = 0.00;
    public static int maxDelayMs = 1000;

    public static int messagesSent = 0;
    public static int messagesDropped = 0;

    public VSBuggyObjectConnection(Socket socket) throws java.io.IOException {
        super(socket);
        this.socket = socket;
    }

    private void simulateDelay() {
        if (Math.random() < delayProbability) {
            try {
                Thread.sleep((long) (Math.random() * maxDelayMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

	public void sendObject(Serializable s) throws IOException
	{
        simulateDelay();
        
        // Randomly discard the object with a given chance
        if (Math.random() < dropProbability) {
            messagesDropped++;
            socket.close(); // Just close the socket, the stream operations will naturally fail
        } else {
            messagesSent++;
        }
        // Send the object
        super.sendObject(s);
    }


	public Serializable receiveObject() throws IOException, ClassNotFoundException, VSConnectionException, VSConnectionEndOfFile
    {
        return super.receiveObject();
    }

}