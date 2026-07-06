package vsue.lamport;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


public class VSClusterImpl extends Thread implements VSCluster {

	public VSClusterImpl(int id, InetSocketAddress[] addresses) {
		this.id = id;
		this.addresses = addresses;
		this.protocol = null;
		this.events = new LinkedBlockingQueue<VSLamportEvent>();
		this.connections = new VSClusterConnection[addresses.length];
		this.connected = new AtomicBoolean(false);
	}


	// ###########
	// # CLUSTER #
	// ###########

	private final int id;
	private final InetSocketAddress[] addresses;


	@Override
	public int getProcessID() {
		return id;
	}

	@Override
	public int getSize() {
		return addresses.length;
	}

	@Override
	public void unicast(Serializable message, int processID) throws IOException {
		if (processID == id) {
			// Create a deep copy of the message and deliver the message locally
			message = copy(message);
			deliver(message);
		} else {
			// Send message remotely
			if ((processID < 0) || (connections.length <= processID))
				throw new IOException("Bad process ID: " + processID);
			connections[processID].send(message);
		}
	}

	@Override
	public void multicast(Serializable message) throws IOException {
		// Create a deep copy of the message (especially for local delivery)
		message = copy(message);

		// Deliver message locally
		deliver(message);

		// Send message to remote processes
		for (int i = 0; i < connections.length; i++) {
			if (i == id) continue;
			connections[i].send(message);
		}
	}

	private static Serializable copy(Serializable message) throws IOException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(message);
			oos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			return (Serializable) ois.readObject();
		} catch (ClassNotFoundException cnfe) {
			throw new IOException("Serialization error: ", cnfe);
		}
	}


	// ###########
	// # CONNECT #
	// ###########

	private final VSClusterConnection[] connections;
	private final AtomicBoolean connected;


	public boolean connected() {
		return connected.get();
	}

	public void connect() throws Exception {
		// Create and start acceptor
		VSClusterAcceptor acceptor = new VSClusterAcceptor();
		acceptor.start();

		// Establish connections to processes with higher IDs
		for (int i = (id + 1); i < addresses.length; i++) {
			System.out.print("Connect to " + addresses[i] + "...");
			System.out.flush();
			while (connections[i] == null) {
				try {
					// Establish connection
					Socket socket = new Socket(addresses[i].getHostString(), addresses[i].getPort());

					// Send local process ID
					OutputStream out = socket.getOutputStream();
					out.write(id);
					out.flush();

					// Store connection
					connections[i] = new VSClusterConnection(socket);
					connections[i].start();
				} catch (IOException e) {
					System.out.print(".");
					System.out.flush();
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException ie) {
						return;
					}
				}
			}
			System.out.println("done.");
		}

		// Wait for acceptor to finish
		acceptor.join();

		// Set flag
		connected.set(true);
	}


	private class VSClusterAcceptor extends Thread {

		@Override
		public void run() {
			// Accept connections from processes with lower IDs
			try (ServerSocket server = new ServerSocket(addresses[id].getPort())) {
				for (int i = 0; i < id; i++) {
					// Accept connection
					Socket socket = server.accept();

					// Receive remote process ID
					int remote = socket.getInputStream().read();
					if ((remote < 0) || (id <= remote)) {
						System.err.println("Unexpected remote-process ID: " + remote);
						i--;
						continue;
					}

					// Store connection
					connections[remote] = new VSClusterConnection(socket);
					connections[remote].start();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	}


	// ##############
	// # CONNECTION #
	// ##############

	private volatile int delayInMs;

	void setTestReceiveDelay(int delayInMs) {
		System.out.println("Setting receive delay: " + delayInMs);
		this.delayInMs = delayInMs;
	}

	private class VSClusterConnection extends Thread {

		private final Socket socket;
		private final ObjectOutputStream out;
		private final ObjectInputStream in;


		public VSClusterConnection(Socket socket) throws IOException {
			// Create streams
			this.socket = socket;
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.in = new ObjectInputStream(socket.getInputStream());

			// Disable Nagle's algorithm
			socket.setTcpNoDelay(true);
		}

		@Override
		public void run() {
			try {
				// Receive messages
				while (!isInterrupted()) {
					Serializable message = (Serializable) in.readObject();
					int delayInMsCopy = delayInMs;
					if (delayInMsCopy > 0) Thread.sleep(delayInMsCopy);
					deliver(message);
				}
			} catch (EOFException | SocketException ignore) {
				// ignore
			} catch (IOException | ClassNotFoundException | InterruptedException e) {
				e.printStackTrace();
			}

			// Close connection
			close();

			// Update flag
			connected.set(false);
			System.out.println("Disconnected from " + socket.getRemoteSocketAddress());
		}

		public void send(Serializable message) throws IOException {
			out.writeObject(message);
			out.flush();
			out.reset();
		}

		public void close() {
			try {
				// Close connection
				socket.close();
			} catch(IOException ignore) {}
		}

	}


	// ##############
	// # EVENT LOOP #
	// ##############

	private volatile int eventDelayInMs;

	void setTestEventDelay(int delayInMs) {
		System.out.println("Setting event delay: " + delayInMs);
		this.eventDelayInMs = delayInMs;
	}

	private final BlockingQueue<VSLamportEvent> events;

	private VSLamportProtocol protocol;

	public void startWithProtocol(VSLamportProtocol protocol) {
		// Set protocol and start delivering messages
		if (protocol == null) {
			throw new IllegalArgumentException("protocol must not be null");
		} else if (this.protocol != null) {
			throw new IllegalStateException("The protocol may only be set once");
		}

		this.protocol = protocol;
		start();
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				VSLamportEvent event = events.take();
				int eventDelayInMsCopy = eventDelayInMs;
				if (eventDelayInMsCopy > 0) Thread.sleep(eventDelayInMsCopy);
				protocol.event(event);
			}
		} catch (InterruptedException ie) {
			// Do nothing
		}
	}

	private void deliver(Serializable message) {
		VSLamportEvent event = new VSLamportEvent(VSLamportEventType.MESSAGE, message);
		events.add(event);
	}

	public void shutdown() throws InterruptedException {
		// Stop delivering messages
		interrupt();
		join();

		// Terminate connections
		for (VSClusterConnection connection : connections) {
			if (connection == null) continue;
			connection.close();
			connection.join();
		}
	}

}
