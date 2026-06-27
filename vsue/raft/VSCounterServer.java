package vsue.raft;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import vsue.raft.VSCounterMessages.VSCounterReply;
import vsue.raft.VSCounterMessages.VSCounterRequest;
import vsue.raft.VSCounterMessages.VSLeaderIdNotification;

/**
 * Replicated implementation of the counter server
 */
public class VSCounterServer implements VSCounterService {

	/**
	 * 封装并持久化存储客户端请求的执行结果。
	 */
	private static class VSCounterResultWrapper implements Serializable {
		/**
		 * 这两个字段组合在一起构成了该请求的唯一指纹
		 */
		public final long clientId; // 标识是哪一个客户端发送的请求。
		public final long requestCounter;// 该客户端发送请求的序列号。
		/**
		 * 计数器逻辑真正执行后返回的数据（例如自增后的最新计数值）。
		 */
		public final VSCounterReply result;

		public VSCounterResultWrapper(long clientId, long requestCounter, VSCounterReply result) {
			this.clientId = clientId;
			this.requestCounter = requestCounter;
			this.result = result;
		}

		@Override
		public String toString() {
			return "VSResultWrapper{[" + clientId + "-" + requestCounter + "], result=" + result + '}';
		}
	}

	/**
	 * Java 的异步工具（Future）来协调 Raft 的多阶段共识过程。
	 * Map 收到客户端请求和请求真正执行这两个时刻之间进行同步。
	 * 当 Leader 的 handleRequest 收到客户端请求时，它还不能立即返回结果，因为 Raft 必须先将请求复制并提交
	 * Leader 会创建一个 CompletableFuture，并将其存入 resultNotifier。此时，处理请求的线程会处于“等待”状态。
	 * 协议提交与执行：当 Raft 协议确认该日志项已提交，并调用 applyRequest()
	 * 执行命令时，服务器会通过 resultNotifier 找到对应的 Future 并“填充”执行结果（即那个 Wrapper）
	 * 一旦 Future 被填充，之前等待的线程就会被唤醒，将结果返回给客户端。
	 */
	private final Map<VSCounterRequest, CompletableFuture<VSCounterResultWrapper>> resultNotifier = new HashMap<>();
	/**
	 * 确保“有且仅执行一次”的缓存;
	 * 在主动复制中，所有副本都会执行所有请求
	 * Map 只能在 applyRequest 内部修改。
	 * 每当一个请求被应用到状态机时，其结果会按 clientId（即 Long 类型的键）存入resultMap
	 * 如果一个客户端因为没收到响应而重新发送了相同的请求，Leader 会首先检查 resultMap。
	 * 如果发现该客户端 ID 对应的最新请求序列号（在Wrapper 中）与当前请求一致，
	 * 它会直接返回缓存的结果，而不再将其交给 Raft 重新复制和执行
	 */
	private final Map<Long, VSCounterResultWrapper> resultMap = new HashMap<>();
	private final VSRaftProtocol protocol;
	private final int myId;
	private final int replicaCount;

	/**
	 * VSRaftProtocol）的核心任务就是
	 * 确保集群中所有副本上的这个 counter 变量在执行相同序列的指令后
	 * 能够保持完全一致的数值
	 * 
	 * 每个副本都会在本地维护自己的 counter 变量。
	 * 当 Raft 协议确认某个请求（例如客户端发出的 inc 指令）已经成功提交（Committed）到大多数节点后
	 * 每个副本都会调用 applyRequest() 方法。该方法会根据指令修改这个 counter的值（例如执行 counter++
	 */
	private int counter = 0;

	// 被包含在快照（Snapshot）中或已被应用到状态机的最后一条日志项的索引（Log-Index）
	private long lastIndex = 0;
	/**
	 * 对应于 lastIndex 那条日志项被创建时的任期号（Term）
	 */
	private int lastTerm = -1;
	

	private boolean isLeader;
	private int currentLeaderId;

	/**
	 * Create a new instance of the counter service
	 *
	 * @param protocol     Raft protocol instance used to coordinate replicas
	 * @param replicaId    id of the current replica. addresses[replicaId] is the
	 *                     address of the registry for use by the current replica
	 * @param replicaCount total number of replicas
	 */
	public VSCounterServer(VSRaftProtocol protocol, int replicaId, int replicaCount) {
		this.protocol = protocol;
		this.myId = replicaId;
		this.replicaCount = replicaCount;

		isLeader = false;
		currentLeaderId = -1;
	}

	/**
	 * Log msg to stdout and prefix it with the current time
	 *
	 * @param msg log message
	 */
	private void log(String msg) {
		System.out.println(LocalTime.now().truncatedTo(ChronoUnit.MILLIS) + " app " + msg);
	}

