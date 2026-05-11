// package rpc;

// import java.lang.reflect.InvocationHandler;
// import java.io.Serializable;
// import vsue.myrmi.VSConnection;
// import vsue.myrmi.VSObjectConnection;

// import java.lang.reflect.Method;

// public class VSInvocationHandler implements InvocationHandler,Serializable {
//     private VSRemoteReference remoteReference;
//     private transient VSObjectConnection connection;
//     public VSInvocationHandler(VSRemoteReference remoteReference, VSObjectConnection connection) {
//         this.remoteReference = remoteReference;
//         this.connection = connection;
//     }

//     public VSRemoteReference getRemoteReference() {
//         return remoteReference;
//     }

//     @Override
//     public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//         // 1. Create a request message
//         RequestMessage request = new RequestMessage(method.getName(), args, remoteReference.getObjectId());     
//         // 2. Send the request to the server and wait for a reply
//         connection.sendObject(request);
//         ReplyMessage reply = (ReplyMessage) connection.receiveObject();
//         // 3. Process the reply
//         if (reply.getException() != null) {
//             System.err.println("⚠️  Exception occurred during remote method invocation: " + reply.getException().getMessage());
//             throw reply.getException();
//         } else {
//             return reply.getResult();
//         }   
//     }
// }
