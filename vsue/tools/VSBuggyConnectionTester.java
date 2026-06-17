package vsue.tools;
import vsue.communication.VSBuggyObjectConnection;
import vsue.rpc.VSServer;
import vsue.rmi.VSAuctionServiceImpl;
import vsue.rmi.VSAuctionService;
import vsue.rmi.VSAuction;
import vsue.rpc.VSRemoteObjectManager;
import java.rmi.Remote;

public class VSBuggyConnectionTester {
    // Testet LOM und AMO Semantiken mithilfe der VSBuggyConnection Klasse

    public static void main(String[] args) throws Exception {
        double dropRate = 0.20;
        double delayRate = 0.10;
        double samples = 100;
        if (args.length >= 1) dropRate = Double.parseDouble(args[0]);
        if (args.length >= 2) delayRate = Double.parseDouble(args[1]);
        if (args.length >= 3) samples = Double.parseDouble(args[2]);

        VSBuggyObjectConnection.dropProbability = dropRate;
        VSBuggyObjectConnection.delayProbability = delayRate;

        System.out.println("Starting Buggy Connection Tester...");
        System.out.println("Drop Probability: " + dropRate);
        System.out.println("Delay Probability: " + delayRate);

        int rpcPort = 2222;
        String hostname = "localhost";
        boolean reuseConnections = false;
        
        // Start server with buggy connections = true
        Thread serverThread = new Thread(() -> {
            VSServer server = new VSServer(rpcPort, true);
            server.start();
        });
        serverThread.setDaemon(true);
        serverThread.start();
        
        Thread.sleep(1000); // Wait for server to start

        // Setup server side service
        VSAuctionServiceImpl auctionService = new VSAuctionServiceImpl();
        VSRemoteObjectManager manager = VSRemoteObjectManager.getInstance(hostname, rpcPort, true);
        Remote proxy = manager.exportObject(auctionService, reuseConnections);
        
        // For local tests we can just use proxy casted to VSAuctionService
        // because we are client and server in the same JVM, wait...
        // VSRemoteObjectManager normally runs in Client OR Server.
        // It's a singleton, so if they are in the same JVM, they share the manager!
        // The invoke on the proxy will actually route through VSInvocationHandler.
        VSAuctionService serviceProxy = (VSAuctionService) proxy;

        System.out.println("\n--- Testing AMO (Register Auction) ---");
        int amoSuccess = 0;
        int amoFailed = 0;

        for (int i = 0; i < samples; i++) {
            System.out.println("AMO Call " + (i+1) + "/" + (int)samples);
            try {
                VSAuction auction = new VSAuction("Auction-" + i, 100);
                serviceProxy.registerAuction(auction, 3600, null);
                amoSuccess++;
            } catch (Exception e) {
                // Should fail sometimes due to drop
                e.printStackTrace();
                amoFailed++;
            }
        }

        int amoSent = VSBuggyObjectConnection.messagesSent;
        int amoDropped = VSBuggyObjectConnection.messagesDropped;

        // Verify AMO deduplication 
        VSAuction[] registeredAuctions = serviceProxy.getAuctions();
        int registeredCount = registeredAuctions != null ? registeredAuctions.length : 0;

        System.out.println("\n--- Testing LOM (Get Auctions) ---");
        VSBuggyObjectConnection.messagesSent = 0;
        VSBuggyObjectConnection.messagesDropped = 0;

        int lomSuccess = 0;
        int lomFailed = 0;

        for (int i = 0; i < samples; i++) {
            System.out.println("LOM Call " + (i+1) + "/" + (int)samples);
            try {
                VSAuction[] auctions = serviceProxy.getAuctions();
                if (auctions != null) lomSuccess++;
            } catch (Exception e) {
                e.printStackTrace();
                lomFailed++;
            }
        }
        
        int lomSent = VSBuggyObjectConnection.messagesSent;
        int lomDropped = VSBuggyObjectConnection.messagesDropped;

        System.out.println("\n--- Test Results ---");

        System.out.println("AMO Calls Total: " + samples);
        System.out.println("AMO Successful: " + amoSuccess);
        System.out.println("AMO Failed Completely: " + amoFailed);
        System.out.println("-------------------------------");
        System.out.println("LOM Calls Total: " + samples);
        System.out.println("LOM Successful: " + lomSuccess);
        System.out.println("LOM Failed Completely: " + lomFailed);
        
        System.out.println("\n--- AMO Deduplication Verification ---");
        System.out.println("Auctions registered on server: " + registeredCount);
        System.out.println("AMO successful calls reported by client: " + amoSuccess);
        if (registeredCount == amoSuccess) {
            System.out.println("PASS: Server executed each AMO call exactly once (no duplicates).");
        } else {
            System.out.println("FAIL: Mismatch — possible duplicate executions or lost results.");
        }

        System.out.println("\n--- Overall Connection Statistics ---");
        System.out.println("AMO Phase: Sent = " + amoSent + ", Dropped = " + amoDropped + ", Retries = " + Math.max(0, amoSent - samples));
        System.out.println("LOM Phase: Sent = " + lomSent + ", Dropped = " + lomDropped + ", Retries = " + Math.max(0, lomSent - samples));
        
        System.exit(0);
    }
}