	/**
	 * Initializes the protocol and the counter application. The protocol is
	 * started before exporting the counter server in a local registry, to
	 * ensure that requests can be processed immediately.
	 *
	 * @param registryPort port to use for the counter server registry
	 * @throws RemoteException thrown if registry create or application export fails
	 */
	public void init(int registryPort) throws RemoteException {
		// Initialize raft protocol before exporting the application
		protocol.init(this);

		// Export server
		UnicastRemoteObject.exportObject(this, 0);

		// Register server
		Registry registry = LocateRegistry.createRegistry(registryPort);
		try {
			registry.bind(VSCounterService.SERVICE_NAME, this);
		} catch (AlreadyBoundException e) {
			throw new RemoteException("Don't bind twice", e);
		}
	}

	/**
	 * Update role and leaderId. Must be called by the protocol whenever the
	 * role of this replica changes or when the leader replica changes.
	 *
	 * @param role     raft protocol role of the current replica
	 * @param leaderId id of the current leader or -1 if there is no leader
	 */
	public synchronized void status(VSRaftProtocol.VSRaftRole role, int leaderId) {
		if (role == null || leaderId <= -2 || leaderId >= replicaCount) {
			throw new IllegalArgumentException("Invalid leader id or role. Got: leaderId=" +
					leaderId + " role=" + role);
		} else if (role == VSRaftProtocol.VSRaftRole.LEADER && leaderId != myId) {
			throw new IllegalArgumentException("LeaderId must be that of the current replica" +
					" as this replica is the leader. Got: leaderId=" + leaderId + " role=" + role);
		} else if (role == VSRaftProtocol.VSRaftRole.CANDIDATE && leaderId != -1) {
			throw new IllegalArgumentException("LeaderId must be == -1 if this replica is a" +
					" candidate. Got: leaderId=" + leaderId + " role=" + role);
		} else if (role == VSRaftProtocol.VSRaftRole.FOLLOWER && leaderId == myId) {
			throw new IllegalArgumentException("This replica cannot be the leader and a follower" +
					" at the same time. Got: leaderId=" + leaderId + " role=" + role);
		}

		log("status update: role=" + role + " leaderId=" + leaderId);

		isLeader = VSRaftProtocol.VSRaftRole.LEADER.equals(role);
		currentLeaderId = leaderId;

		if (!isLeader) {
			// 处理“退位”后的遗留请求:
			// 譬如自己作为老leader在选举中落败; 那么必须取消掉之前所有future中等待自己返回给client的所有请求
			for (CompletableFuture<VSCounterResultWrapper> future : resultNotifier.values()) {
				future.cancel(false);// 解除阻塞
			}
			resultNotifier.clear();
		}
	}

	// see VSCounterService.handleRequest for the documentation
	public VSCounterReply handleRequest(VSCounterRequest request) throws RemoteException, VSLeaderIdNotification {
		log("Got request from client " + request);

		VSCounterResultWrapper result;
		synchronized (this) {
			// check if there is already a stored result
			result = resultMap.get(request.clientId);
			if (result != null) {
				if (result.requestCounter < request.requestCounter) {
					// 新的请求,缓存过期了;
					result = null;
				} else if (result.requestCounter > request.requestCounter) {
					//客户端发起的旧请求;缓存里的结果太新(一般是出错了);
					throw new RemoteException("Rejecting old request");
				}
			}
		}

		if (result == null) { // 现在需要执行,获取最新的结果
			// 为异步执行的 Raft 请求建立一个“结果通知占位符”。 等待大部分follower的成功返回;
			CompletableFuture<VSCounterResultWrapper> replyFuture;
			synchronized (this) {
				// 检查是否存在：查看这个请求是否已经在处理队列中（可能因为客户端重试发送了相同的请求
				if (!resultNotifier.containsKey(request)) {
					// 如果是一个新请求，就创建一个新的 CompletableFuture 存入 Map。
					resultNotifier.put(request, new CompletableFuture<>());
				}
				// 已存在的请求，就获取之前的那个 Future。
				replyFuture = resultNotifier.get(request);
			}

			// 让raft协议处理; 如果是不是leader,返回false告知leaderid;
			//如果是leader,复制log给所有follower,等待大部分的true;
			boolean accepted = protocol.orderRequest(request);

			synchronized (this) {
				if (!accepted) {
					// 不是leader,告知leaderid;
					resultNotifier.remove(request, replyFuture);
					// the protocol rejected the request, probably because it is
					// no longer a leader. Tell the client to retry it's request.
					throw new VSLeaderIdNotification(currentLeaderId);
				}
			}

			//这里就是执行成功了,future对象会在拿到大多数投票成功的applyRequest()里被填充;
			try {
				result = replyFuture.get(); 
				// 阻塞,等待complete,只要大部分都返回true
				// 就会在applyRequest()里被填充,唤醒这里的get()方法;
			} catch (CancellationException e) {
				// tell the client to retry it's request
				throw new VSLeaderIdNotification(-1);
			} catch (InterruptedException | ExecutionException e) {
				// remote execution of request was interrupted
				throw new RemoteException("Execution failed", e);
			}
		}

		return result.result;
	}

