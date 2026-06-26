package vsue.raft;

import java.io.Serializable;

/**
 * A single entry in the raft log. Contains the index and term of the log
 * entry along with the associated request.
 */
public class VSRaftLogEntry implements Serializable {
	public final long index;
	public final int term;
	public final Serializable request;

	public VSRaftLogEntry(long index, int term, Serializable request) {
		this.index = index;
		this.term = term;
		this.request = request;
	}
}
