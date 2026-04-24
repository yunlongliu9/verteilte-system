
package vsue.serialise;


import java.io.Externalizable;

import java.io.IOException;

import java.io.ObjectInput;

import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class ComplicatedVSAuctionNormal implements Serializable {

    String name;
    int price;
    List<String> bids;
    String description;
    long timestamp;
    double rating;
    // ❗ 必须要有无参构造函数
    public ComplicatedVSAuctionNormal() {
    }

    public ComplicatedVSAuctionNormal(String name, int price, List<String> bids,

            String description, long timestamp, double rating) {

        this.name = name;

        this.price = price;

        this.bids = bids;

        this.description = description;

        this.timestamp = timestamp;

        this.rating = rating;

    }

   
}
