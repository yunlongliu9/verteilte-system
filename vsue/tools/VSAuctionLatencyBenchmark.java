package vsue.tools;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Locale;

import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionService;

///// USAGE /////
/// JUST USE THE start_benchmark.sh SCRIPT TO RUN THE BENCHMARK
// below you can find instructions on how to run the benchmark manually


/////// MANUAL USAGE (WITHOUT THE SCRIPT) //////
// git pull
// ./build.sh

////// Server
// ssh <idm>@cipterm0.cip.cs.fau.de // (oder jeglicher anderer cip, whatever)
// RMI: java -cp bin vsue.rmi.VSAuctionRMIServer
// RPC: java -cp bin vsue.rpc.VSAuctionServer 1111 11111 true

////// Client
// ssh <idm>@cip1e3.cip.cs.fau.de
// RMI java -cp bin vsue.tools.VSAuctionLatencyBenchmark rmi cipterm0 11111 2000 100 1000 >> output.csv
// RPC java -cp bin vsue.tools.VSAuctionLatencyBenchmark rpc cipterm0 11111 2000 100 1000 >> output.csv

// Für plots: Doppelte headerzeile (mode,auctionCount,callDurationMicros) entfernen (wegen append), 
// -> im CSV einfach "mode" suchen und das in der mitte entfernen
// python environment erstellen und matplotlib + pandas installieren
// python compare_rpc_rmi.py

public class VSAuctionLatencyBenchmark {

	private static final String SERVICE_NAME = "VSAuctionService";

	public static void main(String[] args) throws Exception {
		if (args.length < 6) {
			System.err.println("Usage: <mode> <registry-host> <registry-port> <max-auctions> <step> <samples> [auction-duration-seconds]");
			System.err.println("  mode: rpc | rmi");
			System.exit(1);
		}

		// Mode kann z.B. "rpc" oder "rmi" sein, um die Ergebnisse später besser zuordnen zu können
		String mode = args[0].toLowerCase(Locale.ROOT);
		// Host und Port der RMI-Registry, in der der VSAuctionService registriert ist
		String registryHost = args[1];
		// Port der RMI-Registry, in der der VSAuctionService registriert ist
		int registryPort = Integer.parseInt(args[2]);
		// Maximale Anzahl von Auktionen, die erstellt werden sollen (z.B. 1000)
		int maxAuctions = Integer.parseInt(args[3]);
		// Schrittweite, mit der die Anzahl der Auktionen erhöht wird (z.B. 100)
		int step = Integer.parseInt(args[4]);
		// Anzahl der Aufrufe von getAuctions pro Sample (z.B. 100)
		int samples = Integer.parseInt(args[5]);
		int auctionDurationSeconds = (args.length > 6) ? Integer.parseInt(args[6]) : 3600;

		Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
		VSAuctionService service = (VSAuctionService) registry.lookup(SERVICE_NAME);

		// System.out.println("mode,auctionCount,callDurationMicros");

		int totalCreatedAuctions = 0;
		// getAuctions mit steigender Anzahl von Auktionen aufrufen und Dauer messen
		// Dies ist nützlich da der Aufruf mit höher werdender Anzahl von Auktionen vielleicht länger dauert
		for (int targetCount = 0; targetCount <= maxAuctions; targetCount += step) {
			totalCreatedAuctions = createAuctions(service, totalCreatedAuctions, targetCount, auctionDurationSeconds, mode);
			measureAndPrint(service, mode, targetCount, samples);
		}
	}

	private static int createAuctions(VSAuctionService service, int createdAuctions, int targetCount,
											 int auctionDurationSeconds, String prefix) throws Exception {
		while (createdAuctions < targetCount) {
			String auctionName = prefix + "-" + createdAuctions;
			VSAuction auction = new VSAuction(auctionName, createdAuctions);
			service.registerAuction(auction, auctionDurationSeconds, null);
			createdAuctions++;
		}
		return createdAuctions;
	}

	private static void measureAndPrint(VSAuctionService service, String mode, int auctionCount, int samples) throws Exception {
		// Warm-up phase: Perform several calls to ensure the JIT compiler has optimized the code path,
		// and any network/serialization caches are initialized. This dramatically reduces outliers.
		int batchSize = Math.max(10, samples / 10);
		int warmupIterations = Math.max(20, samples / 50);
		for (int i = 0; i < warmupIterations; i++) {
			service.getAuctions();
		}

		// Suggest garbage collection before actual measurement to reduce GC pauses during the run
		System.gc();
		Thread.sleep(100);

		// Mehrere samples nehmen um später Durchschnittswerte bilden zu können
		for (int sample = 0; sample < (samples / batchSize); sample++) {
			/// Der eigentliche Testaufruf
			long start = System.nanoTime();
			for (int i = 0; i < batchSize; i++) { // Mehrere Aufrufe in einem Batch um die Messung genauer zu machen
				service.getAuctions();
				Thread.sleep(1); // Kurze Pause zwischen den Aufrufen, um extreme Werte zu vermeiden
			}
			long elapsed = System.nanoTime() - start;

			// Subtract 1 ms (1000 µs) von der durchschnittlichen Dauer pro Aufruf, um die künstliche Verzögerung zu kompensieren
			double micros = (elapsed / (batchSize * 1000.0)) - 1000.0;

			
			System.out.printf(Locale.ROOT, "%s,%d,%.3f%n", mode, auctionCount, micros);
		}
	}
}