package vsue.faults;

import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;

import vsue.communication.VSConnectionEndOfFile;
import vsue.communication.VSObjectConnection;
import vsue.rpc.Request;
import vsue.rpc.Response;
import vsue.rpc.VSInvocationHandler;

public class VSRPCSemanticLOMHandler
        implements VSRPCSemanticHandler {

    private static final int MAX_VERSUCH = 5;
    private static final int TIMEOUT = 3000;

    @Override
    public Object invoke(VSInvocationHandler invocationHandler,Request request, Method method,VSObjectConnection connection) throws Throwable {
        try{
        int versuch = 1;
            while (versuch <= MAX_VERSUCH) {
                try {
                    connection.getSocket().setSoTimeout(TIMEOUT);
                    request.getRequestID().setSequenceNumber(versuch);
                    Response result = invocationHandler.transportProcess(request, connection);
                    if (result == null||!(request.getRequestID().getCallID().equals(result.getRequestID().getCallID())) || result.getRequestID().getSequenceNumber() != versuch){
                        versuch++;
                        continue;
                    }
                    return invocationHandler.handleResponse(result,method.getReturnType());
                } catch (SocketTimeoutException e) {
                    System.out.println(versuch + " timeout");
                }
                versuch++;
            }
            throw new RemoteException("to many Timeouts retry failed in LOM");
        }
        finally{
            connection.getSocket().setSoTimeout(0);
        }
    }

    
}