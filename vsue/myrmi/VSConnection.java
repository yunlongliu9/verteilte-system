package vsue.myrmi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class VSConnection {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public VSConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public synchronized void sendChunk(byte[] chunk) throws IOException {
        out.writeInt(chunk.length);  // 先发长度
        out.write(chunk);            // 再发数据
        out.flush();
    }

    public synchronized byte[] receiveChunk() throws IOException {
        int len = in.readInt();      // 先读长度（阻塞直到有数据）
        byte[] data = new byte[len];
        in.readFully(data);          // 读满 len 字节
        return data;
    }

    public void close() throws IOException {
        socket.close();
    }
}