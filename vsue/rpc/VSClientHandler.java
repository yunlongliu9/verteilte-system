package vsue.rpc;

import vsue.rmi.VSAuctionEventHandler;
import vsue.rmi.VSAuctionEventType;
import vsue.rmi.VSAuction;
import java.rmi.RemoteException;

public class VSClientHandler implements VSAuctionEventHandler {
	public VSClientHandler() {
	}
    @Override
    public void handleEvent(VSAuctionEventType eventType, VSAuction auction) throws RemoteException {
        switch (eventType) {
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
    
}
