package vsue.rpc;

import java.io.Externalizable;

public class VSRemoteReference implements Externalizable
{
	private String host;
	private int port;
	private int objectID;

	// Für Externalizable
	public VSRemoteReference() { }
		
	public VSRemoteReference(String host, int port, int objectID)
	{
		this.host = host;
		this.port = port;
		this.objectID = objectID;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public int getObjectID() {
		return objectID;
	}

	@Override
	public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException {
		out.writeUTF(host);
		out.writeInt(port);
		out.writeInt(objectID);
	}

	@Override
	public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
		this.host = in.readUTF();
		this.port = in.readInt();
		this.objectID = in.readInt();
	}
}