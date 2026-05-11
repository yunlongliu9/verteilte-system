package vsue.rpc;

import java.net.ServerSocket;
import java.net.Socket;

import vsue.rmi.VSAuctionService;
import vsue.rmi.VSAuctionServiceImpl;

public class VSAuctionServer {
    public static void main(String[] args) {
        try {
            /*
             * 
             * create real service object
             * 
             */

            VSAuctionService auctionService =
                    new VSAuctionServiceImpl();
            /*
             * 
             * export object
             * 
             * +
             * 
             * bind into registry
             * 
             */
            VSRemoteObjectManager
                    .getInstance()
                    .exportObject(
                            "auctionService",
                            auctionService
                    );
            System.out.println(
                    "Auction service exported."
            );
            /*
             * 
             * start RPC server
             * 
             */
            VSServer server =
                    new VSServer(12345);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}