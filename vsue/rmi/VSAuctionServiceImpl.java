package vsue.rmi;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import java.rmi.server.UnicastRemoteObject;
import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionEventHandler;
import vsue.rmi.VSAuctionException;
import vsue.rmi.VSAuctionService;

public class VSAuctionServiceImpl extends UnicastRemoteObject implements VSAuctionService {

    private Map<String, VSAuctionData> auctions = new HashMap();// phone, auctiondata

    public VSAuctionServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public void registerAuction(VSAuction auction, int duration, VSAuctionEventHandler handler) throws VSAuctionException {
        // TODO Auto-generated method stub
        VSAuctionData auctionData = new VSAuctionData(auction, handler);
        synchronized (this) {
            if (auction.getName() == null || auction.getName().isEmpty()) {
                throw new VSAuctionException("Auction name cannot be null or empty.");
            }
            if (duration < 0) {
                throw new VSAuctionException("Duration cannot be negative.");
            }
            if (auctions.containsKey(auction.getName())) {
                throw new VSAuctionException("An auction with the same name already exists.");
            }  
            this.auctions.put(auction.getName(), auctionData);
        } 

        
        
    new Thread(() -> {
        VSAuctionData ended = null;
        try {
            Thread.sleep(duration * 1000);
            synchronized (this) {
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
                                ended.getAuction()
                            );
                        } catch (RemoteException e) {
                            System.out.println("⚠️  Failed to notify client (disconnected)");
                        }
                        }
                        if (ended.getWinnerHandler() != null) {
                            try{
                                ended.getWinnerHandler().handleEvent(
                                VSAuctionEventType.AUCTION_WON,
                                ended.getAuction()
                            );
                            } catch (RemoteException e) {
                                 System.out.println("Client unreachable, removing handler");
                            }
                        }

                    }
        }).start();
    }

    @Override
    public synchronized VSAuction[] getAuctions() {
        if (auctions.isEmpty()) {
            System.out.println("No auctions currently in progress.");
            return null;
        }else{
            return auctions.values().stream()
            .map(VSAuctionData::getAuction)
            .toArray(VSAuction[]::new);
        }
    }

    @Override
    public boolean placeBid(String userName, String auctionName, int price, VSAuctionEventHandler handler) throws RemoteException, VSAuctionException {
        // TODO Auto-generated method stub
        /**
         * 1. compare the price
         * **/
        if (price <= 0) {
            throw new VSAuctionException("Bid price must be positive.");
        }
        
        VSAuctionData auctionData = auctions.get(auctionName);// store the probable former winner's info
        VSAuctionData beforeWinnerData = auctionData != null ? new VSAuctionData(
            auctionData.getAuction(),
            auctionData.getCreatorHandler(),
            auctionData.getWinnerHandler()
        ) : null;
        synchronized (this) {
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
        if (beforeWinnerData != null && beforeWinnerData.getWinnerHandler() != null ) {
            try {
                beforeWinnerData.getWinnerHandler().handleEvent(
                    VSAuctionEventType.HIGHER_BID,
                    auctionData.getAuction()
                );
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}

