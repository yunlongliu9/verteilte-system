package vsue;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import vsue.rmi.VSAuctionEventHandler;
import vsue.rmi.VSAuctionServiceImpl;

public class VSAuctionRMIServer {   
    public static void main(String[] args) {
        try {
            VSAuctionServiceImpl auctionService = new VSAuctionServiceImpl();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("VSAuctionService", auctionService);
            System.out.println("VSAuctionService is running...");
        } catch (Exception e) {
            e.printStackTrace();    
        }
    }
}
