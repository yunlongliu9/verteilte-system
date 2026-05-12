package vsue.rpc;

import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import vsue.rmi.VSAuctionService;
import vsue.myrmi.VSConnection;
import vsue.myrmi.VSObjectConnection;
import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionEventHandler;
import java.lang.reflect.Proxy;

public class VSAuctionClientLongtcp {
    public static void main(String[] args) {
        VSInvocationHandlerLongTCP handler = null;
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            VSAuctionService service = (VSAuctionService) registry.lookup("auctionService");

            final int CALLBACK_PORT = 10000;
            final int Server_PORT = 12345;
            VSObjectConnection objConn = new VSObjectConnection(new VSConnection(new Socket("localhost", Server_PORT)));
            handler = new VSInvocationHandlerLongTCP(((VSInvocationHandler)Proxy.getInvocationHandler(service)).getRemoteReference(), objConn);
            
            service = (VSAuctionService)Proxy.newProxyInstance(VSAuctionService.class.getClassLoader(),new Class<?>[] {VSAuctionService.class},handler);
            VSClientHandler callbackHandler = new VSClientHandler();
            VSClientCallbackServer callbackServer = new VSClientCallbackServer(
                    callbackHandler,
                    CALLBACK_PORT);
            callbackServer.start();

            service.registerAuction(new VSAuction("Item 1", 100), 10, callbackServer.getStub());
            service.registerAuction(new VSAuction("Item 2", 200), 10, callbackServer.getStub());
            service.registerAuction(new VSAuction("Item 3", 300), 10, callbackServer.getStub());
            
            //warm up
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

                    "========== Long TCP Benchmark =========="

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

                    "========================================"

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
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
                System.out.println("Client exiting...");
                handler.close();
        }

    }
}
