package vsue.rmi;

public class TestHandler implements VSAuctionEventHandler {

    private String name;

    public TestHandler(String name) {
        this.name = name;
    }

    @Override
    public void handleEvent(VSAuctionEventType event, VSAuction auction) {
        if (event == VSAuctionEventType.AUCTION_END) {
            System.out.println(name + " received AUCTION_END for " + auction.getName() + " with final price " + auction.getPrice());
        } else if (event == VSAuctionEventType.HIGHER_BID) {
            System.out.println(name + " received HIGHER_BID for " + auction.getName() + " with new price " + auction.getPrice());
        } else if (event == VSAuctionEventType.AUCTION_WON) {
            System.out.println(name + " received AUCTION_WON for " + auction.getName() + " with winning price " + auction.getPrice());
        }
    }
    
}
