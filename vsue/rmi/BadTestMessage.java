package vsue.rmi;

import java.io.Serializable;

public class BadTestMessage implements Serializable
{
    private int integer;
    private String string;
    private Object[] objects;
    
    public BadTestMessage(int a, String b, Object[] c)
    {
        integer = a;
        string = b;
        objects = c;
    }
}