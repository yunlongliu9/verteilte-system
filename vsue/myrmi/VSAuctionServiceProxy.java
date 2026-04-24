package vsue.myrmi;

import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionEventHandler;
import vsue.rmi.VSAuctionException;
import vsue.rmi.VSAuctionService;

//自己实现的 RMI stub
public class VSAuctionServiceProxy implements VSAuctionService {

    private VSObjectConnection conn;

    public VSAuctionServiceProxy(VSObjectConnection conn) {
        this.conn = conn;
    }

    public VSAuction[] getAuctions()  {
        try {
            Request req = new Request();
            req.methodName = "getAuctions";
            req.paramTypes = new Class<?>[0];
            req.args = new Object[0];

            conn.sendObject(req);

            Response resp = (Response) conn.receiveObject();

            if (resp.exception != null) {
                throw (VSAuctionException) resp.exception;
            }

            return (VSAuction[]) resp.result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerAuction(VSAuction auction, int duration, VSAuctionEventHandler handler) throws VSAuctionException {
        try {
            Request req = new Request();
            req.methodName = "registerAuction";
            req.paramTypes = new Class<?>[] { VSAuction.class, int.class, VSAuctionEventHandler.class };
            req.args = new Object[] { auction, duration, handler };

//            System.out.println("Client sending request...");

            conn.sendObject(req);

            //System.out.println("Client waiting response...");

            Response resp = (Response) conn.receiveObject();

            
            if (resp.exception != null) {
                throw (VSAuctionException) resp.exception;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean placeBid(String userName, String auctionName, int price,VSAuctionEventHandler handler)throws VSAuctionException {
        try {
            Request req = new Request();
            req.methodName = "placeBid";
            req.paramTypes = new Class<?>[] {
                    String.class, String.class, int.class, VSAuctionEventHandler.class
            };
            req.args = new Object[] {
                    userName, auctionName, price, null
            };
            conn.sendObject(req);
            Response resp = (Response) conn.receiveObject();
            if (resp.exception != null) {

                throw (VSAuctionException) resp.exception;

            }

            return (Boolean) resp.result;

        } catch (Exception e) {

            throw new RuntimeException(e);

        }

    }

}