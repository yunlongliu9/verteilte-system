package vsue.rpc;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import vsue.rmi.VSAuctionService;
import vsue.rmi.VSAuction;

public class VSAuctionClient {
    public static void main(String[] args) {
       try{
        Registry registry =    LocateRegistry.getRegistry("localhost", 1099);
        VSAuctionService service = (VSAuctionService) registry.lookup("auctionService");
        service.registerAuction(new VSAuction("Item 1", 100), 100, null);
        service.registerAuction(new VSAuction("Item 2", 200), 100, null);
        service.registerAuction(new VSAuction("Item 3", 300), 100, null);
        VSAuction[] auctions = service.getAuctions();
        for (VSAuction a : auctions) {
            System.out.println(a.getName() + " : " + a.getPrice());
        }  
       }catch(Exception e){
        e.printStackTrace();
       } 

    }
}
