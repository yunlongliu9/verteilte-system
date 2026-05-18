package vsue.rpc;

import java.io.Serializable;
import java.net.Socket;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import vsue.faults.ReplyMessage;
import vsue.myrmi.VSConnection;
import vsue.myrmi.VSObjectConnection;

public class VSInvocationHandler implements Serializable,InvocationHandler  {
    private VSRemoteReference remoteReference;

    public VSRemoteReference getRemoteReference() {
        return remoteReference;
    }
    

    public VSInvocationHandler(VSRemoteReference remoteReference) {
        this.remoteReference = remoteReference;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        VSObjectConnection connection = new VSObjectConnection(new VSConnection(new Socket(remoteReference.getHostIP(), remoteReference.getPort())));
        // Create a request message with the method name, parameters, and object ID
        RequestMessage requestMessage = new RequestMessage(method.getName(), args, remoteReference.getObjectId());
        ReplyMessage replyMessage = null;
        try{
            connection.sendObject(requestMessage); // Send the request message to the server
            // Send the request message to the server and receive a reply message
            replyMessage = (ReplyMessage)connection.receiveObject(); // Receive the reply message from the server
            // Check if the reply message contains an exception
            if (replyMessage.getException() != null) {
                throw replyMessage.getException(); // Throw the exception if it exists
            }
        }catch(Exception e){
            System.err.println("⚠️  Exception occurred during remote method invocation: " + e.getMessage());
            throw e; // Rethrow the exception to be handled by the caller
        }finally{
            try {
                connection.close(); // Ensure the connection is closed even if an exception occurs
            } catch (Exception e) {
                System.err.println("⚠️  Failed to close connection: " + e.getMessage());
            }
        }
        return replyMessage.getResult(); // Return the result from the reply message
    }
}