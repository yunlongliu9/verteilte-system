package vsue.raft;

import java.io.Serializable;

/**
 * Generic return type for raft RPCs returning a term and a boolean
 * success / voteGranted flag.
 */
public class VSRaftRPCResult implements Serializable {
	public final int term;
	public final boolean success;

	public VSRaftRPCResult(int term, boolean success) {
		this.term = term;
		this.success = success;
	}
}
