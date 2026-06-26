package vsue.raft;

import java.rmi.Remote;
import java.rmi.RemoteException;

import vsue.raft.VSCounterMessages.VSCounterReply;
import vsue.raft.VSCounterMessages.VSCounterRequest;
import vsue.raft.VSCounterMessages.VSLeaderIdNotification;

/**
 * Remote interface for the counter application
 */
public interface VSCounterService extends Remote {

	String SERVICE_NAME = "CounterService";

	/**
	 * Submit a request to the counter service. The latter will then replicate
	 * the request using the VSRaftProtocol and return the execution result.
	 *
	 * @param request a VSCounterRequest
	 * @return the result as a VSCounterResult or redirect the client to a
	 * different leader using a VSLeaderIdNotification
	 * @throws RemoteException request execution failed
	 */
	VSCounterReply handleRequest(VSCounterRequest request) throws RemoteException, VSLeaderIdNotification;

}
