package vsue.faults;

import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
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
    private static final int TIMEOUT = 100;

    @Override
    public Object invoke(VSInvocationHandler invocationHandler,Request request, Method method,VSObjectConnection connection) throws Throwable {
        java.util.List<Socket> createdSockets = new java.util.ArrayList<>();
        try {
            int versuch = 1;
            // LOM soll immer TIMEOUT lang warte
            // Weil: z.b. wenn alte antwort mit alter requestID kommt, 
            // soll sie ignoriert und der vollständige TIMEOUT abgewartet werden, bevor nochmal gesendet wird
            while (versuch <= MAX_VERSUCH) {
                long deadline = System.currentTimeMillis() + TIMEOUT;
                request.getRequestID().setSequenceNumber(versuch);

                try {
                    connection.sendObject(request);

                    while (true) {
                        long remaining = deadline - System.currentTimeMillis();
                        if (remaining <= 0) {
                            throw new SocketTimeoutException("LOM attempt timed out");
                        }

                        connection.getSocket().setSoTimeout((int) Math.min(Integer.MAX_VALUE, remaining));
                        Response result = (Response) connection.receiveObject();

                        if (result == null
                                || result.getRequestID() == null
                                || !request.getRequestID().getCallID().equals(result.getRequestID().getCallID())
                                || result.getRequestID().getSequenceNumber() != versuch) {
                            // Keine, falsche oder veraltete Antwort erhalten, also weiter warten
                            continue;
                        }

                        return invocationHandler.handleResponse(result, method.getReturnType());
                    }
                } catch (SocketTimeoutException | SocketException | VSConnectionEndOfFile e) {
                    System.out.println(versuch + ". Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());

                    // Connection ist kaputt, also neu verbinden und nochmal versuchen
                    Socket oldSock = connection.getSocket();
                    Socket newSock = new Socket(oldSock.getInetAddress(), oldSock.getPort());
                    newSock.setTcpNoDelay(true);
                    createdSockets.add(newSock);
                    try {
                        connection = connection.getClass().getConstructor(Socket.class).newInstance(newSock);
                    } catch (Exception ex) {
                        connection = new VSObjectConnection(newSock);
                    }
                }
                versuch++;
            }
            throw new RemoteException("Too many LOM retries, giving up :(");
        }
        finally {
            if (!connection.getSocket().isClosed()) {
                // Reset to no timeout because we don't want to affect other calls (e.g. AMO calls that might reuse the connection)
                connection.getSocket().setSoTimeout(0);
            }
            for (Socket s : createdSockets) {
                try { s.close(); } catch(Exception ignored) {}
            }
        }
    }

    
}