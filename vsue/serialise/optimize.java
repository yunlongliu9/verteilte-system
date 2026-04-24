package vsue.serialise;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;



public class optimize {
    public static void main(String[] args) throws Exception {
        int iterations = 100000;

        List<VSAuctionOpt> list = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            list.add(new VSAuctionOpt("item" + i, i));
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        long startTime = System.nanoTime();

        oos.writeObject(list);
        oos.flush();
        byte[] data = bos.toByteArray();

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("Time taken for " + iterations + " iterations: " + duration + " nanoseconds");
        System.out.println("OptimizedSerializeSize: " + bos.toByteArray().length);
    }
}
