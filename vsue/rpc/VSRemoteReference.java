package vsue.rpc;

import java.io.Serializable;

public class VSRemoteReference implements Serializable {
    private String hostIP;
    private int port;
    private int objectId;

    public VSRemoteReference(String hostIP, int port, int objectId) {
        this.hostIP = hostIP;
        this.port = port;
        this.objectId = objectId;
    }

    public String getHostIP() {
        return hostIP;
    }

    public int getPort() {
        return port;
    }

    public int getObjectId() {
        return objectId;
    }

}
