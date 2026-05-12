package vsue.rpc;

import java.rmi.RemoteException;

import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionEventHandler;
import vsue.rmi.VSAuctionEventType;

public class VSClientCallbackServer  {
    private final int port;
    private final VSAuctionEventHandler localHandler;
    private final VSAuctionEventHandler remoteStub;
    private final VSServer server;
    public VSClientCallbackServer(VSAuctionEventHandler handler,int port) {
        this.port = port;
        this.localHandler = handler;
        /*
         * 
         * export callback object
         * 
         */
        try {

            this.remoteStub =

                    (VSAuctionEventHandler)

                    VSRemoteObjectManager

                            .getInstance()

                            .exportObject(

                                    handler,

                                    port

                            );

        } catch (Exception e) {

            throw new RuntimeException(e);

        }
        /*
         * 
         * local callback rpc server
         * 
         */
        this.server = new VSServer(port);
    }
    public void start() {
        new Thread(() -> {
            server.start();
        }).start();
    }
    /*
     * 
     * server side will use this
     * 
     */
    public VSAuctionEventHandler getStub() {
        return remoteStub;
    }

}
