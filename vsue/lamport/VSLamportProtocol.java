package vsue.lamport;

public class VSLamportProtocol {

	public VSLamportProtocol(VSCluster cluster) {
		/*
		 * TODO: Implement constructor
		 */
	}

	public void init() {
		/*
		 * TODO: Implement initialization code (if necessary)
		 */
	}

	// The lock() and unlock() methods from VSLamportLock are supposed to be uninterruptible.
	// This means that the event method is also not allowed to use interruptible methods
	// (i.e. methods that throw an InterruptedException).
	// It is highly discouraged to use try-catch-blocks to catch an InterruptedException here
	// as this might lead to incorrect behaviour of the lock and tests!
	public void event(VSLamportEvent event) {
		/*
		 * TODO: Implement event handling / protocol logic
		 */
	}

}
