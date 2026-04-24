package vsue.serialise;

import java.io.Externalizable;

import java.io.IOException;

import java.io.ObjectInput;

import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;

public class VSAuctionOpt implements Externalizable {

    private String name;
    private int price;

    // ❗ 必须要有无参构造函数
    public VSAuctionOpt() {
    }

    public VSAuctionOpt (String name, int price) {
        this.name = name;
        this.price = price;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(price);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        this.name = in.readUTF();
        this.price = in.readInt();
    }

    // getter / setter 保持不变
}
