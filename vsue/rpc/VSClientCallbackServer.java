package vsue.rpc;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.RemoteException;

import vsue.communication.*;
import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionEventHandler;
import vsue.rmi.VSAuctionEventType;

public class VSClientCallbackServer {
    private int callbackPort;
    private String host;
    private VSRemoteObjectManager manager;
    private ServerSocket serverSocket;
    private Remote localHandler;//handler object 
    private Remote remoteStub;// handler ref for server to call
    public VSClientCallbackServer(
            String host,
            Remote handler,
            String username
    ) {
        try{
            this.serverSocket = new ServerSocket(0);
            this.callbackPort = serverSocket.getLocalPort();
            this.host = host;
            this.localHandler = handler;
            this.manager = VSRemoteObjectManager.getInstance(host, callbackPort);
            this.manager.username = username;
            // export local callback object
            this.remoteStub = this.manager.exportObject(localHandler);
        }catch (IOException e){
            throw new RuntimeException("Failed to start callback server", e);       
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to export callback handler", e);
        }
    }
    // start callback listener
    public void start() {
        Thread callbackThread = new Thread(() -> {
                System.out.println(
                        "Client callback server started on port "
                                + callbackPort
                );
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        new Thread(()->{
                            handleCallback(socket);
                        }).start();;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
           
        });
        callbackThread.setDaemon(true);
        callbackThread.start();
    }


    private void handleCallback(Socket socket) {
        VSObjectConnection objectConnection = null;
        try {
            objectConnection = new VSObjectConnection(socket);
            while (true) {
                try{
                    Request request =(Request)objectConnection.receiveObject();
                    manager.invokeMethod(
                        request.getObjectId(),
                        request.getMethodName(),
                        request.getParameters()
                    );   
                }catch(RemoteException e){
                    System.out.println("RemoteException in callback handler: " + e.getMessage());
                    break;
                }catch (VSConnectionEndOfFile e) {
                    break;
                }catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}