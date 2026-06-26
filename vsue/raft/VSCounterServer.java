package vsue.raft;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import vsue.raft.VSCounterMessages.VSCounterReply;
import vsue.raft.VSCounterMessages.VSCounterRequest;
import vsue.raft.VSCounterMessages.VSLeaderIdNotification;

/**
 * Replicated implementation of the counter server
 */
public class VSCounterServer implements VSCounterService {

	/**
	 * 封装并持久化存储客户端请求的执行结果。
	 */
	private static class VSCounterResultWrapper implements Serializable {
		/**
		 * 这两个字段组合在一起构成了该请求的唯一指纹
		 */
		public final long clientId; // 标识是哪一个客户端发送的请求。
		public final long requestCounter;// 该客户端发送请求的序列号。
		/**
		 * 计数器逻辑真正执行后返回的数据（例如自增后的最新计数值）。
		 */
		public final VSCounterReply result;

		public VSCounterResultWrapper(long clientId, long requestCounter, VSCounterReply result) {
			this.clientId = clientId;
			this.requestCounter = requestCounter;
			this.result = result;
		}

		@Override
		public String toString() {
			return "VSResultWrapper{[" + clientId + "-" + requestCounter + "], result=" + result + '}';
		}
	}

	/**
	 * Map from the request id to the result wrapper. The future is used to pass
	 * the result to handleRequest
	 */
	private final Map<VSCounterRequest, CompletableFuture<VSCounterResultWrapper>> resultNotifier = new HashMap<>();
	/**
	 * Reply cache containing the latest reply for the client. Must only be
	 * modified from within applyRequest
	 */
	private final Map<Long, VSCounterResultWrapper> resultMap = new HashMap<>();
	private final VSRaftProtocol protocol;
	private final int myId;
	private final int replicaCount;

	/**
	 * Counter application state
	 */
	private int counter = 0;
	/**
	 * Term and index of last processed log entry / snapshot
	 */
	private int lastTerm = -1;
	private long lastIndex = 0;

	private boolean isLeader;
	private int currentLeaderId;

	/**
	 * Create a new instance of the counter service
	 *
	 * @param protocol     Raft protocol instance used to coordinate replicas
	 * @param replicaId    id of the current replica. addresses[replicaId] is the
	 *                     address of the registry for use by the current replica
	 * @param replicaCount total number of replicas
	 */
	public VSCounterServer(VSRaftProtocol protocol, int replicaId, int replicaCount) {
		this.protocol = protocol;
		this.myId = replicaId;
		this.replicaCount = replicaCount;

		isLeader = false;
		currentLeaderId = -1;
	}

	/**
	 * Log msg to stdout and prefix it with the current time
	 *
	 * @param msg log message
	 */
	private void log(String msg) {
		System.out.println(LocalTime.now().truncatedTo(ChronoUnit.MILLIS) + " app " + msg);
	}

	/**
	 * Initializes the protocol and the counter application. The protocol is
	 * started before exporting the counter server in a local registry, to
	 * ensure that requests can be processed immediately.
	 *
	 * @param registryPort port to use for the counter server registry
	 * @throws RemoteException thrown if registry create or application export fails
	 */
	public void init(int registryPort) throws RemoteException {
		// Initialize raft protocol before exporting the application
		protocol.init(this);

		// Export server
		UnicastRemoteObject.exportObject(this, 0);

		// Register server
		Registry registry = LocateRegistry.createRegistry(registryPort);
		try {
			registry.bind(VSCounterService.SERVICE_NAME, this);
		} catch (AlreadyBoundException e) {
			throw new RemoteException("Don't bind twice", e);
		}
	}

	/**
	 * Update role and leaderId. Must be called by the protocol whenever the
	 * role of this replica changes or when the leader replica changes.
	 *
	 * @param role     raft protocol role of the current replica
	 * @param leaderId id of the current leader or -1 if there is no leader
	 */
	public synchronized void status(VSRaftProtocol.VSRaftRole role, int leaderId) {
		if (role == null || leaderId <= -2 || leaderId >= replicaCount) {
			throw new IllegalArgumentException("Invalid leader id or role. Got: leaderId=" +
					leaderId + " role=" + role);
		} else if (role == VSRaftProtocol.VSRaftRole.LEADER && leaderId != myId) {
			throw new IllegalArgumentException("LeaderId must be that of the current replica" +
					" as this replica is the leader. Got: leaderId=" + leaderId + " role=" + role);
		} else if (role == VSRaftProtocol.VSRaftRole.CANDIDATE && leaderId != -1) {
			throw new IllegalArgumentException("LeaderId must be == -1 if this replica is a" +
					" candidate. Got: leaderId=" + leaderId + " role=" + role);
		} else if (role == VSRaftProtocol.VSRaftRole.FOLLOWER && leaderId == myId) {
			throw new IllegalArgumentException("This replica cannot be the leader and a follower" +
					" at the same time. Got: leaderId=" + leaderId + " role=" + role);
		}

		log("status update: role=" + role + " leaderId=" + leaderId);

		isLeader = VSRaftProtocol.VSRaftRole.LEADER.equals(role);
		currentLeaderId = leaderId;

		if (!isLeader) {
			// only the leader replica processes requests, thus cancel pending requests everywhere else
			for (CompletableFuture<VSCounterResultWrapper> future : resultNotifier.values()) {
				future.cancel(false);
			}
			resultNotifier.clear();
		}
	}

