package vsue.faults;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import vsue.myrmi.VSConnection;

public class VSObjectConnection {
    private final VSConnection conn;

    public VSObjectConnection(String host, int port) throws IOException {
        this.conn = new VSConnection(new Socket(host, port));
    }
    public VSObjectConnection(Socket socket) throws IOException {
        this.conn = new VSConnection(socket);
    }
    public VSObjectConnection(VSConnection conn) {
        this.conn = conn;
    }

    public void sendObject(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();

        conn.sendChunk(bos.toByteArray());
    }

    public Object receiveObject() throws IOException, ClassNotFoundException {
        byte[] data = conn.receiveChunk();

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);

        return ois.readObject();
    }

    public void close() throws IOException {
        conn.close();
    }
}
