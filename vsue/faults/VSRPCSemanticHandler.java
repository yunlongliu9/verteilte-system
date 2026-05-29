package vsue.faults;

import java.lang.reflect.Method;

import vsue.communication.VSObjectConnection;
import vsue.rpc.Request;
import vsue.rpc.VSInvocationHandler;

public interface VSRPCSemanticHandler {
    public Object invoke(
            VSInvocationHandler invocationHandler,
            Request request,
            Method method,
            VSObjectConnection connection
    ) throws Throwable;
} 
