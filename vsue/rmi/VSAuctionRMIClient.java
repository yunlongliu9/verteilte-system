package vsue.rmi;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;

public class VSAuctionRMIClient extends VSShell implements VSAuctionEventHandler
{
	private final String userName;
	private VSAuctionService auctionService;
	private VSAuctionEventHandler handler;

	public VSAuctionRMIClient(String userName)
	{
		this.userName = userName;
	}

	public String getUsername(){
		return userName;
	}

	// #############################
	// # INITIALIZATION & SHUTDOWN #
	// #############################

	public void init(String registryHost, int registryPort)
	{
		final String auctionServiceName = "VSAuctionService";
		// null, sonst meckert der Compiler unten, weil er meint, das könnte uninitialisiert benutzt werden
		Registry registry = null;
		try {
			// Aus der Dokumentation:
			// Note that a getRegistry call does not actually make a connection to the remote host.
			// It simply creates a local reference to the remote registry and will succeed even if
			// no registry is running on the remote host. Therefore, a subsequent method invocation
			// to a remote registry returned as a result of this method may fail.
			registry = LocateRegistry.getRegistry(registryHost, registryPort);
		} catch (RemoteException e) {
			System.err.println("Error: Could not create reference to Registry at " + registryHost + ":" + registryPort);
			System.exit(1);
		}
		try {
			auctionService = (VSAuctionService) registry.lookup(auctionServiceName);
		} catch (RemoteException e) {
			System.err.println("Error: remote communication with the registry failed");
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (NotBoundException e) {
			System.err.println("Error: VSAuctionService \"" + auctionServiceName + "\" not bound at " + registryHost + ":" + registryPort);
			System.exit(1);
		}
		try {
			handler = (VSAuctionEventHandler) UnicastRemoteObject.exportObject(this, 0);
		} catch (RemoteException e) {
			System.err.println("Error: failed to export client as VSAuctionEventHandler");
			System.exit(1);
		}
	}

	public void shutdown()
	{
		try {
			// Da wir den Client danach beenden, ist der Rückgabewert von unexportObject() egal.
			UnicastRemoteObject.unexportObject(handler, true);
		} catch (NoSuchObjectException e) {
			// ignore (wir wissen, dass handler existiert)
		}
	}

	// #################
	// # EVENT HANDLER #
	// #################

	@Override
	public void handleEvent(VSAuctionEventType event, VSAuction auction)
	throws RemoteException
	{
		switch (event) {
		case HIGHER_BID:
			System.out.println("[Higher Bid] Auction \"" + auction.getName() + "\" now at " + auction.getPrice());
			break;
		case AUCTION_END:
			System.out.println("[Auction End] Auction \"" + auction.getName() + "\" ended at " + auction.getPrice());
			break;
		case AUCTION_WON:
			System.out.println("[Auction Won] Won auction \"" + auction.getName() + "\" (" + auction.getPrice() + ")");
			break;
		}
	}

	// ##################
	// # CLIENT METHODS #
	// ##################

	public void register(String auctionName, int duration, int startingPrice)
	{
		VSAuction auction = new VSAuction(auctionName, startingPrice);
		try {
			auctionService.registerAuction(auction, duration, this);
		} catch (VSAuctionException e) {
			System.err.println("VSAuctionException: " + e.getMessage());
			return;
		} catch (RemoteException e) {
			System.err.println("RemoteException: " + e.getMessage());
			return;
		}
	}

	public void list()
	{
		VSAuction[] auctions;
		try {
			auctions = auctionService.getAuctions();
		} catch (RemoteException e) {
			System.err.println("RemoteException: " + e.getMessage());
			return;
		}
		if (auctions == null) {
			System.out.println("There are no active auctions right now");
		} else {
			for (VSAuction auction : auctions) {
				System.out.println(auction.getName() + ": " + auction.getPrice());
			}
		}
	}

	public void bid(String auctionName, int price)
	{
		boolean success;
		try {
			success = auctionService.placeBid(userName, auctionName, price, this);
		} catch (VSAuctionException e) {
			System.err.println("VSAuctionException: " + e.getMessage());
			return;
		} catch (RemoteException e) {
			System.err.println("RemoteException: " + e.getMessage());
			return;
		}
		if (success) {
			System.out.println("Bid has been placed");
		} else {
			System.out.println("Bid has not been placed");
		}
	}

	// #########
	// # SHELL #
	// #########

	protected boolean processCommand(String[] args)
	{
		switch (args[0]) {
		case "help":
		case "h":
			System.out.println("The following commands are available:\n"
					+ "  help\n"
					+ "  bid <auction-name> <price>\n"
					+ "  list\n"
					+ "  register <auction-name> <duration> [<starting-price>]\n"
					+ "  quit"
			);
			break;
		case "register":
		case "r":
			if (args.length < 3)
				throw new IllegalArgumentException("Usage: register <auction-name> <duration> [<starting-price>]");
			int duration = Integer.parseInt(args[2]);
			int startingPrice = (args.length > 3) ? Integer.parseInt(args[3]) : 0;
			register(args[1], duration, startingPrice);
			break;
		case "list":
		case "l":
			list();
			break;
		case "bid":
		case "b":
			if (args.length < 3) throw new IllegalArgumentException("Usage: bid <auction-name> <price>");
			int price = Integer.parseInt(args[2]);
			bid(args[1], price);
			break;
		case "exit":
		case "quit":
		case "x":
		case "q":
			return false;
		default:
			throw new IllegalArgumentException("Unknown command: " + args[0] + "\nUse \"help\" to list available commands");
		}
		return true;
	}

	// ########
	// # MAIN #
	// ########

	public static void main(String[] args)
	{
		checkArguments(args);
		createAndExecuteClient(args);
	}

	private static void checkArguments(String[] args)
	{
		if (args.length < 3) {
			System.err.println("parameters: <user-name> <registry_host> <registry_port>");
			System.exit(1);
		}
	}

	private static void createAndExecuteClient(String[] args)
	{
		String userName = args[0];
		VSAuctionRMIClient client = new VSAuctionRMIClient(userName);

		String registryHost = args[1];
		int registryPort = Integer.parseInt(args[2]);
		client.init(registryHost, registryPort);
		client.shell();
		client.shutdown();
	}
}
