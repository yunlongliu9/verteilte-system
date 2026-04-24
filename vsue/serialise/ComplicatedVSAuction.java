package vsue.serialise;


import java.io.Externalizable;

import java.io.IOException;

import java.io.ObjectInput;

import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ComplicatedVSAuction implements Externalizable {

    String name;

    int price;
List<String> bids;
    String description;

    long timestamp;

    double rating;

    // ❗ 必须要有无参构造函数
    public ComplicatedVSAuction() {
    }

    public ComplicatedVSAuction(String name, int price, List<String> bids,

            String description, long timestamp, double rating) {

        this.name = name;

        this.price = price;

        this.bids = bids;

        this.description = description;

        this.timestamp = timestamp;

        this.rating = rating;

    }

    @Override

    public void writeExternal(ObjectOutput out) throws IOException {

        // name

        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

        out.writeInt(nameBytes.length);

        out.write(nameBytes);

        // price

        out.writeInt(price);

        // description

        byte[] descBytes = description.getBytes(StandardCharsets.UTF_8);

        out.writeInt(descBytes.length);

        out.write(descBytes);

        // timestamp + rating

        out.writeLong(timestamp);

        out.writeDouble(rating);

        // bids

        if (bids == null) {

            out.writeInt(0);

        } else {

            out.writeInt(bids.size());

            for (String b : bids) {

                byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

                out.writeInt(bBytes.length);

                out.write(bBytes);

            }

        }

    }

    @Override

    public void readExternal(ObjectInput in) throws IOException {

        // name

        int nameLen = in.readInt();

        byte[] nameBytes = new byte[nameLen];

        in.readFully(nameBytes);

        this.name = new String(nameBytes, StandardCharsets.UTF_8);

        // price

        this.price = in.readInt();

        // description

        int descLen = in.readInt();

        byte[] descBytes = new byte[descLen];

        in.readFully(descBytes);

        this.description = new String(descBytes, StandardCharsets.UTF_8);

        // timestamp + rating

        this.timestamp = in.readLong();

        this.rating = in.readDouble();

        // bids

        int size = in.readInt();

        this.bids = new ArrayList<>();

        for (int i = 0; i < size; i++) {

            int len = in.readInt();

            byte[] bBytes = new byte[len];

            in.readFully(bBytes);

            bids.add(new String(bBytes, StandardCharsets.UTF_8));

        }

    }
    // getter / setter 保持不变
}
