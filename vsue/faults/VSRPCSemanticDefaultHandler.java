package vsue.faults;

import java.lang.reflect.Method;

import vsue.communication.VSObjectConnection;
import vsue.rpc.Request;
import vsue.rpc.Response;
import vsue.rpc.VSInvocationHandler;

public class VSRPCSemanticDefaultHandler
        implements VSRPCSemanticHandler {

    @Override
    public Object invoke(VSInvocationHandler invocationHandler,Request request,
            Method method,VSObjectConnection connection) throws Throwable {
        Response response = (Response)invocationHandler.transportProcess(request, connection);
        return invocationHandler.handleResponse(response, method.getReturnType());
    }
}