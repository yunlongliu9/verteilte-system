package vsue.raft;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

import vsue.raft.VSCounterMessages.VSCounterReply;
import vsue.raft.VSCounterMessages.VSCounterRequest;
import vsue.raft.VSCounterMessages.VSLeaderIdNotification;

/**
 * Client application to access the replicated counter service. Provides an
 * increment and a basic benchmark command.
 */
public class VSCounterClient extends VSShell {

	/**
	 * number of request retries
	 */
	private static final int RETRIES = 10;

	/**
	 * Addresses of counter application replicas
	 */
	private final InetSocketAddress[] replicas;
	/**
	 * Stub for counter service of last used replica
	 */
	private VSCounterService contactReplica;

	/**
	 * Hopefully unique client Id
	 */
	private final long clientId = new Random().nextLong();
	/**
	 * Match request and reply based on its counter
	 */
	private long requestCounter = 0;

	/**
	 * Initialize client with list of application replicas
	 *
	 * @param replicas Addresses of counter application replicas
	 */
	public VSCounterClient(InetSocketAddress[] replicas) {
		this.replicas = replicas;
	}

	/**
	 * Increase the counter at the server by one
	 *
	 * @return new counter value
	 * @throws RemoteException failed to replicate and / or execute the command
	 */
	public int increment() throws RemoteException {
		VSCounterReply result = invoke(new VSCounterRequest(clientId, nextRequestId()));
		return result.counterValue;
	}

	/**
	 * Call the increase method <code>times</code> times.
	 *
	 * @param times number of increase calls
	 * @return new counter value
	 * @throws RemoteException          failed to replicate and / or execute a command
	 * @throws IllegalArgumentException number of repetitions is less than 1
	 */
	public int bench(int times) throws RemoteException {
		if (times < 1) {
			throw new IllegalArgumentException("Number of repetitions must be positive");
		}

		int newValue = -1;
		for (int i = 0; i < times; i++) {
			newValue = increment();
		}
		return newValue;
	}

	/**
	 * Returns id to use for the next client request
	 *
	 * @return request counter
	 */
	private long nextRequestId() {
		return requestCounter++;
	}

	/**
	 * Send the request to the counter service and return its reply. The request
	 * is first sent to the last used replica. In case no reply is received, the
	 * client randomly selects another replica and repeats its request. If the
	 * contacted replica is not the leader replica, then it returns the id of
	 * the leader replica if available. The client then switches to the leader
	 * replica. Otherwise the client waits a short time before retrying the
	 * request. A request is retried up to <code>RETRIES</code> times.
	 *
	 * @param request command to execute at the replicated server
	 * @return result of executing the request
	 * @throws RemoteException signals that no reply was received for the initial
	 *                         request and its retries
	 */
	private VSCounterReply invoke(VSCounterRequest request) throws RemoteException {
		int nextLeaderId = -1; // start by picking a random leader
		for (int i = 0; i < RETRIES; i++) {
			// Send request
			try {
				if (contactReplica == null) switchLeader(nextLeaderId);
				return contactReplica.handleRequest(request);
			} catch (VSLeaderIdNotification lin) {
				nextLeaderId = lin.leaderId;
				if (lin.leaderId < 0) {
					// we've contacted a follower / candidate
					System.out.println("Waiting for leader election");
					// no leader at the moment, wait a bit before retrying
					// to give the replicas a chance to pick a new leader
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						System.out.println("Nested error: " + e);
					}
				} else {
					// trigger leader switch
					contactReplica = null;
				}
			} catch (RemoteException re) {
				System.out.println("Error: " + re);
				contactReplica = null;
				// try a random replica next
				nextLeaderId = -1;
				// connection / replica problems, wait a bit before retrying
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("Nested error: " + e);
				}
			}
		}
		throw new RemoteException("Failed to contact replicas. Did you set the leader by calling status()?");
	}

	/**
	 * 访问了错误的leader, 切换到正确的leader访问[给了leaderid]
	 * Retrieve a counter application stub for the leader with the given id.
	 * If the id is negative, then randomly select a replica.
	 *
	 * @param leaderId id of the replica to use as contact replica
	 * @throws RemoteException retrieving the counter application stub failed
	 */
	private void switchLeader(int leaderId) throws RemoteException {
		// Determine index of the next replica to contact
		if (leaderId < 0) {
			leaderId = (int) (Math.random() * replicas.length);
		}
		InetSocketAddress leaderAddress = replicas[leaderId];
		System.out.print("Switch contact replica to " + leaderId + "@" + leaderAddress + ": ");
		System.out.flush();

		// Perform lookup
		Registry registry = LocateRegistry.getRegistry(leaderAddress.getHostString(), leaderAddress.getPort());
		try {
			contactReplica = (VSCounterService) registry.lookup(VSCounterService.SERVICE_NAME);
		} catch (NotBoundException e) {
			throw new RemoteException("Broken registry", e);
		}
		if (contactReplica == null) throw new RemoteException("Registry entry is null");
		System.out.println("Success");
	}

	protected boolean processCommand(String[] args) throws RemoteException {
		switch (args[0]) {
		case "help":
		case "h":
			System.out.println("The following commands are available:\n"
					+ "  help\n"
					+ "  inc\n"
					+ "  bench <times>\n"
					+ "  quit"
			);
			break;
		case "inc":
		case "i": {
			int newValue = increment();
			System.out.println("New counter value: " + newValue);
			break;
		}
		case "bench": {
			if (args.length < 2) throw new IllegalArgumentException("Usage: bench <times>");
			int newValue = bench(Integer.parseInt(args[1]));
			System.out.println("New counter value: " + newValue);
			break;
		}
		case "exit":
		case "quit":
		case "x":
		case "q":
			return false;
		default:
			throw new IllegalArgumentException("Unknown command: " + args[0] + "\nUse \"help\" to list available commands");
		}
		return true;
	}


	// ########
	// # MAIN #
	// ########

	public static void main(String[] args) throws IOException {
		// Check arguments
		if (args.length < 1) {
			System.err.println("usage: java " + VSCounterClient.class.getName() + " <replica-address-config>");
			System.err.println("  replica-config-file must contain lines of the form" +
					" \"replicaX=hostname:port\" (without quotes), where X is the replica id");
			System.exit(1);
		}

		InetSocketAddress[] addresses = VSCounterReplica.parseAddresses(args[0]);
		addresses = VSCounterReplica.deriveClientAddresses(addresses);
		System.out.println("Loaded " + addresses.length + " addresses:");
		for (int i = 0; i < addresses.length; i++) {
			System.out.println("    " + i + " " + addresses[i]);
		}

		// Create and execute client
		VSCounterClient client = new VSCounterClient(addresses);
		client.shell();
	}
}
