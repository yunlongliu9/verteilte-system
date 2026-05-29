package vsue.rpc;

import vsue.rmi.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.Remote;

public class VSAuctionServer extends VSAuctionRMIServer {
    
    private static final int RPC_PORT = 1111; //Standardport für den VSRemoteObjectManager
    
    public static void main(String[] args) {
        int rpcPort = RPC_PORT;
        int registryPort = 11111;
        boolean reuseConnections = false;
        
        if (args.length > 0) {
            rpcPort = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            registryPort = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            reuseConnections = Boolean.parseBoolean(args[2]);
        }
        
        try {
            // VSAuctionServiceImpl erzeugen
            VSAuctionServiceImpl auctionService = new VSAuctionServiceImpl();
            
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            final int finalRpcPort = rpcPort;
            
            // VSServer in eigenem Thread starten und system out weiterleiten
            new Thread(() -> {
                try {
                    VSServer server = new VSServer(finalRpcPort);
                    server.start();
                } catch (Exception e) {
                    System.err.println("[VSAuctionServer] Server error: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();


            
            // Remote-Objekt mit VSRemoteObjectManager exportieren
            Remote vsproxy = VSRemoteObjectManager.getInstance(hostname, rpcPort).exportObject(auctionService, reuseConnections);

            // Eigene Klassen erlauben
            System.setProperty("sun.rmi.registry.registryFilter", "vsue.**");
            // Registry erstellen
            Registry registry = LocateRegistry.createRegistry(registryPort);
            registry.bind("VSAuctionService", vsproxy);
            
            System.out.println("[VSAuctionServer] VSAuctionServer gestartet:");
            System.out.println("[VSAuctionServer] RPC-Port: " + rpcPort);
            System.out.println("[VSAuctionServer] Registry-Port: " + registryPort);
            System.out.println("[VSAuctionServer] Wiederverwendete Verbindungen: " + reuseConnections);
            System.out.println("[VSAuctionServer] Service VSAuctionService registriert an " + hostname + ":" + registryPort);
            
            Thread.sleep(Long.MAX_VALUE);
            
        } catch (RemoteException e) {
            System.err.println("[VSAuctionServer] RemoteException: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[VSAuctionServer] Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
