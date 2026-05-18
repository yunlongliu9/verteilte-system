package vsue.faults.tests;

import java.lang.reflect.Proxy;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import vsue.faults.VSBuggyObjectConnection;

import vsue.faults.VSInvocationHandler;
import vsue.faults.VSObjectConnection;
import vsue.rmi.VSAuctionService;

import vsue.rpc.VSClientCallbackServer;
import vsue.rpc.VSClientHandler;
import vsue.rmi.VSAuction;

public class TestLOM {
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
                10000);
        callbackServer.start();
        VSObjectConnection objConn = new VSBuggyObjectConnection(
                "localhost",
                12345,
                true, // delay
                true, // drop
                false // crash
        );
        System.out.println("===== LOM TEST =====");
        VSInvocationHandler handler = new VSInvocationHandler(
                ((VSInvocationHandler) Proxy.getInvocationHandler(service)).getRemoteReference(),
                objConn);
        service = (VSAuctionService) Proxy.newProxyInstance(
                VSAuctionService.class
                        .getClassLoader(),
                new Class<?>[] {
                        VSAuctionService.class
                },
                handler);
        System.out.println("register begin.");
        service.registerAuction(new VSAuction("Item 1", 100), 10, null);
        System.out.println("register done.");
        final int NUM_CALLS = 3;
        for (int i = 0; i < NUM_CALLS; i++) {
            service.getAuctions();
            service.placeBid("viende", "Item 1", i+500, null);
            Thread.sleep(4000);
            System.out.println("loop " + i + " finished.");
        }
        System.out.println("Benchmark done.");
    }
}