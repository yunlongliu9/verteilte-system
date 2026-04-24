package vsue.myrmi;

import java.net.Socket;
import vsue.rmi.VSAuction;

public class VSClient {
    public static void main(String[] args) throws Exception {
        

        // 1. 建立 socket 连接

        Socket socket = new Socket("localhost", 12345);

        // 2. 包装连接

        VSConnection conn = new VSConnection(socket);

        VSObjectConnection objConn = new VSObjectConnection(conn);

        // 3. 创建 proxy（代替 RMI stub）

        VSAuctionServiceProxy service = new VSAuctionServiceProxy(objConn);

        // 4. 测试调用
        service.registerAuction(new VSAuction("iphone", 50), 60, null);
        service.registerAuction(new VSAuction("pen", 2), 60, null);

        System.out.println("Calling getAuctions...");

        VSAuction[] auctions = service.getAuctions();

        if (auctions != null) {
            for (VSAuction a : auctions) {
                System.out.println(a.getName() + " : " + a.getPrice());
            }
        } else {
            System.out.println("No auctions");
        }

        // 5. 测试 bid（你也可以换成 register）
        boolean success1 = service.placeBid("user1", "pen", 100, null);
        boolean success2 = service.placeBid("user1", "iphone", 100, null);
        System.out.println("Bid result: " + success1 + ", " + success2);
    }
}
