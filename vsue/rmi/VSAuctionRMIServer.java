package vsue.rmi;

import vsue.rmi.VSAuctionServiceImpl;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class VSAuctionRMIServer {

    public static void main(String[] args) {
        // Catch RemoteException and InterruptedException
        try {
            // Create Auction Service
            VSAuctionServiceImpl auctionService = new VSAuctionServiceImpl();

            // Remote-Objekt bekannt machen
            Registry registry = LocateRegistry.createRegistry(11111);
            registry.bind("VSAuctionService", auctionService);

            // Print hostname:port
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            System.out.println("VSAuctionService bound to " + hostname + ":11111");

            // Prozess weiterlaufen lassen
            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
