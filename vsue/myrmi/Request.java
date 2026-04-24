package vsue.myrmi;

import java.io.Serializable;

public class Request implements Serializable {

    public String methodName;

    public Class<?>[] paramTypes;

    public Object[] args;

}