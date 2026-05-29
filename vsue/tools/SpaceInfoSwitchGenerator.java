package vsue.tools;

public class SpaceInfoSwitchGenerator
{
	// Codegenerator für vsue.rmi.VSTestMessage
	// (keine Lust, 180 Fälle per Hand zu schreiben :P)

	// Kodierung:
	// Das erste Byte, das über die Leitung geschickt wird, dient als Header.
	// Darin sind die Größen von Integer, Stringlänge und Objektarraylänge
	// kodiert.
	// Die Größe kann jeweils ZERO_BYTES, ONE_BYTE, TWO_BYTES, THREE_BYTES,
	// oder FOUR_BYTES sein. ZERO_BYTES entspricht einer 0, einem leeren
	// String, bzw. einem leeren Array. Die anderen Größen sollten ziemlich
	// selbsterklärend sein.
	// String und Array können außerdem die Größe NONE annehmen. Damit wird
	// null signalisiert.
	// Insgesamt hat man also 5 * 6 * 6 = 180 mögliche Fälle. Das passt
	// schön in ein Byte, ergibt allerdings recht viel Code für die
	// Fallunterscheidung, deshalb der Codegenerator.

	// Warum das Ganze:
	// Gegenüber der ursprünglichen Kodierung hat man jetzt nur noch ein
	// Byte für die Wertebereiche aller drei Parameter. Leere Parameter
	// erzeugen keine weiteren Bytes. Dabei wird zwischen Parametern der
	// Größe 0 und null als Parameter unterschieden.
	// Array und String unterstützen jetzt auch variable Längen. Außerdem
	// gibt es den Zwischenschritt mit THREE_BYTES.
	//
	// Man hätte auch drei Bits für String und Array (je 8 Werte) und zwei
	// für den Integer (4 Werte) reservieren können, um die dann einzeln zu
	// parsen.
	// Also so: 000|000|00
	//                   ^- Integer
	//                ^---- String
	//            ^-------- Array
	// Das wäre zwar einfacher, aber man hätte beim Integer keinen Platz mehr
	// für THREE_BYTES.

	public static final String[] sizes = {
		"ZERO_BYTES",
		"ONE_BYTE",
		"TWO_BYTES",
		"THREE_BYTES",
		"FOUR_BYTES",
		"NONE"
	};

	public static void main(String[] args)
	{
		int currentCase;

		// writeExternal() bzw. compressSpaceInfo()
		System.out.print("\t\tswitch (spaceForInteger) {\n");
		currentCase = 0;
		for (int integer = 0; integer < 5; integer++) {
			System.out.print("\t\tcase " + sizes[integer] + ":\n");
			System.out.print("\t\t\tswitch (spaceForStringLength) {\n");
			for (int string = 0; string < 6; string++) {
				System.out.print("\t\t\tcase " + sizes[string] + ":\n");
				System.out.print("\t\t\t\tswitch (spaceForObjectCount) {\n");
				for (int array = 0; array < 6; array++) {
					System.out.print("\t\t\t\tcase " + sizes[array] + ":\n");
					System.out.print("\t\t\t\t\treturn " + (byte) currentCase + ";\n");
					currentCase++;
				}
				System.out.print("\t\t\t\t}\n");
			}
			System.out.print("\t\t\t}\n");
		}
		System.out.print("\t\t}\n");

		System.out.print("\n----------------------------------\n\n");

		// readExternal() bzw. extractSpaceInfo()
		System.out.print("\t\tswitch (header) {\n");
		currentCase = 0;
		for (int integer = 0; integer < 5; integer++) {
			for (int string = 0; string < 6; string++) {
				for (int array = 0; array < 6; array++) {
					System.out.print(
						"\t\tcase " + (byte) currentCase + ":\n"
						+ "\t\t\tspaceForInteger = " + sizes[integer] + ";\n"
						+ "\t\t\tspaceForStringLength = " + sizes[string] + ";\n"
						+ "\t\t\tspaceForObjectCount = " + sizes[array] + ";\n"
						+ "\t\t\tbreak;\n"
					);
					currentCase++;
				}
			}
		}
		System.out.print("\t\t}\n");
	}
}