	// see VSCounterService.handleRequest for the documentation
	public VSCounterReply handleRequest(VSCounterRequest request) throws RemoteException, VSLeaderIdNotification {
		log("Got request from client " + request);

		VSCounterResultWrapper result;
		synchronized (this) {
			// check if there is already a stored result
			result = resultMap.get(request.clientId);
			if (result != null) {
				if (result.requestCounter < request.requestCounter) {
					// result is for an old request
					result = null;
				} else if (result.requestCounter > request.requestCounter) {
					// reject old request
					throw new RemoteException("Rejecting old request");
				}
			}
		}

		if (result == null) {
			// process request for which we have no reply yet
			CompletableFuture<VSCounterResultWrapper> replyFuture;
			synchronized (this) {
				// add future before submitting the request to ensure that it
				// is available when executing the request. Just reuse the
				// future if a request has already been queued for ordering.
				if (!resultNotifier.containsKey(request)) {
					resultNotifier.put(request, new CompletableFuture<>());
				}
				replyFuture = resultNotifier.get(request);
			}

			// no synchronized block here as the protocol callbacks could
			// deadlock otherwise
			boolean accepted = protocol.orderRequest(request);

			synchronized (this) {
				if (!accepted) {
					// cleanup unused future
					resultNotifier.remove(request, replyFuture);
					// the protocol rejected the request, probably because it is
					// no longer a leader. Tell the client to retry it's request.
					throw new VSLeaderIdNotification(currentLeaderId);
				}
			}

			try {
				result = replyFuture.get();
			} catch (CancellationException e) {
				// tell the client to retry it's request
				throw new VSLeaderIdNotification(-1);
			} catch (InterruptedException | ExecutionException e) {
				// remote execution of request was interrupted
				throw new RemoteException("Execution failed", e);
			}
		}

		return result.result;
	}

	/**
	 * Applies an ordered log entry from the raft protocol. The counter server
	 * sanity checks that no log entries were skipped. The contained request is
	 * only executed if no newer request from the same client has been executed.
	 * Finally the result is stored and a waiting handleRequest call gets notified.
	 *
	 * @param entry log entry with request to execute
	 */
	public synchronized void applyRequest(VSRaftLogEntry entry) {
		if (entry.request == null) {
			throw new IllegalArgumentException("Stub elements must not be passed to the application");
		}
		// sanity check to prevent skipping log entries
		if (lastTerm > entry.term || entry.index != lastIndex + 1) {
			throw new IllegalArgumentException("Expected a log entry for index " + (lastIndex + 1)
					+ " with at least term " + lastTerm + ", but got: index "
					+ entry.index + " term " + entry.term);
		}
		lastTerm = entry.term;
		lastIndex = entry.index;

		// unpack request from log entry
		VSCounterRequest request = (VSCounterRequest) entry.request;

		// check if request was already executed
		VSCounterResultWrapper lastResult = resultMap.get(request.clientId);
		if (lastResult != null && request.requestCounter <= lastResult.requestCounter) {
			return;
		}

		// process request and store result
		VSCounterReply reply = processRequest(request);
		VSCounterResultWrapper result = new VSCounterResultWrapper(request.clientId, request.requestCounter, reply);
		resultMap.put(request.clientId, result);

		// notify client waiting for a reply
		CompletableFuture<VSCounterResultWrapper> replyFuture = resultNotifier.remove(request);
		if (replyFuture != null) {
			replyFuture.complete(result);
		}
	}

	/**
	 * Executes the client request
	 *
	 * @param request client request
	 * @return result of request execution
	 */
	private VSCounterReply processRequest(@SuppressWarnings("unused") VSCounterRequest request) {
		counter++;
		return new VSCounterReply(counter);
	}


	/**
	 * Copy of the application state. Used for the raft snapshot mechanism
	 */
	private static class VSCounterSnapshot implements Serializable {
		// index/term for sanity checks
		public final long lastIncludedIndex;
		public final int lastIncludedTerm;
		// application data
		public final int counter;
		// result cache copy
		public final HashMap<Long, VSCounterResultWrapper> results;

		public VSCounterSnapshot(long lastIncludedIndex, int lastIncludedTerm, int counter, Map<Long, VSCounterResultWrapper> results) {
			this.lastIncludedIndex = lastIncludedIndex;
			this.lastIncludedTerm = lastIncludedTerm;
			this.counter = counter;
			this.results = new HashMap<>(results);
		}

		@Override
		public String toString() {
			return "VSCounterSnapshot{" +
					"lastIncludedIndex=" + lastIncludedIndex +
					", lastIncludedTerm=" + lastIncludedTerm +
					", counter=" + counter +
					'}';
		}
	}

	/**
	 * Applies the given snapshot to the application state. The applied snapshot
	 * must be at least as recent as the current application state. Rolling back
	 * to an older state is not permitted.
	 *
	 * @param snapshot Counter state snapshot to apply
	 */
	public synchronized void applySnapshot(Serializable snapshot) {
		VSCounterSnapshot data = ((VSCounterSnapshot) snapshot);
		if (data.lastIncludedIndex < lastIndex) {
			throw new IllegalArgumentException("Must not apply an old snapshot, snapshot idx="
					+ data.lastIncludedIndex + " lastIndex=" + lastIndex);
		}

		counter = data.counter;
		resultMap.clear();
		resultMap.putAll(data.results);
		lastTerm = data.lastIncludedTerm;
		lastIndex = data.lastIncludedIndex;
	}

	/**
	 * Creates a snapshot of the current application state. The snapshot can
	 * later on be applied on other replicas using applySnapshot.
	 *
	 * @return Snapshot of the current application state and result cache
	 */
	public synchronized Serializable createSnapshot() {
		return new VSCounterSnapshot(lastIndex, lastTerm, counter, resultMap);
	}
}
