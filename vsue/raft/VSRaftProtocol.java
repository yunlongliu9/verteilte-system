package vsue.raft;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

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

	private VSCounterServer application;
	private VSRaftRole role;
	private long lastContactTime;
	private long currentRandomTimeout;

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
		// Fields defined in Figure 2 of the raft paper
		// persistent
		currentTerm = 0; // 当前任期,逻辑时钟
		votedFor = -1; // 当前任期投票给谁了,如果没有投票给任何人,则为-1
		log = new VSRaftLog();

		// volatile 易失性状态——这些状态在重启后会重置为 0
		commitIndex = 0; // (已提交索引)（即被集群大多数节点确认）的最高日志索引
		lastApplied = 0;// (最后应用索引)已经被应用到状态机（即计数器服务）的最高日志索引

		// volatile leader 仅在领导者上存在的易失性状态
		// 选举成功后，Leader 必须为每个 Follower 维护这些进度信息
		// 记录 Leader 准备发给每个 Follower 的下一个日志条目的索引
		// 初始为 Leader 最新日志索引 + 1
		nextIndex = new long[addresses.length];
		// 记录 Leader 已知每个 Follower 已经成功拥有的最高日志索引。初始为
		matchIndex = new long[addresses.length];
		// 譬如matchIndex[R1] = 5;
		// leader明确知道R1节点已经和自己从开始到5点所有log都同步了
		role = VSRaftRole.FOLLOWER;
		lastContactTime = 0;
	    currentRandomTimeout = (int) (ELECTION_TIMEOUT* (0.8 + Math.random() * 0.2));
	}

	private void resetElectionTimeout(){
		currentRandomTimeout = (int) (ELECTION_TIMEOUT* (0.8 + Math.random() * 0.2));
		lastContactTime = System.currentTimeMillis();	
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
		this.application = application;
		Remote stub = UnicastRemoteObject.exportObject(this, 0);
		// Register server
		Registry registry = LocateRegistry.createRegistry(addresses[myId].getPort());
		try {
			registry.bind("RAFT", stub);
		} catch (AlreadyBoundException e) {
			throw new RemoteException("Don't bind twice", e);
		}
		/*
		 * TODO: Exercise Section 5.1: Export protocol via a registry, use testConnection()
		 *  Use the address from addresses[myId] for the registry
		 * TODO: Exercise Section 5.2: Start protocol thread
		 */
		new Thread(()->{
			application.status(VSRaftRole.FOLLOWER, -1);
				while (true) {
					synchronized(this){
						if (role.equals(VSRaftRole.LEADER)) {
							leaderLoop();
						} else if (role.equals(VSRaftRole.FOLLOWER)) {
							followerLoop(application);
						} else if (role.equals(VSRaftRole.CANDIDATE)) {
							candidateLoop(application);
						}
					}
				}
			
		}).start();
	}

	private void followerLoop(VSCounterServer application)  {
			// follower timeout
		if (System.currentTimeMillis() - lastContactTime > currentRandomTimeout) {
			this.role = VSRaftRole.CANDIDATE;
		}else{
			// follower not timeout
			try {
				long sleepTime = currentRandomTimeout - (System.currentTimeMillis() - lastContactTime);
				this.wait(Math.min(sleepTime, 50));
			} catch (InterruptedException e) {
				// 如果线程被中断，记录日志或者直接跳出
				System.out.println("Follower loop was interrupted");
				Thread.currentThread().interrupt(); // 重新标记中断状态，保持良好习惯
			}
		}
	}

	private void candidateLoop(VSCounterServer application){
		application.status(VSRaftRole.CANDIDATE, -1);
		resetElectionTimeout();
		currentTerm++;
		votedFor = myId;
		int stimme = 1;
		// election begin

		for (int i = 0; i < addresses.length; i++) {
			if (role != VSRaftRole.CANDIDATE) 
				return; // 期间可能被动变回 Follower
			if (i == myId)
				continue;
			try {
				VSRaftRPCResult res = getStub(i).requestVote(currentTerm, myId,
						log.getLatestIndex(), log.getLatestEntry().term);
				if (res.success) {
					stimme++;
				}
				if (currentTerm < res.term) {
					role = VSRaftRole.FOLLOWER;
					currentTerm = res.term;
					break;
				}
			} catch (RemoteException re) {
				discardStub(i); // 通信失败清理 Stub [12]
			}
		}

		if (stimme > addresses.length / 2) {
			role = VSRaftRole.LEADER;
			application.status(role, myId);
			long myLatestIndex = log.getLatestIndex();
			// become leader , immediately send heartbeat;
			for (int i = 0; i < addresses.length; i++) {
				nextIndex[i] = myLatestIndex + 1;
				matchIndex[i] = 0;
				if (i == myId)
					continue;
				try {
					VSRaftRPCResult res = getStub(i).appendEntries(currentTerm, myId,
							log.getLatestIndex(),
							log.getLatestEntry().term, null, commitIndex);
				} catch (RemoteException re) {
					discardStub(i); // 通信失败清理 Stub [12]
				}

			}
		}else{
			// fail the election? wait to time out and elect again
			try {
				long sleeptime = currentRandomTimeout - (System.currentTimeMillis() - lastContactTime);
				if (sleeptime>0){
					this.wait(sleeptime);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void leaderLoop(){
		for (int i = 0; i < addresses.length; i++) {
			if (i == myId) continue;
			try {
				long pIndex = nextIndex[i] - 1;
				int pTerm = log.getEntry(pIndex).term;

				// 2. 决定发送心跳还是发送新日志条目
				VSRaftLogEntry[] entriesToSend = null;
				if (log.getLatestIndex() >= nextIndex[i]) {
					entriesToSend = log.getEntriesSince(nextIndex[i]);
				}

				VSRaftRPCResult res = getStub(i).appendEntries(currentTerm, myId, 
								pIndex, pTerm, entriesToSend, commitIndex);
				long lastSentIndex = pIndex + (entriesToSend == null ? 0 : entriesToSend.length);
				handleAppendEntriesRes(res,i, lastSentIndex);
			} catch (RemoteException e) {// 没有收到心跳
				discardStub(i); // 通信失败清理 Stub [12]
			}
		}

		try {
			this.wait(HEARTBEAT_TIMEOUT);// heartbeat timeout=> begin new heartbeat
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	private void handleAppendEntriesRes(VSRaftRPCResult res,int i, long lastSentIndex){
				if (!res.success) {
					if (res.term > currentTerm) {
						// 情况 A: 发现更高任期，退位
						synchronized (this) {
							this.currentTerm = res.term;
							this.role = VSRaftRole.FOLLOWER;
							this.votedFor = -1;
							application.status(VSRaftRole.FOLLOWER, -1);
						}
					} else {
						// 情况 B: 日志不一致，为下一次同步做准备
						// 递减该副本的 nextIndex，下次循环会自动尝试同步较早的条目 [4]
						nextIndex[i] = Math.max(1, nextIndex[i] - 1);
					}
				}else{
					// 成功：更新该副本已匹配的最高索引 [1, 2]
					matchIndex[i] = Math.max(matchIndex[i], lastSentIndex);
					nextIndex[i] = matchIndex[i] + 1;

					// 尝试推进 Leader 的 commitIndex
					advanceCommitIndex();
				}
	}

	// Leader 必须根据所有副本的 matchIndex 来决定是否提交日志
	private void advanceCommitIndex() {
		// 寻找满足多数派（> N/2）的最大索引 N [1]
		for (long n = log.getLatestIndex(); n > commitIndex; n--) {
			int count = 1; // 计入自己
			for (int j = 0; j < addresses.length; j++) {
				if (j != myId && matchIndex[j] >= n)
					count++;
			}

			// 规则：多数派达成一致，且该条目必须是当前任期创建的 [1, 9]
			if (count > addresses.length / 2 && log.getEntry(n).term == currentTerm) {
				this.commitIndex = n;
				// 提醒：commitIndex 改变后，协议线程应调用 applyRequest() 通知应用 [2, 7, 10]
				break;
			}
		}
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
		
		// 1. 任期更新:当前node过于老旧; 继续参与选举
		if (term > currentTerm){
			currentTerm = term;
			role = VSRaftRole.FOLLOWER;
			votedFor = -1;
			application.status(VSRaftRole.FOLLOWER, -1);
		}

		// 此时此刻，所有小于我当前任期的请求都是“过期”的
		if (term < currentTerm) {
			return new VSRaftRPCResult(currentTerm, false);
		}

		long myLastLogTerm = log.getLatestEntry().term;
		long myLastLogIndex = log.getLatestIndex();

		boolean isLogNewest = (lastLogTerm > myLastLogTerm ) || (lastLogTerm==myLastLogTerm &&(lastLogIndex >= myLastLogIndex)) ;
		if (isLogNewest && (votedFor == -1 || votedFor == candidateId) ){
				votedFor = candidateId;
				lastContactTime = System.currentTimeMillis();
				return new VSRaftRPCResult(currentTerm, true);
		}else{
			return new VSRaftRPCResult(currentTerm, false);
		}
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
	 * leader节点:将操作添加到日志中;
	 *不是leader就拒绝;
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
