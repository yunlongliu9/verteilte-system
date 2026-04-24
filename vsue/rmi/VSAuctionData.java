package vsue.rmi;

public class VSAuctionData {

    private VSAuction auction;
    private VSAuctionEventHandler creatorHandler;//creator handler
    private VSAuctionEventHandler winnerHandler;//winner handler

    public VSAuctionData(VSAuction auction, VSAuctionEventHandler creatorHandler) {
        this.auction = auction;
        this.creatorHandler = creatorHandler;
    }
       
    public VSAuctionData(VSAuction auction, VSAuctionEventHandler creatorHandler, VSAuctionEventHandler winnerHandler) {
        this.auction = auction;
        this.creatorHandler = creatorHandler;
        this.winnerHandler = winnerHandler;
    }

    public VSAuctionEventHandler getWinnerHandler() {
        return winnerHandler;
    }

    public VSAuctionEventHandler getCreatorHandler() {
        return creatorHandler;
    }

    public String getName() {
        return auction.getName();
    }

    public VSAuction getAuction() {
        return auction;
    }

    public void setPrice(int price) {
        this.auction.setPrice(price);
    }

    public int getPrice() {
        return this.auction.getPrice();
    }

    public void setWinnerHandler(VSAuctionEventHandler winnerHandler) {
        this.winnerHandler = winnerHandler;
    }

    public void setCreatorHandler(VSAuctionEventHandler creatorHandler) {
        this.creatorHandler = creatorHandler;
    }

}
