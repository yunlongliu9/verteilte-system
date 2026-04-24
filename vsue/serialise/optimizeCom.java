package vsue.serialise;

import java.io.*;
import java.util.*;

public class optimizeCom {

    public static void main(String[] args) throws Exception {

        List<ComplicatedVSAuction> list = createData(10000);

        int runs = 10;
        long totalTime = 0;
        int size = 0;

        for (int i = 0; i < runs; i++) {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            long start = System.nanoTime();

            oos.writeObject(list); // 自动调用 writeExternal

            oos.flush();

            long end = System.nanoTime();

            totalTime += (end - start);
            size = bos.toByteArray().length;
        }

        System.out.println("Externalizable size: " + size);
        System.out.println("Externalizable avg time: " + (totalTime / runs));
    }

    private static List<ComplicatedVSAuction> createData(int n) {
        List<ComplicatedVSAuction> list = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            List<String> bids = Arrays.asList("A", "B", "C");
            list.add(new ComplicatedVSAuction(
                    "item" + i,
                    i,
                    bids,
                    "description" + i,
                    System.currentTimeMillis(),
                    Math.random()));
        }
        return list;
    }
}