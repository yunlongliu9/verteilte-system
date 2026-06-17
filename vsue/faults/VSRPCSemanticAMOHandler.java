package vsue.faults;

import java.lang.reflect.Method;

import vsue.communication.*;
import vsue.rpc.*;
import vsue.faults.VSRequestID;
import vsue.faults.VSRPCSemanticType;
import java.net.*;
import java.rmi.RemoteException;

public class VSRPCSemanticAMOHandler implements VSRPCSemanticHandler
{
    private static int callID = 0;
    final int MAX_TRIES = 5;
    final int TIMEOUT = 100;
    
    @Override
    public Object invoke(
        VSInvocationHandler invocationHandler,
        Request request,
        Method method,
        VSObjectConnection connection
    ) throws Throwable
    {
        request.semantic = VSRPCSemanticType.AT_MOST_ONCE;
        request.AMOcallID = callID++;
        connection.getSocket().setSoTimeout(TIMEOUT);
        for (int tries = 1; tries <= MAX_TRIES; tries++) {
            System.out.println("\tVersuch " + tries + ":");
            try {
                Response response = invocationHandler.transportProcess(request, connection);
                return invocationHandler.handleResponse(response, method.getReturnType());
            } catch (SocketException | VSConnectionEndOfFile | SocketTimeoutException e) {
                System.out.println("\t\tClient: " + e.toString());

                // Reconnect with a new socket; keep the same callID
                Socket oldSock = connection.getSocket();
                try { oldSock.close(); } catch (Exception ignored) {}
                Socket newSock = new Socket(oldSock.getInetAddress(), oldSock.getPort());
                newSock.setTcpNoDelay(true);
                newSock.setSoTimeout(TIMEOUT);
                try {
                    connection = connection.getClass()
                            .getConstructor(Socket.class).newInstance(newSock);
                } catch (Exception ex) {
                    connection = new VSObjectConnection(newSock);
                }
                continue;
            }
        }
        invocationHandler.closeConnection();
        throw new RemoteException("VSRPCSemanticAMOHandler: max tries exceeded");
    }
}
