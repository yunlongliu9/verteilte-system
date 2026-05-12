package vsue.rpc;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import vsue.rmi.VSAuctionService;
import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionEventHandler;

public class VSAuctionClient {
    public static void main(String[] args) {
       try{
        Registry registry =    LocateRegistry.getRegistry("localhost", 1099);
        VSAuctionService service = (VSAuctionService) registry.lookup("auctionService");

        final int CALLBACK_PORT = 10000;
        VSClientHandler handler = new VSClientHandler();
        //make client handler for callback
        VSClientCallbackServer callbackServer =new VSClientCallbackServer(
                        handler,
                        CALLBACK_PORT
                );
        callbackServer.start();

        service.registerAuction(new VSAuction("Item 1", 100), 10, callbackServer.getStub());
        service.registerAuction(new VSAuction("Item 2", 200), 10, callbackServer.getStub());
        service.registerAuction(new VSAuction("Item 3", 300), 10, callbackServer.getStub());
        /*
         *
         * 
         * 
         * warmup
         *
         * 
         * 
         */

        for (int i = 0; i < 100; i++) {

            service.getAuctions();

        }
        System.out.println("Warmup done.");
        /*
         *
         * 
         * 
         * benchmark
         *
         * 
         * 
         */

        final int NUM_CALLS = 10000;

        long start =

                System.nanoTime();

        for (int i = 0; i < NUM_CALLS; i++) {

            service.getAuctions();

        }

        long end =

                System.nanoTime();

        double totalMs =

                (end - start) / 1_000_000.0;

        double avgMs =

                totalMs / NUM_CALLS;

        System.out.println();

        System.out.println(

                "========= Short TCP Benchmark ========="

        );

        System.out.println(

                "Total calls: "

                        + NUM_CALLS

        );

        System.out.println(

                "Total time: "

                        + totalMs

                        + " ms"

        );

        System.out.println(

                "Average per call: "

                        + avgMs

                        + " ms"

        );

        System.out.println(

                "======================================="

        );

        /*
         *
         * 
         * 
         * verify result
         *
         * 
         * 
         */

        VSAuction[] auctions =

                service.getAuctions();

        for (VSAuction a : auctions) {

            System.out.println(

                    a.getName()

                            + " : "

                            + a.getPrice()

            );

        }
       }catch(Exception e){
        e.printStackTrace();
       } 

    }
}
