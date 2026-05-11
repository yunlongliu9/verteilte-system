package vsue.rpc;

import java.net.ServerSocket;
import java.net.Socket;

import vsue.myrmi.VSConnection;
import vsue.myrmi.VSObjectConnection;
import vsue.rmi.VSAuctionService;
import vsue.rmi.VSAuctionServiceImpl;

public class VSServer {
    private int port;
    public VSServer(int port) {
        this.port = port;
    }

    private void handleRequest(Socket clientSocket) {
        Object result = null;
        Throwable throwable = null;
        VSObjectConnection objectConnection = null;
        try {
            objectConnection = new VSObjectConnection(new VSConnection(clientSocket));
            RequestMessage request = (RequestMessage) objectConnection.receiveObject();
            result = VSRemoteObjectManager.getInstance()
                    .invokeMethod(
                            request.getObjectId(),
                            request.getMethodName(),
                            request.getParameters());
            // proxy, Method method, Object[] args
        } catch (Exception e) {
            throwable = e;
            System.err.println("⚠️  Exception occurred while handling request: " + e.getMessage());
        } finally {
            if (objectConnection != null){
                try {
                    objectConnection.sendObject(new ReplyMessage(result, throwable));
                    objectConnection.close();
                } catch (Exception e) {
                    System.err.println("⚠️  Failed to close object connection: " + e.getMessage());
                }
            }else{
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    System.err.println("⚠️  Failed to close client socket: " + e.getMessage());
                }
            }
        }
    }

    public void start() {
        ServerSocket serverSocket = null;
        try  {
            serverSocket = new ServerSocket(port);
            System.out.println("VSServer is running on port " + port);
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
