package vsue.faults;

import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.Remote;
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
             Registry registry = LocateRegistry.createRegistry(1099);
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
            Remote stub = VSRemoteObjectManager
                    .getInstance()
                    .exportObject(
                            auctionService,
                            12345
                    );
                   registry.bind("auctionService", stub);
            System.out.println(
                    "Auction service exported."
            );
            /*
             * 
             * start RPC server
             * 
             */
            VSServer server = new VSServer(12345);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            System.out.println("Server setup complete.");
        }
    }
}