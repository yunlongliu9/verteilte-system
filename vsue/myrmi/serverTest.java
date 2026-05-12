package vsue.myrmi;

import java.net.ServerSocket;
import java.net.Socket;

public class serverTest {
    public static void main(String[] args) throws Exception {
       ServerSocket ss = new ServerSocket(12345);
        Socket s = ss.accept();
        VSConnection conn = new VSConnection(s);
        VSObjectConnection objConn = new VSObjectConnection(conn);
        byte[] data = conn.receiveChunk();
        
        System.out.println(new String(data)+" received from client");
        conn.sendChunk(data); // echo

        Object obj = objConn.receiveObject();
        System.out.println(obj.toString()+" received as object from client");
        objConn.sendObject(obj); // echo object as bytes  
        

        s.close();
        ss.close();
    }
}
