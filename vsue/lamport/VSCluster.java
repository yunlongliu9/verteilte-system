package vsue.lamport;

import java.io.IOException;
import java.io.Serializable;


public interface VSCluster {

	/**
	 * Returns the ID of the local Lamport-protocol process.
	 *
	 * @return The ID of the local Lamport-protocol process.
	 */
	public int getProcessID();

	/**
	 * Returns the number of processes in the cluster.
	 *
	 * @return The size of the Lamport-protocol cluster
	 */
	public int getSize();

	/**
	 * Sends a message to a specific process in the cluster.
	 *
	 * @param message   The message to send
	 * @param processID The ID of the process to which the message should be sent
	 * @throws IOException if the message was not sent properly
	 */
	public void unicast(Serializable message, int processID) throws IOException;

	/**
	 * Sends a message to all processes in the cluster.
	 *
	 * @param message The message to send
	 * @throws IOException if the message was not sent properly
	 */
	public void multicast(Serializable message) throws IOException;

}
