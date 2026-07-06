package vsue.lamport;

import java.util.concurrent.TimeUnit;


public class VSLamportLock {

	public VSLamportLock(VSLamportProtocol protocol) {
		/*
		 * TODO: Implement constructor
		 */
	}


	public void lock() {
		/*
		 * TODO: Block until having acquired the lock; must not be interruptible
		 */
	}

	public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
		/*
		 * TODO: Try to acquire the lock
		 */
		return false;
	}

	public void unlock() {
		/*
		 * TODO: Release the lock
		 */
	}

}
