package vsue.lamport;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public final class VSLamportTest {

	private static final long DURATION = 60000L;
	private static final long EARLY_EXIT_DURATION = 5000L;
	private static final long SHUTDOWN_DELAY = 3000L;

	private final VSClusterImpl cluster;


	public VSLamportTest(int id, InetSocketAddress[] addresses, String log) throws IOException {
		this.cluster = new VSClusterImpl(id, addresses);
		this.lock = null;
		this.logger = new PrintWriter(new FileWriter(log));
		this.end = -1L;
	}


	// ##################
	// # INITIALIZATION #
	// ##################

	private VSLamportLock lock;


	public void init() throws Exception {
		// Connect cluster
		cluster.connect();

		// Create and initialize protocol
		VSLamportProtocol protocol = new VSLamportProtocol(cluster);
		protocol.init();

		// Create lock
		lock = new VSLamportLock(protocol);

		// Start delivering messages to protocol
		cluster.startWithProtocol(protocol);
	}


	// ###########
	// # LOGGING #
	// ###########

	private final PrintWriter logger;


	private void print(String message) {
		System.out.print(message);
		System.out.flush();
	}

	private void log(String message) {
		print(message);
		logger.print(message);
	}


	// #############
	// # EXECUTION #
	// #############

	private long end;


	public void execute(VSLamportTestCase test) throws Exception {
		// Initialize test
		test.init();

		// Set deadline and start test
		end = System.currentTimeMillis() + DURATION;
		test.start();

		// Wait until the test is complete or a connection breaks
		while (active() && cluster.connected()) Thread.sleep(1000L);

		// Wait a little bit longer
		Thread.sleep(SHUTDOWN_DELAY);

		// Shutdown
		logger.close();
		test.interrupt();
		cluster.shutdown();
	}

	private boolean active() {
		return (System.currentTimeMillis() < end);
	}

	private boolean nearlyFinished() {
		return active() && (System.currentTimeMillis() > end - EARLY_EXIT_DURATION);
	}

	private void pause() throws InterruptedException {
		Thread.sleep((long) (Math.random() * 2));
	}


	private abstract class VSLamportTestCase extends Thread {

		protected long lockTimeout = -1;
		protected long activeLockTimeout = -1;

		public void init() throws Exception {
			// Override in sub classes if necessary
		}

		protected abstract void work(int count) throws Exception;

		public void setLockTimeout(long timeout) {
			lockTimeout = timeout;
			activeLockTimeout = lockTimeout;
		}

		protected boolean isUsingTryLock() {
			return lockTimeout >= 0;
		}

		protected boolean lock() throws InterruptedException {
			if (!isUsingTryLock()) {
				lock.lock();
				return true;
			} else {
				if (lock.tryLock(activeLockTimeout, TimeUnit.MILLISECONDS)) {
					activeLockTimeout = Math.max(lockTimeout, activeLockTimeout - 1);
					return true;
				} else {
					activeLockTimeout++;
					return false;
				}
			}
		}

		protected void unlock() {
			lock.unlock();
		}

		@Override
		public void run() {
			try {
				for (int iteration = 0; (active() && !isInterrupted()); iteration++) work(iteration);
				log("\n" + getName() + ": DONE.\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}


	// ##########
	// # SIMPLE #
	// ##########

	private class VSLamportSimpleTestCase extends VSLamportTestCase {

		public VSLamportSimpleTestCase() {
			setName("SIMPLE");
		}


		@Override
		protected void work(int iteration) throws InterruptedException {
			// simulate process 0 finishing early
			if (cluster.getProcessID() == 0 && nearlyFinished()) {
				try {
					Thread.sleep(EARLY_EXIT_DURATION + SHUTDOWN_DELAY + 1000L);
				} catch (InterruptedException e) {
					// ignore
				}
				return;
			}

			// Acquire lock
			log("?");

			while (!lock()) {
				log("x");
				pause();
			}

			// Simulate work in critical section
			pause();
			log(".");
			pause();

			// Release lock
			unlock();
		}

	}


	// #########
	// # FANCY #
	// #########

	private static final String FANCY_NAME = "FANCY";


	private class VSLamportFancyTestCase extends VSLamportTestCase {

		private final InetSocketAddress[] addresses;
		private final int basePort;
		private final VSLamportTestFancyService[] services;


		public VSLamportFancyTestCase(InetSocketAddress[] addresses, int basePort) {
			this.addresses = addresses;
			this.basePort = basePort;
			this.services = new VSLamportTestFancyService[addresses.length];
			setName(FANCY_NAME);
		}


		@Override
		public void init() throws Exception {
			// Export local service
			VSLamportTestFancyService service = new VSLamportTestFancyServiceImpl();
			Remote stub = UnicastRemoteObject.exportObject(service, 0);
			Registry registry = LocateRegistry.createRegistry(basePort + cluster.getProcessID());
			registry.bind(FANCY_NAME, stub);

			// Get stubs
			for (int i = 0; i < services.length; i++) {
				print("Connect to " + addresses[i].getHostString() + "...");
				while (true) {
					try {
						// Contact remote registry
						Registry remote = LocateRegistry.getRegistry(addresses[i].getHostString(), basePort + i);
						services[i] = (VSLamportTestFancyService) remote.lookup(FANCY_NAME);
						print("done.\n");
						break;
					} catch (RemoteException e) {
						// Wait some time and then try again
						print(".");
						Thread.sleep(1000L);
					}
				}
			}
		}

		@Override
		protected void work(int iteration) throws Exception {
			// Acquire lock
			if (iteration == 0) {
				lock.lock();
			} else if (isUsingTryLock() && iteration == 150) {
				// interrupt tryLock at iteraton 150
				currentThread().interrupt();
				try {
					while (lock()) {
						unlock();
						print("I");
						currentThread().interrupt();
					}
					throw new AssertionError("Expected an InterruptedException. Check your handling of InterruptedExceptions");
				} catch (InterruptedException ignored) {
					return;
				}
			} else {
				while (!lock()) {
					log("Locking timeout\n");
					pause();
				}
			}

			// Perform work
			if (iteration % 100 == 0) {
				// Determine the sum of all local values
				int sum = 0;
				for (VSLamportTestFancyService service : services) sum += service.get();
				log("Sum is " + sum + "\n");
			} else {
				// Transfer credits
				int other = (int) (Math.random() * addresses.length);
				int credits = services[other].get();
				int change = (int) (Math.random() * credits);
				log(String.format("Stealing %4d credits from %s\n", change, addresses[other]));
				services[other].set(credits - change);
				services[cluster.getProcessID()].set(services[cluster.getProcessID()].get() + change);
			}

			// Release lock
			unlock();
			pause();
		}

	}


	public static interface VSLamportTestFancyService extends Remote {

		public int get() throws RemoteException;

		public void set(int value) throws RemoteException;

	}


	public static class VSLamportTestFancyServiceImpl implements VSLamportTestFancyService {

		public final AtomicInteger credits;


		public VSLamportTestFancyServiceImpl() {
			this.credits = new AtomicInteger((int) (Math.random() * 1000));
		}


		@Override
		public int get() {
			return credits.get();
		}

		@Override
		public void set(int value) {
			credits.set(value);
		}

	}


	// #########
	// # DEBUG #
	// #########

	private static final String DEBUG_NAME = "DEBUG";


	private class VSLamportDebugTestCase extends VSLamportTestCase {

		private final InetSocketAddress[] addresses;
		private final int basePort;
		private VSLamportTestDebugService service;


		public VSLamportDebugTestCase(InetSocketAddress[] addresses, int basePort) {
			this.addresses = addresses;
			this.basePort = basePort;
			setName(DEBUG_NAME);
		}


		@Override
		public void init() throws Exception {
			// Export local service
			if (cluster.getProcessID() == 0) {
				VSLamportTestDebugService service = new VSLamportTestDebugServiceImpl(cluster.getSize());
				Remote stub = UnicastRemoteObject.exportObject(service, 0);
				LocateRegistry.createRegistry(basePort + cluster.getProcessID());
				LocateRegistry.getRegistry(basePort + cluster.getProcessID()).bind(DEBUG_NAME, stub);
			}

			// Get stubs
			print("Connect to " + addresses[0].getHostString() + "...");
			while (true) {
				try {
					// Contact remote registry
					Registry remote = LocateRegistry.getRegistry(addresses[0].getHostString(), basePort);
					service = (VSLamportTestDebugService) remote.lookup(DEBUG_NAME);
					print("done.\n");
					break;
				} catch (RemoteException | NotBoundException e) {
					// Wait some time and then try again
					print(".");
					Thread.sleep(1000L);
				}
			}
		}

		@Override
		protected void work(int iteration) throws Exception {
			service.sync();
			int processID = cluster.getProcessID();
			if (iteration < 50) {
				if (iteration == 0) {
					print("\n> The test will print 'SUCCESS' once it completes <\n\n");
					print("Checking that a single process can take the lock. No output on all replicas means that something is broken\n");
					cluster.setTestReceiveDelay(10);
					cluster.setTestEventDelay(0);
				}
				if (processID == 0) {
					lock.lock();
					print(".");
					lock.unlock();
				} else {
					print(".");
				}
			} else if (iteration < 100) {
				if (iteration == 50) {
					print("\nChecking request ordering between multiple processes and/or too frequent entering of the critical section\n");
					cluster.setTestReceiveDelay(10);
					cluster.setTestEventDelay(0);
				}
				lockSleepUnlock();
			} else if (iteration < 150) {
				if (iteration == 100) {
					print("\nChecking proper matching of lock call and request message\n");
					cluster.setTestReceiveDelay(0);
					cluster.setTestEventDelay(5);
				}
				if (processID == 0) {
					int duration = Math.min(100, iteration - 50);
					print("Sleeping for " + duration + " ms\n");
					Thread.sleep(duration);
				}
				lockSleepUnlock();
				lockSleepUnlock();
			} else if (iteration < 200 && isUsingTryLock()) {
				if (iteration == 150) {
					print("\nChecking failed tryLock calls\n");
					cluster.setTestReceiveDelay(10);
					cluster.setTestEventDelay(0);
				}
				if (processID == 0) {
					lockSleepUnlock();
				} else if (processID == 1) {
					Thread.sleep(25);
					if (lock.tryLock(1, TimeUnit.MILLISECONDS)) {
						// process 2 does not send requests on its own. Thus the other process must wait for
						// the ack of process 2
						throw new AssertionError("This lock request should never succeed");
					}
					lockSleepUnlock();
				} else {
					print(".");
				}
			} else if (iteration < 220 && isUsingTryLock()) {
				if (iteration == 200) {
					print("\nChecking interrupted tryLock calls\n");
					lockInterrupt();
				} else {
					lockSleepUnlock();
				}
				print(".");
			} else {
				print("\nSUCCESS\n");
				Thread.sleep(1000);
				// Hard shutdown
				System.exit(0);
			}
		}

		private void lockSleepUnlock() throws RemoteException, InterruptedException {
			lock.lock();
			lockCheck();
			lock.unlock();
		}

		private void lockInterrupt() {
			// interrupt tryLock
			currentThread().interrupt();
			try {
				while (lock()) {
					unlock();
					print("I");
					currentThread().interrupt();
				}
				throw new AssertionError("Expected an InterruptedException. Check your handling of InterruptedExceptions");
			} catch (InterruptedException ignored) {}
		}

		private void lockCheck() throws RemoteException, InterruptedException {
			if (!service.toggle(cluster.getProcessID())) {
				throw new AssertionError("The lock is already owned by another process!");
			}
			print(".");
			Thread.sleep(50);
			if (!service.toggle(-1)) {
				throw new AssertionError("Another process has unlocked the service in the meantime O.o!");
			}
		}

	}


	public interface VSLamportTestDebugService extends Remote {
		void sync() throws RemoteException;

		boolean toggle(int value) throws RemoteException;
	}


	public static class VSLamportTestDebugServiceImpl implements VSLamportTestDebugService {

		private final CyclicBarrier barrier;
		private int lockOwner;

		public VSLamportTestDebugServiceImpl(int replicas) {
			// Central lock, only for testing!
			barrier = new CyclicBarrier(replicas);
			lockOwner = -1;
		}

		@Override
		public void sync() {
			try {
				barrier.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new InternalError(e);
			}
		}

		@Override
		public synchronized boolean toggle(int value) {
			boolean ok = (lockOwner >= 0 && value < 0) || (lockOwner < 0 && value >= 0);
			lockOwner = value;
			return ok;
		}

	}

	// ########
	// # MAIN #
	// ########

	public static void main(String[] args) throws Exception {
		// Check arguments
		if (args.length < 2) {
			System.err.println("usage: " + VSLamportTest.class.getSimpleName() + " <local-id> <base-port> [fancy]");
			System.exit(1);
		}

		// Parse arguments
		int id = Integer.parseInt(args[0]);
		int basePort = Integer.parseInt(args[1]);
		String type = (args.length > 2) ? args[2] : "simple";

		// Load addresses
		String configPath = System.getProperty("configs_path");
		Path path = Paths.get(configPath, "my_hosts");
		List<InetSocketAddress> addrs = new LinkedList<InetSocketAddress>();
		int i = 0;
		for (String line : Files.readAllLines(path)) {
			try {
				// Parse address
				String[] parts = line.split(":");
				InetSocketAddress address = new InetSocketAddress(parts[0], basePort + i);
				addrs.add(address);
				i++;
			} catch (Exception e) {
				System.err.println("Ignore line \"" + line + "\" due to " + e);
			}
		}
		InetSocketAddress[] addresses = new InetSocketAddress[addrs.size()];
		addrs.toArray(addresses);

		// Create and initialize test framework
		VSLamportTest framework = new VSLamportTest(id, addresses, configPath + "/vslocktest." + type + "." + id + ".log");
		framework.init();

		// Create and execute test case
		VSLamportTestCase test = null;
		switch (type) {
		case "simple":
			test = framework.new VSLamportSimpleTestCase();
			break;
		case "fancy":
			test = framework.new VSLamportFancyTestCase(addresses, basePort + addresses.length);
			break;
		case "simple-try":
			test = framework.new VSLamportSimpleTestCase();
			test.setLockTimeout(5);
			break;
		case "fancy-try":
			test = framework.new VSLamportFancyTestCase(addresses, basePort + addresses.length);
			test.setLockTimeout(5);
			break;
		case "debug":
			test = framework.new VSLamportDebugTestCase(addresses, basePort + addresses.length);
			break;
		case "debug-try":
			test = framework.new VSLamportDebugTestCase(addresses, basePort + addresses.length);
			test.setLockTimeout(5);
			break;
		default:
			throw new IllegalArgumentException("Unknown test case: " + type);
		}
		framework.execute(test);

		// Hard shutdown
		System.exit(0);
	}

}
