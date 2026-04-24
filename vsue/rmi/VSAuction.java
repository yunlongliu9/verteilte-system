package vsue.rmi;

import java.io.Serializable;

public class VSAuction implements Serializable {
	//unmodifiable; in case client safety is not guaranteed, these fields should be private and only readable

	/* The auction name. */
	private final String name;

	/* The  bid for this auction. */
	private int price;	


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

}
