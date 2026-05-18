package vsue.faults;

import java.net.ServerSocket;
import java.net.Socket;

import vsue.rpc.ReplyMessage;
import vsue.myrmi.VSConnection;
import vsue.myrmi.VSObjectConnection;
import vsue.rmi.VSAuctionService;
import vsue.rmi.VSAuctionServiceImpl;



public class VSServer {
    /**
     * basic communication server for RPC
     * start a server socket, listen for incoming connections, and handle requests
     * **/

    private int port;
    public VSServer(int port) {
        this.port = port;
    }

    private void handleRequest(Socket clientSocket) {
        VSObjectConnection objectConnection = null;
        try {
            objectConnection =
                    new VSObjectConnection(
                            new VSConnection(clientSocket)
                    );
            /*
             *
             * 
             * 
             * keep connection alive
             *
             * 
             * 
             */
            while (true) {
                try {
                    RequestMessage request =(RequestMessage)objectConnection.receiveObject();
                    Object result =
                            VSRemoteObjectManager
                                    .getInstance()
                                    .invokeMethod(
                                            request.getObjectId(),
                                            request.getMethodName(),
                                            request.getParameters(),
                                            request.getRequestID()
                                    );
                    objectConnection.sendObject(
                            new ReplyMessage(
                                    result,
                                    null
                            )
                    );
                } catch (java.io.EOFException eof) {
                    /*
                     *
                     * 
                     * 
                     * client disconnected normally
                     *
                     * 
                     * 
                     */
                    break;
                } catch (Exception e) {
                    System.err.println(
                            "⚠️ RPC error: "
                                    + e.getMessage()
                    );
                    objectConnection.sendObject(
                            new ReplyMessage(
                                    null,
                                    e
                            )
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (objectConnection != null) {
                    objectConnection.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void start() {
        ServerSocket serverSocket = null;
        try  {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is running on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> {
                    handleRequest(clientSocket);
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
