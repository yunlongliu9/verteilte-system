package vsue.rmi;

public class TestMain {

    public static void main(String[] args) throws Exception {

        VSAuctionService service = new VSAuctionServiceImpl();

        // 创建两个 handler（模拟两个用户）
        VSAuctionEventHandler user1 = new TestHandler("User1");
        VSAuctionEventHandler user2 = new TestHandler("User2");

        // 1️⃣ 创建 auction（3秒结束）
        VSAuction auction = new VSAuction("iphone", 100);
        service.registerAuction(auction, 3, user1);

        System.out.println("Auction registered.");

        // 2️⃣ 查看 auction
        Thread.sleep(500);
        System.out.println("Current auctions:");
        for (VSAuction a : service.getAuctions()) {
            System.out.println(a.getName() + " price=" + a.getPrice());
        }

        // 3️⃣ user2 出价（更高）
        Thread.sleep(500);
        boolean ok1 = service.placeBid("user2", "iphone", 150, user2);
        System.out.println("User2 bid result: " + ok1);

        // 4️⃣ user1 再出价（更高）
        Thread.sleep(500);
        boolean ok2 = service.placeBid("user1", "iphone", 200, user1);
        System.out.println("User1 bid result: " + ok2);

        // 5️⃣ 等待 auction 结束
        Thread.sleep(4000);

        System.out.println("Test finished.");
    }
}