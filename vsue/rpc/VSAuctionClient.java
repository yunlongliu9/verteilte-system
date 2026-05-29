package vsue.rpc;

import java.net.*;
import vsue.rmi.*;

public class VSAuctionClient extends VSAuctionRMIClient {

    public VSAuctionClient(String userName) {
        super(userName);
    }

    public static void main(String[] args) {
        checkArguments(args);
        VSAuctionClient client = new VSAuctionClient(args[0]);
        client.createCallbackServer(args[0]);
        String registryHost = args[1];
        int registryPort = Integer.parseInt(args[2]);
        client.init(registryHost, registryPort);
        client.shell();
        client.shutdown();
    }

    private static void checkArguments(String[] args) {
        if (args.length < 3) {
            System.err.println(
                    "parameters: <user-name> <registry_host> <registry_port>");
            System.exit(1);
        }
    }

    private void createCallbackServer(String username)  {
        VSClientCallbackServer callbackServer = null;
        try{
            callbackServer = new VSClientCallbackServer(
                InetAddress.getLocalHost().getHostAddress(),
                this,
                username
            );
        }catch(UnknownHostException e){
            throw new RuntimeException("Failed to get local host address", e);
        }
        
        callbackServer.start();
    }
}