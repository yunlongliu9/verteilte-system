package vsue.rpc;

import java.io.Externalizable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.lang.reflect.Proxy;
import vsue.communication.*;
import vsue.faults.VSRPCSemantic;
import vsue.faults.VSRPCSemanticDefaultHandler;
import vsue.faults.VSRPCSemanticHandler;
import vsue.faults.VSRequestID;

public class VSInvocationHandler implements InvocationHandler, Externalizable {
	private VSRemoteReference remote;
	private boolean reuseConnection;
	private boolean useBuggyConnections;
	private transient Socket socket;
	private transient VSObjectConnection intercom;

	// Für Externalizable
	public VSInvocationHandler() {
	}

	public VSInvocationHandler(VSRemoteReference remote) {
		this(remote, false, false);
	}

	public VSInvocationHandler(VSRemoteReference remote, boolean reuseConnection) {
		this(remote, reuseConnection, false);
	}

	public VSInvocationHandler(VSRemoteReference remote, boolean reuseConnection, boolean useBuggyConnections) {
		this.remote = remote;
		this.reuseConnection = reuseConnection;
		this.useBuggyConnections = useBuggyConnections;
	}

	private VSRPCSemanticHandler dispatchSemanticHandler(Method method){
		VSRPCSemantic semantic = method.getAnnotation(VSRPCSemantic.class);
		VSRPCSemanticHandler handler;
		if (semantic == null) {
			handler = new VSRPCSemanticDefaultHandler();
		} else {
			handler = semantic.value().createHandler();
		}
		return handler;
	}

	private boolean callBackCheck(Request request, Method method, VSObjectConnection connection) throws Throwable {
		if (method.getName().equals("handleEvent")) {
			connection.sendObject(request);
			return true;
		} else {
			return false;
		}
	}

	private void argsTransform(Object[] args) {// object->stub
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Remote && !Proxy.isProxyClass(args[i].getClass())){
					args[i] = VSRemoteObjectManager.getInstance().lookUpStub((Remote) args[i]); // convert to stub if but not a stub yet	
				}																			
			}
		}
	}

	private Request requestBuild(Method method, Object[] args){
		Request request = new Request(remote.getObjectID(), method.toGenericString().hashCode(), args,new VSRequestID(VSRemoteObjectManager.username,method.getName()), VSRemoteObjectManager.username);
		return request;
	}

	@Override
	public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		argsTransform(args);
		VSRPCSemanticHandler handler = dispatchSemanticHandler(method);
		Request request = requestBuild(method,args);

        try {
            if (!reuseConnection) {
                try (Socket socket = new Socket(remote.getHost(), remote.getPort())) {
                    VSObjectConnection connection = createConnection(socket);
                    if (callBackCheck(request, method, connection)) {
                        return null;
                    }
                    return handler.invoke(
                        this,
                        request,
                        method,
                        connection);
                }
            } else {
                // Reuse connection: create if not exists, otherwise use existing one
                if (intercom == null) {
                    socket = new Socket(remote.getHost(), remote.getPort());
                    intercom = createConnection(socket);
                }
                if (callBackCheck(request, method, intercom)) {
                    return null;
                }
                return handler.invoke(
                        this,
                        request,
                        method,
                        intercom);
            }
        } catch (java.io.IOException e) {
            throw new java.rmi.RemoteException("Network connection failed", e);
        }
	}

	// Hilfsmethode zum Erstellen einer Verbindung, abhängig von der Einstellung für fehlerhafte Verbindungen
	private VSObjectConnection createConnection(Socket socket) throws IOException {
	    socket.setTcpNoDelay(true);
	    if (useBuggyConnections) {
		    return new VSBuggyObjectConnection(socket);
	    } else {
		    return new VSObjectConnection(socket);
	    }
	}

	public Response transportProcess(Request request, VSObjectConnection connection) throws Throwable {
		try {
			connection.sendObject(request);
			return (Response) connection.receiveObject();
		} catch (VSConnectionEndOfFile e) {
			throw e;
		}
	}

	public Object handleResponse(Response response, Class<?> returnType) throws Throwable {
		Throwable exception = response.getException();
		if (exception != null) {
			throw exception;
		}
		if (returnType == Void.TYPE) {
		    return null;
		} else {
		    return response.getResult();
		}
	}

	public synchronized void closeConnection() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// ignore during cleanup
			}
		}
		socket = null;
		intercom = null;
	}

	@Override
	public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException {
		remote.writeExternal(out);
		out.writeBoolean(reuseConnection);
	}

	@Override
	public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
		remote = new VSRemoteReference();
		remote.readExternal(in);
		this.reuseConnection = in.readBoolean();
	}
}
