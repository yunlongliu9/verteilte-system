package vsue.raft;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface with RPCs used by the raft protocol.
 */
public interface VSRaftProtocolService extends Remote {

	String SERVICE_NAME = "ReplicaService";

	/**
	 * Request vote sent by candidate replica. See the raft paper for details.
	 *
	 * @param term         term of calling replica
	 * @param candidateId  id of calling replica
	 * @param lastLogIndex index used to check whether candidate has up to date log
	 * @param lastLogTerm  term used to check whether candidate has up to date log
	 * @return Current term and whether the vote was granted
	 * @throws RemoteException RPC failed
	 */
	VSRaftRPCResult requestVote(int term, int candidateId, long lastLogIndex, int lastLogTerm) throws RemoteException;

	/**
	 * Replicate log entries and provide heartbeat from leader. See the raft paper for details.
	 *
	 * @param term         term of calling replica
	 * @param leaderId     id of calling replica, which is now the leader replica
	 * @param prevLogIndex log index immediately before new entries
	 * @param prevLogTerm  associated log term
	 * @param entries      entries to store
	 * @param leaderCommit commit index from leader
	 * @return Current term and whether the prevLogIndex/Term were matching
	 * @throws RemoteException RPC failed
	 */
	VSRaftRPCResult appendEntries(int term, int leaderId, long prevLogIndex, int prevLogTerm,
	                              VSRaftLogEntry[] entries, long leaderCommit) throws RemoteException;

	/**
	 * Apply snapshot at receiving replica. See the raft paper for details.
	 *
	 * @param term              term of calling replica
	 * @param leaderId          id of calling replica, which is now the leader replica
	 * @param lastIncludedIndex last index included in snapshot
	 * @param lastIncludedTerm  last term included in snapshot
	 * @param data              snapshot data
	 * @return Current term
	 * @throws RemoteException RPC failed
	 */
	int installSnapshot(int term, int leaderId, long lastIncludedIndex, int lastIncludedTerm,
	                    Serializable data) throws RemoteException;
}
