package vsue.serialise.ProblemTest;

import java.io.Serializable;

public class VSTestMessage implements Serializable {
    public int integer;
    public String string;
    public Object[] objects;

    public VSTestMessage(int integer, String string, Object[] objects) {
        this.integer = integer;
        this.string = string;
        this.objects = objects;
    }
}