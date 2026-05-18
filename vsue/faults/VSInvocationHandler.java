package vsue.faults;

import java.io.Serializable;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import vsue.faults.ReplyMessage;
import vsue.myrmi.VSConnection;
import vsue.faults.VSObjectConnection;
import vsue.rpc.VSRemoteReference;

public class VSInvocationHandler implements Serializable,InvocationHandler  {
    private VSRemoteReference remoteReference;
    private static long requestCounter = 0;
    private final String clientID = UUID.randomUUID().toString();
    private final static int MAX_RETRIES = 3; // Maximum number of retries for a remote method invocation
    private transient VSObjectConnection injectedConnection;
    public VSRemoteReference getRemoteReference() {
        return remoteReference;
    }
    

    public VSInvocationHandler(VSRemoteReference remoteReference, VSObjectConnection connection) {
        this.remoteReference = remoteReference;
        this.injectedConnection = connection;
    }

    public VSInvocationHandler(VSRemoteReference remoteReference) {
        this.remoteReference = remoteReference;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        VSObjectConnection connection =  injectedConnection;
        Socket socket = null;
        if (connection == null) {
                socket = new Socket(
                remoteReference.getHostIP(),
                remoteReference.getPort()
            );
            connection =
                new VSObjectConnection(
                    new VSConnection(socket)
                );
        }
        VSRPCSemantic semantic = method.getAnnotation(VSRPCSemantic.class);
        System.out.println("Invoking method: " + method.getName());
        // Create a request message with the method name, parameters, and object ID
        VSRequestID requestID = new VSRequestID(clientID+"-"+ (++requestCounter), 0); // Generate a unique request ID for this invocation
        RequestMessage requestMessage = new RequestMessage(method.getName(), args, remoteReference.getObjectId(),requestID);; // Generate a unique request ID for this invocation
        ReplyMessage replyMessage = null;
        try{
            System.out.println("Invoking method begin: " + method.getName() );  
        if (semantic != null ){ 
            if (socket != null) {
                socket.setSoTimeout(3000);
            }else{
                socket = connection.getSocket();
                socket.setSoTimeout(3000);
            }
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                    requestMessage.setSequenceNumber(attempt);
                    System.out.println("Attempt " + attempt + " for method: " + method.getName());  
                    try{
                        connection.sendObject(requestMessage); // Send the request message to the server
                        // Send the request message to the server and receive a reply message
                        while (true) {
                            replyMessage = (ReplyMessage) connection.receiveObject();
                            if (!replyMessage.getRequestID().getCallID().equals(requestID.getCallID())) {
                                continue;// other rpc reply
                            }
                            if (replyMessage.getRequestID().getSequenceNumber() != attempt) {
                                continue; // old reply
                            }
                            if (replyMessage.getRequestID().getSequenceNumber() == attempt) {
                                if (replyMessage.getException() != null) {
                                    throw replyMessage.getException();
                                }
                                return replyMessage.getResult(); // right reply
                            }

                        }
                    }catch(SocketTimeoutException e){
                        System.err.println("⚠️  Attempt " + attempt + " failed: " + e.getMessage());
                        if (attempt == MAX_RETRIES) {
                            throw new RuntimeException("Maximum retries exceeded"); // Rethrow the exception if we've reached the maximum number of retries
                        }
                        continue; // Retry the invocation
                    }
                }
        }else{
            connection.sendObject(requestMessage); // Send the request message to the server
            // Send the request message to the server and receive a reply message
            replyMessage = (ReplyMessage)connection.receiveObject(); // Receive the reply message from the server
            // Check if the reply message contains an exception
            if (replyMessage.getException() != null) {
                throw replyMessage.getException(); // Throw the exception if it exists
            }
            return replyMessage.getResult(); // Return the result from the reply message
        } 
            
        }catch(Exception e){
            System.err.println("⚠️  Exception occurred during remote method invocation: " + e.getMessage());
            throw e; // Rethrow the exception to be handled by the caller
        } finally {
            if (injectedConnection == null) {
                connection.close();
            }
        }
        throw new RuntimeException("Unreachable");
    }
}