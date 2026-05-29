package vsue.faults;

import java.lang.reflect.Method;

import vsue.communication.VSObjectConnection;
import vsue.rpc.Request;
import vsue.rpc.VSInvocationHandler;
import vsue.rpc.VSRemoteReference;

public class VSRPCSemanticAMOHandler implements VSRPCSemanticHandler{
    public VSRPCSemanticAMOHandler(){

    }
   
    @Override
    public Object invoke(VSInvocationHandler invocationHandler, Request request, Method method,
            VSObjectConnection connection) throws Throwable {
        // TODO Auto-generated method stub
        return null;
    }
}
