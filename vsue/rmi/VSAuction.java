package vsue.rmi;

import java.io.Externalizable;
public class VSAuction implements Externalizable
{
	private String name;

	/* The currently highest bid for this auction. */
	private int price;

	// Für Externalizable
	public VSAuction() { }

	public VSAuction(String name, int startingPrice) {
		this.name = name;
		this.price = startingPrice;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public String getName() {
		return name;
	}

	public int getPrice() {
		return price;
	}

    public boolean equals(Object o) {
        VSAuction o2;
        if (o instanceof VSAuction) {
            o2 = (VSAuction) o;
            if (name.equals(o2.getName())) {
                // Das wäre ein Synchronisationsfehler.
                // Keine Ahnung, ob das in unserer momentanen Implementierung
                // vorkommen kann.
                assert price == o2.getPrice();
                return true;
            }
        }
        return false;
    }

	public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException {
		out.writeInt(price);
		out.writeUTF(name);
	}

	public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
		price = in.readInt();
		name = in.readUTF();
	}
}
