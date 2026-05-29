package vsue.tools;

public class VSAuctionSwitchGenerator
{
	// Codegenerator für vsue.rmi.VSAuction
	// siehe SpaceInfoSwitchGenerator.java

	public static final String[] sizes = {
		"ZERO_BYTES",
		"ONE_BYTE",
		"TWO_BYTES",
		"THREE_BYTES",
		"FOUR_BYTES"
	};

	public static void main(String[] args)
	{
		int currentCase;

		// writeExternal() bzw. compressSpaceInfo()
		System.out.print("\t\tswitch (spaceForPrice) {\n");
		currentCase = 0;
		for (int price = 0; price < 5; price++) {
			System.out.print("\t\tcase " + sizes[price] + ":\n");
			System.out.print("\t\t\tswitch (spaceForNameLength) {\n");
			for (int name = 1; name < 5; name++) {
				System.out.print("\t\t\tcase " + sizes[name] + ":\n");
				System.out.print("\t\t\t\treturn " + (byte) currentCase + ";\n");
				currentCase++;
			}
			System.out.print("\t\t\t}\n");
		}
		System.out.print("\t\t}\n");

		System.out.print("\n----------------------------------\n\n");

		// readExternal() bzw. extractSpaceInfo()
		System.out.print("\t\tswitch (header) {\n");
		currentCase = 0;
		for (int price = 0; price < 5; price++) {
			for (int name = 1; name < 5; name++) {
				System.out.print(
					"\t\tcase " + (byte) currentCase + ":\n"
					+ "\t\t\tspaceForPrice = " + sizes[price] + ";\n"
					+ "\t\t\tspaceForNameLength = " + sizes[name] + ";\n"
					+ "\t\t\tbreak;\n"
				);
				currentCase++;
			}
		}
		System.out.print("\t\t}\n");
	}
}