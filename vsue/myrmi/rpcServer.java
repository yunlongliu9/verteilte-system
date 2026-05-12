package vsue.myrmi;

import java.lang.reflect.Method;
import java.io.EOFException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;

import vsue.rmi.VSAuctionServiceImpl;

public class rpcServer {
    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(12345);
        System.out.println("RPC Server running...");
        Socket socket = server.accept();
        VSObjectConnection objConn = new VSObjectConnection(new VSConnection(socket));

        Object service = new VSAuctionServiceImpl();

        
        while(true){
           try {

        System.out.println("Server waiting for request...");

        Request req = (Request) objConn.receiveObject();

        System.out.println("Server received request!");

        Response resp = new Response();

        try {

            Method method = service.getClass().getMethod(

                    req.methodName,

                    req.paramTypes

            );

            Object result = method.invoke(service, req.args);

            resp.result = result;

        } catch (InvocationTargetException e) {

            resp.exception = (Exception) e.getCause();

        } catch (Exception e) {

            resp.exception = e;

        }

        objConn.sendObject(resp);

        System.out.println("Server sent response!");

    } catch (EOFException e) {

        System.out.println("Client disconnected.");

        break;  // 👈 关键：退出循环

    }
        }

        }
    
}