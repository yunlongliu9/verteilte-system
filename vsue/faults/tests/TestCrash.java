package vsue.faults.tests;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import vsue.faults.VSBuggyObjectConnection;
import vsue.faults.VSInvocationHandler;
import vsue.faults.VSObjectConnection;

import vsue.rmi.VSAuctionService;
import vsue.faults.tests.VSInvocationHandlerLongTCP;
import java.lang.reflect.Proxy;
public class TestCrash {

    public static void main(String[] args)
            throws Exception {
        VSInvocationHandlerLongTCP handler = null;
        try {
            Registry registry =
                LocateRegistry.getRegistry(
                    "localhost",
                    1099
                );
            VSAuctionService service =
                (VSAuctionService)
                registry.lookup(
                    "auctionService"
                );
            VSObjectConnection objConn =
                new VSBuggyObjectConnection(
                    "localhost",
                    12345,
                    false, // delay
                    false, // drop
                    true   // crash
                );
            handler =
                new VSInvocationHandlerLongTCP(
                    ((VSInvocationHandler)Proxy.getInvocationHandler(service)).getRemoteReference(),
                    objConn
                );
            service =(VSAuctionService)Proxy.newProxyInstance(
                    VSAuctionService.class
                        .getClassLoader(),
                    new Class<?>[] {
                        VSAuctionService.class
                    },
                    handler
                );
            System.out.println(
                "Calling getAuctions..."
            );
            service.getAuctions();
            System.out.println(
                "Call succeeded"
            );
        } catch (Exception e) {
            System.out.println(
                "Crash test triggered:"
            );
            e.printStackTrace();
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }
}