	/**
	 * Applies an ordered log entry from the raft protocol. The counter server
	 * sanity checks that no log entries were skipped. The contained request is
	 * only executed if no newer request from the same client has been executed.
	 * Finally the result is stored and a waiting handleRequest call gets notified.
	 *
	 * @param entry log entry with request to execute
	 */
	// 已经commited,就正式执行,并且放入缓存;
	public synchronized void applyRequest(VSRaftLogEntry entry) {
		if (entry.request == null) {
			throw new IllegalArgumentException("Stub elements must not be passed to the application");
		}
		// 检查 lastTerm > entry.term 确保不会应用来自旧任期的冲突日志
		// entry.index != lastIndex + 1 如果收到的索引不是紧接着上一个执行的索引，说明日志出现了空洞，违反了 Raft 的顺序性保证
		if (lastTerm > entry.term || entry.index != lastIndex + 1) {
			throw new IllegalArgumentException("Expected a log entry for index " + (lastIndex + 1)
					+ " with at least term " + lastTerm + ", but got: index "
					+ entry.index + " term " + entry.term);
		}
		lastTerm = entry.term;
		lastIndex = entry.index;

		// 拿出request,准备执行;
		VSCounterRequest request = (VSCounterRequest) entry.request;

		// 如果已经执行过了,直接返回;[幂等]
		VSCounterResultWrapper lastResult = resultMap.get(request.clientId);
		// lastResult != null 代表已经被执行过了;真的拿出来结果了
		// 客户发出的requestcounter更小???那说明早就执行过了,不要重新执行;
		if (lastResult != null && request.requestCounter <= lastResult.requestCounter) {
			return;// 如果判断为重复请求，则直接退出方法，不再执行后续的计数器自增操作（processRequest）
		}

		// process request and store result
		VSCounterReply reply = processRequest(request); //counter++
		VSCounterResultWrapper result = new VSCounterResultWrapper(request.clientId, request.requestCounter, reply);
		resultMap.put(request.clientId, result);

		// 先找到当初在等待majority返回的future待填充对象;因为要填充了,所以remove掉;
		// 拿出来放进去result即可(complete)变更状态,唤醒client那边一直等待的;
		CompletableFuture<VSCounterResultWrapper> replyFuture = resultNotifier.remove(request);
		if (replyFuture != null) {
			replyFuture.complete(result);// “知道填充了”是通过 complete() 方法主动触发状态变更实现的。
		}
	}

	/**
	 * Executes the client request
	 *
	 * @param request client request
	 * @return result of request execution
	 */
	private VSCounterReply processRequest(@SuppressWarnings("unused") VSCounterRequest request) {
		counter++;
		return new VSCounterReply(counter);
	}


	/**
	 * Copy of the application state. Used for the raft snapshot mechanism
	 */
	private static class VSCounterSnapshot implements Serializable {
		// index/term for sanity checks
		public final long lastIncludedIndex;
		public final int lastIncludedTerm;
		// application data
		public final int counter;
		// result cache copy
		public final HashMap<Long, VSCounterResultWrapper> results;

		public VSCounterSnapshot(long lastIncludedIndex, int lastIncludedTerm, int counter, Map<Long, VSCounterResultWrapper> results) {
			this.lastIncludedIndex = lastIncludedIndex;
			this.lastIncludedTerm = lastIncludedTerm;
			this.counter = counter;
			this.results = new HashMap<>(results);
		}

		@Override
		public String toString() {
			return "VSCounterSnapshot{" +
					"lastIncludedIndex=" + lastIncludedIndex +
					", lastIncludedTerm=" + lastIncludedTerm +
					", counter=" + counter +
					'}';
		}
	}

	/**
	 * Applies the given snapshot to the application state. The applied snapshot
	 * must be at least as recent as the current application state. Rolling back
	 * to an older state is not permitted.
	 *
	 * @param snapshot Counter state snapshot to apply
	 */
	public synchronized void applySnapshot(Serializable snapshot) {
		VSCounterSnapshot data = ((VSCounterSnapshot) snapshot);
		if (data.lastIncludedIndex < lastIndex) {
			throw new IllegalArgumentException("Must not apply an old snapshot, snapshot idx="
					+ data.lastIncludedIndex + " lastIndex=" + lastIndex);
		}

		counter = data.counter;
		resultMap.clear();
		resultMap.putAll(data.results);
		lastTerm = data.lastIncludedTerm;
		lastIndex = data.lastIncludedIndex;
	}

	/**
	 * Creates a snapshot of the current application state. The snapshot can
	 * later on be applied on other replicas using applySnapshot.
	 *
	 * @return Snapshot of the current application state and result cache
	 */
	public synchronized Serializable createSnapshot() {
		return new VSCounterSnapshot(lastIndex, lastTerm, counter, resultMap);
	}
}
