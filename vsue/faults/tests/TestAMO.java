package vsue.faults.tests;

import java.lang.reflect.Proxy;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import vsue.faults.VSBuggyObjectConnection;

import vsue.faults.VSInvocationHandler;
import vsue.faults.VSObjectConnection;
import vsue.rmi.VSAuctionService;
import vsue.faults.tests.VSInvocationHandlerLongTCP;
import vsue.rpc.VSClientCallbackServer;
import vsue.rpc.VSClientHandler;
import vsue.rmi.VSAuction;

import vsue.rpc.VSClientHandler;
import vsue.rpc.VSClientCallbackServer;

public class TestAMO {

    public static void main(String[] args)
            throws Exception {
                Registry registry = LocateRegistry.getRegistry(
                "localhost",
                1099);
                VSAuctionService service = (VSAuctionService) registry.lookup(
                "auctionService");
                VSClientHandler callbackHandler = new VSClientHandler();
                VSClientCallbackServer callbackServer = new VSClientCallbackServer(
                                callbackHandler,
                                10000
                        );
                callbackServer.start();
                VSObjectConnection objConn =
                new VSBuggyObjectConnection(
                        "localhost",
                        12345,
                        false, // delay
                        true,  // drop
                        true  // crash
                );
            System.out.println("===== AMO TEST =====");
            VSInvocationHandlerLongTCP handler = new VSInvocationHandlerLongTCP(
                    ((VSInvocationHandler) Proxy.getInvocationHandler(service)).getRemoteReference(),
                    objConn);
            service = (VSAuctionService) Proxy.newProxyInstance(
                    VSAuctionService.class
                            .getClassLoader(),
                    new Class<?>[] {
                            VSAuctionService.class
                    },
                    handler);
                    
            service.registerAuction(new VSAuction("Item 1", 100), 10, callbackServer.getStub());

            final int NUM_CALLS = 3;
            for (int i = 0; i < NUM_CALLS; i++) {
                service.placeBid("viende", "Item 1", i+500, callbackServer.getStub());
            }
            System.out.println("Benchmark done.");
    }
}