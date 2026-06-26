package vsue.raft;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * Implementation of the raft protocol
 */
public class VSRaftProtocol implements VSRaftProtocolService {

	/**
	 * Roles of a replica as defined by raft
	 */
	enum VSRaftRole {
		LEADER,
		FOLLOWER,
		CANDIDATE
	}

	// Order of magnitude between timeouts, see raft paper section 5.6
	private static final int ELECTION_TIMEOUT = 3000;
	private static final int HEARTBEAT_TIMEOUT = 300;

	private final InetSocketAddress[] addresses;
	private final int myId;


	// Fields defined in Figure 2 of the raft paper
	// persistent
	private int currentTerm; // 当前任期,逻辑时钟
	private int votedFor; // 当前任期投票给谁了,如果没有投票给任何人,则为-1
	private VSRaftLog log;

	// volatile 易失性状态——这些状态在重启后会重置为 0
	private long commitIndex; // (已提交索引)（即被集群大多数节点确认）的最高日志索引
	private long lastApplied;// (最后应用索引)已经被应用到状态机（即计数器服务）的最高日志索引

	// volatile leader 仅在领导者上存在的易失性状态
	// 选举成功后，Leader 必须为每个 Follower 维护这些进度信息
	// 记录 Leader 准备发给每个 Follower 的下一个日志条目的索引
	// 初始为 Leader 最新日志索引 + 1
	private long[] nextIndex;
	// 记录 Leader 已知每个 Follower 已经成功拥有的最高日志索引。初始为
	private long[] matchIndex;
	// 譬如matchIndex[R1] = 5;
	// leader明确知道R1节点已经和自己从开始到5点所有log都同步了


	/**
	 * Create a new instance of the raft protocol
	 *
	 * @param replicaId 当前副本在集群中的唯一编号
	 *                  Raft 节点需要知道自己是谁
	 *                  以便在投票请求（RequestVote）中标识自己
	 *                  或者判断来自 Leader的心跳是否是发给自己的
	 * @param addresses 存储了集群中所有副本的注册表地址
	 *                  当节点需要发送心跳或拉票时，
	 * 它会利用这些地址通过 getStub(replicaId) 方法获取其他节点的 stub               
	 */
	public VSRaftProtocol(int replicaId, InetSocketAddress[] addresses) {
		myId = replicaId;
		this.addresses = addresses;

		/*
		 * TODO: initialize protocol state
		 */
	}


	/**
	 * Initialize the raft protocol instance. Exports the protocol instance and
	 * makes it accessible via a registry instance for this protocol. Runs a
	 * connection test afterwards and initiates periodic protocol tasks.
	 *
	 * @param application Counter server application. Provides status(), applyRequest(),
	 *                    createSnapshot() and applySnapshot() methods
	 * @throws RemoteException Failed to export protocol or setup registry
	 */
	public void init(VSCounterServer application) throws RemoteException {
		/*
		 * TODO: Exercise Section 5.1: Export protocol via a registry, use testConnection()
		 *  Use the address from addresses[myId] for the registry
		 * TODO: Exercise Section 5.2: Start protocol thread
		 */
	}

	/**
	 * Returns a VSRaftProtocolService stub for the specified replica.
	 *
	 * @param replicaId Id of replica to connect to
	 * @return VSRaftProtocolService stub
	 * @throws RemoteException Failed to retrieve the stub
	 */
	private VSRaftProtocolService getStub(int replicaId) throws RemoteException {
		/*
		 * TODO: Exercise Section 5.1: Implement stub retrieval
		 */
		return null;
	}

	/**
	 * Discard a cached VSRaftProtocolService stub for the specified replica.
	 *
	 * @param replicaId Id of replica whose stub should be dropped
	 */
	private void discardStub(int replicaId) {
		/*
		 * TODO: Exercise Section 5.1: Forget broken stub for replicaId
		 */
	}

	/**
	 * Basic test whether communication with the other replicas is possible.
	 */
	private void testConnection() {
		for (int i = 0; i < addresses.length; i++) {
			if (i == myId) {
				continue;
			}

			while (true) {
				VSRaftProtocolService stub = null;
				try {
					stub = getStub(i);
				} catch (RemoteException e) {
					System.out.println("Retrying getStub for replica " + i + " after exception: " + e);
				}
				if (stub != null) {
					try {
						// issue request for old term which must always be rejected
						stub.requestVote(-1, myId, -1, -1);
						break;
					} catch (RemoteException e) {
						System.out.println("Retrying connection to replica " + i + " after exception: " + e);
						discardStub(i);
					} catch (UnsupportedOperationException e) {
						// UnsupportedOperationException is thrown by the rpc stub implementation
						break;
					}
				}
				try {
					//noinspection BusyWait
					Thread.sleep(500);
				} catch (InterruptedException interruptedException) {
					interruptedException.printStackTrace();
				}
			}
			System.out.println("Connection to replica " + i + " successful");
		}
		System.out.println("Connection test completed");
	}


	@Override
	// see VSRaftProtocolService.requestVote for the documentation
	public synchronized VSRaftRPCResult requestVote(int term, int candidateId, long lastLogIndex, int lastLogTerm) {
		/*
		 * TODO: Exercise Section 5.2: Implement leader election
		 */
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	// see VSRaftProtocolService.appendEntries for the documentation
	public synchronized VSRaftRPCResult appendEntries(int term, int leaderId, long prevLogIndex, int prevLogTerm,
	                                                  VSRaftLogEntry[] entries, long leaderCommit) {
		/*
		 * TODO: Exercise Section 5.3: Implement log replication
		 */
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Appends a request to the log for ordering. If the current replica is not
	 * the leader at the moment, the request is rejected. Called by the
	 * VSCounterServer.
	 *
	 * @param request Request to append to the log
	 * @return True when the current replica is the leader, false otherwise
	 */
	public synchronized boolean orderRequest(Serializable request) {
		/*
		 * TODO: Exercise Section 5.3: Implement log replication
		 */
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	// see VSRaftProtocolService.installSnapshot for the documentation
	public synchronized int installSnapshot(int term, int leaderId, long lastIncludedIndex,
	                                        int lastIncludedTerm, Serializable data) {
		/*
		 * TODO: Exercise Section 5.4: Implement snapshotting
		 */
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
