package vsue.rmi;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import java.rmi.server.UnicastRemoteObject;
import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionEventHandler;
import vsue.rmi.VSAuctionException;
import vsue.rmi.VSAuctionService;
import vsue.faults.VSRPCSemantic;
import vsue.faults.VSRPCSemanticType;

public class VSAuctionServiceImpl extends UnicastRemoteObject implements VSAuctionService {

    private final Map<String, VSAuctionData> auctions = new HashMap<String, VSAuctionData>();

    public VSAuctionServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public void registerAuction(VSAuction auction, int duration, VSAuctionEventHandler handler)
            throws VSAuctionException {
        VSAuctionData auctionData = new VSAuctionData(auction, handler);
        synchronized (auctions) {
            if (auction.getName() == null || auction.getName().isEmpty()) {
                throw new VSAuctionException("Auction name cannot be null or empty.");
            }
            if (duration < 0) {
                throw new VSAuctionException("Duration cannot be negative.");
            }
            if (auctions.containsKey(auction.getName())) {
                throw new VSAuctionException("An auction with the same name already exists.");
            }
            if (auction.getPrice() < 0) {
                throw new VSAuctionException("Starting price cannot be negative.");
            }
            this.auctions.put(auction.getName(), auctionData);
        }

        new Thread(() -> {
            VSAuctionData ended = null;
            try {
                Thread.sleep(duration * 1000);
                synchronized (auctions) {
                    ended = auctions.remove(auctionData.getName());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (ended != null) {
                if (ended.getCreatorHandler() != null) {
                    try {
                        ended.getCreatorHandler().handleEvent(
                                VSAuctionEventType.AUCTION_END,
                                ended.getAuction());
                    } catch (RemoteException e) {
                        System.out.println("⚠️  Failed to notify client (disconnected)");
                        ended.setCreatorHandler(null);
                    }
                }
                if (ended.getWinnerHandler() != null) {
                    try {
                        ended.getWinnerHandler().handleEvent(
                                VSAuctionEventType.AUCTION_WON,
                                ended.getAuction());
                    } catch (RemoteException e) {
                        System.out.println("Client unreachable, removing handler");
                        ended.setWinnerHandler(null);
                    }
                }

            }
        }).start();
    }

    @Override
    public VSAuction[] getAuctions() {
        VSAuctionData[] dataArray;
        // Synchronize so kurz wie möglich, um array bald wieder freizugeben
        synchronized (auctions) {
            dataArray = auctions.values().toArray(new VSAuctionData[0]);
        }
        
        // For-loop statt Stream-API ist hier schneller
        VSAuction[] result = new VSAuction[dataArray.length];
        for (int i = 0; i < dataArray.length; i++) {
            result[i] = dataArray[i].getAuction();
        }
        return result;
    }

    @Override
    public boolean placeBid(String userName, String auctionName, int price, VSAuctionEventHandler handler)
            throws RemoteException, VSAuctionException {
        if (price <= 0) {
            throw new VSAuctionException("Bid price must be positive.");
        }
   
            VSAuctionData auctionData = null;// store the probable former winner's info
            VSAuctionData beforeWinnerData = null;
  
        synchronized (auctions) {
             auctionData = auctions.get(auctionName);// store the probable former winner's info
             beforeWinnerData = auctionData != null ? new VSAuctionData(
                    auctionData.getAuction(),
                    auctionData.getCreatorHandler(),
                    auctionData.getWinnerHandler()) : null;
            if (auctionData == null) {
                throw new VSAuctionException("No auction with the specified name is currently in progress.");
            }
            if (price > auctionData.getPrice()) {
                auctionData.setPrice(price);
                auctionData.setWinnerHandler(handler);
            } else {
                return false;
            }
        }
        if (beforeWinnerData != null && beforeWinnerData.getWinnerHandler() != null) {
            try {
                beforeWinnerData.getWinnerHandler().handleEvent(
                        VSAuctionEventType.HIGHER_BID,
                        auctionData.getAuction());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
