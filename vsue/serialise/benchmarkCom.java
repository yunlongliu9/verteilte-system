
package vsue.serialise;

import java.io.*;
import java.util.*;

public class benchmarkCom {

    public static void main(String[] args) throws Exception {

        List<ComplicatedVSAuctionNormal> list = createData(10000);

        int runs = 10;
        long totalTime = 0;
        int size = 0;

        for (int i = 0; i < runs; i++) {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            long start = System.nanoTime();

            oos.writeObject(list);
            oos.flush();

            long end = System.nanoTime();

            totalTime += (end - start);
            size = bos.toByteArray().length;
        }

        System.out.println("Serializable size: " + size);
        System.out.println("Serializable avg time: " + (totalTime / runs));
    }

    private static List<ComplicatedVSAuctionNormal> createData(int n) {
        List<ComplicatedVSAuctionNormal> list = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            List<String> bids = Arrays.asList("A", "B", "C");
            list.add(new ComplicatedVSAuctionNormal(
                    "item" + i,
                    i,
                    bids,
                    "description" + i,
                    System.currentTimeMillis(),
                    Math.random()
            ));
        }
        return list;
    }
}