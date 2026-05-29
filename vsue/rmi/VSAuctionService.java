package vsue.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import vsue.faults.VSRPCSemantic;
import vsue.faults.VSRPCSemanticType;


public interface VSAuctionService extends Remote {

	/**
	 * Registers a new auction.
	 *
	 * @param auction The auction to register
	 *                duration  Time in seconds until the auction ends
	 *                handler   The handler to notify when the auction has ended
	 * @throws VSAuctionException if the duration is negative or an auction
	 *                            with the same name already exists
	 */
	@VSRPCSemantic(VSRPCSemanticType.AT_MOST_ONCE) // Auktion soll nur einmal registriert werden können
	public void registerAuction(VSAuction auction, int duration, VSAuctionEventHandler handler) throws VSAuctionException, RemoteException;

	/**
	 * Retrieves all auctions currently in progress.
	 *
	 * @return The auctions currently in progress.
	 */
	@VSRPCSemantic(VSRPCSemanticType.LAST_OF_MANY) // Neueste Version der Auktionen soll zurückgegeben werden
	public VSAuction[] getAuctions() throws RemoteException;

	/**
	 * Places a new bid.
	 *
	 * @param userName    The name of the bidder
	 * @param auctionName The name of the auction
	 * @param price       The bid
	 * @param handler     The handler to notify if another bid is placed
	 *                    that is higher than this bid
	 * @return <tt>true</tt> if the bid has been placed, otherwise <tt>false</tt>
	 * @throws VSAuctionException if no auction with the specified name is
	 *                            currently in progress
	 */
	@VSRPCSemantic(VSRPCSemanticType.AT_MOST_ONCE) // Ein Gebot soll nur einmal abgegeben werden können
	public boolean placeBid(String userName, String auctionName, int price, VSAuctionEventHandler handler) throws VSAuctionException, RemoteException;

}
