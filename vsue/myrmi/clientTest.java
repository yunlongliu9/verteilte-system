package vsue.myrmi;

import java.net.Socket;

public class clientTest {
    public static void main(String[] args) throws Exception {
        Socket s = new Socket("localhost", 12345);
        VSConnection conn = new VSConnection(s);
        conn.sendChunk("hello".getBytes());
        byte[] resp = conn.receiveChunk();
        System.out.println("Received bytes from server: " + new String(resp));

        Object obj = new String("hello object");
        VSObjectConnection objConn = new VSObjectConnection(conn);
        objConn.sendObject(obj);
        Object respObj = objConn.receiveObject();
        System.out.println("Received object from server: " + respObj);
        s.close();
    }
}
