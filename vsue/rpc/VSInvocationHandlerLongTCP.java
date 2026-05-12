package vsue.rpc;

import java.lang.reflect.InvocationHandler;
import java.io.Serializable;
import vsue.myrmi.VSConnection;
import vsue.myrmi.VSObjectConnection;

import java.lang.reflect.Method;

public class VSInvocationHandlerLongTCP implements InvocationHandler,Serializable {
    private VSRemoteReference remoteReference;
    private transient VSObjectConnection connection;
    public VSInvocationHandlerLongTCP(VSRemoteReference remoteReference, VSObjectConnection connection) {
        this.remoteReference = remoteReference;
        this.connection = connection;
    }

    public VSRemoteReference getRemoteReference() {
        return remoteReference;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                System.err.println("⚠️  Failed to close connection: " + e.getMessage());
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. Create a request message
        RequestMessage request = new RequestMessage(method.getName(), args, remoteReference.getObjectId());     
        // 2. Send the request to the server and wait for a reply
        connection.sendObject(request);
        ReplyMessage reply = (ReplyMessage) connection.receiveObject();
        // 3. Process the reply
        if (reply.getException() != null) {
            System.err.println("⚠️  Exception occurred during remote method invocation: " + reply.getException().getMessage());
            throw reply.getException();
        } else {
            return reply.getResult();
        }   
    }
}
