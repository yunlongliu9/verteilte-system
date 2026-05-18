package vsue.rpc;

import vsue.rmi.VSAuctionEventHandler;

public class VSClientCallbackServer {
    /*
     *
     * local callback RPC server
     *
     */
    private final int port;
    private final VSAuctionEventHandler localHandler;
    private final VSAuctionEventHandler remoteStub;
    /*
     *
     * embedded RPC server
     *
     */
    private final VSServer server;

    public VSClientCallbackServer(VSAuctionEventHandler handler,int port) {
        this.port = port;
        this.localHandler = handler;    
        try {
            this.remoteStub =(VSAuctionEventHandler)VSRemoteObjectManager.getInstance().exportObject(handler,port);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to export callback handler",e
            );
        }

        this.server = new VSServer(port);
    }

    /*
     *  callback listener
     */
    public void start() {
        Thread callbackThread =
                new Thread(() -> {
                    server.start();
                });
        callbackThread.setDaemon(true);
        callbackThread.start();
        System.out.println(
                "Client callback server started on port "
                        + port
        );
    }

    public VSAuctionEventHandler getStub() {
        return remoteStub;
    }
}