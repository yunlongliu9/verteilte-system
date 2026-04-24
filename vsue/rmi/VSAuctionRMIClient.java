package vsue.rmi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;


public class VSAuctionRMIClient extends VSShell implements VSAuctionEventHandler {

	// The user name provided via command line.
	private final String userName;
	private VSAuctionService service;


	public VSAuctionRMIClient(String userName) {
		this.userName = userName;
	}


	// #############################
	// # INITIALIZATION & SHUTDOWN #
	// #############################

	public void init(String registryHost, int registryPort) {
		/*
		 * TODO: Implement client startup code
		 */
		try{
			// 1. 连接 registry
			Registry registry = LocateRegistry.getRegistry("localhost", 1099);
			// 2. 获取远程对象
			this.service = (VSAuctionService) registry.lookup("VSAuctionService");
			 // 3. 导出 callback（非常关键！！！）
        	VSAuctionEventHandler stub = (VSAuctionEventHandler) UnicastRemoteObject.exportObject(this, 0);
        	System.out.println("Client initialized");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {
		/*
		 * TODO: Implement client shutdown code
		 */
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}


	// #################
	// # EVENT HANDLER #
	// #################

	@Override
	public void handleEvent(VSAuctionEventType event, VSAuction auction) {
		/*
		 * TODO: Implement event handler
		 */
		switch (event) {
			case AUCTION_END:
				System.out.println("Auction ended: " + auction.getName() + " with final price " + auction.getPrice());
				break;
			case HIGHER_BID:
				System.out.println("Higher bid on " + auction.getName() + ": new price " + auction.getPrice());
				break;
			case AUCTION_WON:
				System.out.println("You won the auction for " + auction.getName() + " with price " + auction.getPrice());
				break;
		}
	}


	// ##################
	// # CLIENT METHODS #
	// ##################

	public void register(String auctionName, int duration, int startingPrice) {
		/*
		 * TODO: Register auction
		 */
		try {
			VSAuction auction = new VSAuction(auctionName, startingPrice);
			service.registerAuction(auction, duration, this);
			System.out.println("Auction registered: " + auctionName);
		} catch (VSAuctionException | RemoteException e) {
			System.out.println("⚠️  " + e.getMessage());
		}
	}

	public void list() {
		/*
		 * TODO: List all auctions that are currently in progress
		 */
		try {
			VSAuction[] auctions = service.getAuctions();
			if (auctions == null || auctions.length == 0) {
				System.out.println("No auctions currently in progress.");
				return;
			}
			for (VSAuction auction : auctions) {
				System.out.println("- " + auction.getName());
			}
		} catch (RemoteException e) {
			System.out.println("⚠️  " + e.getMessage());
		}
	}

	public void bid(String auctionName, int price) {
		/*
		 * TODO: Place a new bid
		 */
		try {
			boolean success = service.placeBid(userName, auctionName, price, this);
			if (success) {
				System.out.println("Bid placed successfully on " + auctionName + " with price " + price);
			} else {
				System.out.println("Failed to place bid on " + auctionName + " with price " + price);
			}
		} catch (VSAuctionException | RemoteException e) {
			System.out.println("⚠️  " + e.getMessage());
		}
	}


	// #########
	// # SHELL #
	// #########

	protected boolean processCommand(String[] args) {
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

	public static void main(String[] args) {
		checkArguments(args);
		createAndExecuteClient(args);
	}

	private static void checkArguments(String[] args) {
		if (args.length < 3) {
			System.err.println("usage: java " + VSAuctionRMIClient.class.getName() + " <user-name> <registry_host> <registry_port>");
			System.exit(1);
		}
	}

	private static void createAndExecuteClient(String[] args) {
		String userName = args[0];
		VSAuctionRMIClient client = new VSAuctionRMIClient(userName);

		String registryHost = args[1];
		int registryPort = Integer.parseInt(args[2]);
		client.init(registryHost, registryPort);
		client.shell();
		client.shutdown();
	}
}
