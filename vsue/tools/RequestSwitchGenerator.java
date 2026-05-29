package vsue.tools;

public class RequestSwitchGenerator
{
	// Codegenerator für vsue.rpc.Request
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

		// writeExternal()
		System.out.print("\t\tswitch (spaceForParameterLength) {\n");
		currentCase = 0;
		for (int params = 0; params < 5; params++) {
			System.out.print("\t\tcase " + sizes[params] + ":\n");
			System.out.print("\t\t\tswitch (spaceForHash) {\n");
			for (int hash = 1; hash < 5; hash++) {
				System.out.print("\t\t\tcase " + sizes[hash] + ":\n");
				System.out.print("\t\t\t\tswitch (spaceForID) {\n");
				for (int id = 1; id < 5; id++) {
					System.out.print("\t\t\t\tcase " + sizes[id] + ":\n");
					System.out.print("\t\t\t\t\treturn " + (byte) currentCase + ";\n");
					currentCase++;
				}
				System.out.print("\t\t\t\t}\n");
			}
			System.out.print("\t\t\t}\n");
		}
		System.out.print("\t\t}\n");

		System.out.print("\n----------------------------------\n\n");

		// readExternal()
		System.out.print("\t\tswitch (header) {\n");
		currentCase = 0;
		for (int params = 0; params < 5; params++) {
			for (int hash = 1; hash < 5; hash++) {
				for (int id = 1; id < 5; id++) {
					System.out.print(
						"\t\tcase " + (byte) currentCase + ":\n"
						+ "\t\t\tspaceForParameterLength = " + sizes[params] + ";\n"
						+ "\t\t\tspaceForHash = " + sizes[hash] + ";\n"
						+ "\t\t\tspaceForID = " + sizes[id] + ";\n"
						+ "\t\t\tbreak;\n"
					);
					currentCase++;
				}
			}
		}
		System.out.print("\t\t}\n");
	}
}