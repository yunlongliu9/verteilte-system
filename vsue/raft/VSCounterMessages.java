package vsue.raft;

import java.io.Serializable;
import java.util.Objects;

/**
 * Messages exchanged between counter client and replicas
 */
public class VSCounterMessages {
	/**
	 * Request increase of the counter by one. Includes a unique request
	 * identifier. Used by the replicas to detect request retransmissions
	 * and match replies to requests.
	 */
	public static class VSCounterRequest implements Serializable {
		public final long clientId;
		public final long requestCounter;

		public VSCounterRequest(long clientId, long requestCounter) {
			this.clientId = clientId;
			this.requestCounter = requestCounter;
		}

		@Override
		public String toString() {
			return "VSCounterRequest{[" + clientId + "-" + requestCounter + "]}";
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			VSCounterRequest that = (VSCounterRequest) o;
			return clientId == that.clientId && requestCounter == that.requestCounter;
		}

		@Override
		public int hashCode() {
			return Objects.hash(clientId, requestCounter);
		}
	}

	/**
	 * Result of the counter increase command. Contains the new counter value.
	 */
	public static class VSCounterReply implements Serializable {
		public final int counterValue;

		public VSCounterReply(int counterValue) {
			this.counterValue = counterValue;
		}

		@Override
		public String toString() {
			return "VSCounterReply{" + "counterValue=" + counterValue + '}';
		}
	}

	/**
	 * Notify the client that a different replica is the leader. Contains the
	 * id of the current leader replica.
	 */
	public static class VSLeaderIdNotification extends Exception {
		public final int leaderId;

		public VSLeaderIdNotification(int leaderId) {
			this.leaderId = leaderId;
		}

		@Override
		public String toString() {
			return "VSLeaderIdNotification{" + "leaderId=" + leaderId + '}';
		}
	}
}
