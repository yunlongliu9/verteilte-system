package vsue.rpc;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.*;
import vsue.communication.*;
import vsue.faults.VSRequestID;

public class VSServer {

    private int port = 1111; // Standardport für den VSRemoteObjectManager
    
    public VSServer(int port) {
        this.port = port;
    }

    public void start() {
        ExecutorService executor = Executors.newFixedThreadPool(11);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true); // Ermöglicht schnelleres Neustarten des Servers
            System.out.println("VSServer läuft auf Port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Auf eingehende Verbindungen warten
                clientSocket.setTcpNoDelay(true); // Nagle-Algorithmus deaktivieren für geringere Latenz
                
                // Neue Verbindung in einem separaten Thread behandeln
                executor.submit(() -> {
                    try {
                        VSObjectConnection connection = new VSObjectConnection(clientSocket);
                        
                        while (true) {
                            // Nachricht empfangen und verarbeiten
                            Serializable receivedObject = connection.receiveObject();
                            
                            if (!(receivedObject instanceof Request)) {
                                System.out.println("Fehler: Erwartet Request, erhalten " + receivedObject.getClass().getName());
                                continue;
                            }
                            
                            Request request = (Request) receivedObject;
                            VSRequestID vsRequestID = request.getRequestID();
                            Response reply;
                            try {
                                // Methode über Manager aufrufen
                                // Manager wird automatisch erstellt falls er noch nicht existiert
                                VSRemoteObjectManager manager = VSRemoteObjectManager.getInstance("localhost", port);
                                Object result = manager.invokeMethod(
                                    request.getObjectId(),
                                    request.getMethodName(),
                                    request.getParameters()
                                );
                        
                                if (result instanceof Remote && !Proxy.isProxyClass(result.getClass())){
                                    result = VSRemoteObjectManager.getInstance().lookUpStub((Remote) result); // convert to stub if but not a stub yet	
                                }																			
                            
                                
                                // Antwort senden
                                reply = new Response(result, null, vsRequestID);
                            } catch (Exception e) {
                                // Exception wird in der Response gekapselt
                                System.out.println("Fehler bei Methodenaufruf: " + e.getMessage());
                                reply = new Response(null, e, vsRequestID);
                            }
                            
                            connection.sendObject(reply); // Antwort zurück an den Client
                        }
                    } catch (VSConnectionEndOfFile e) {
                        // System.out.println("Connection to " + clientSocket.getInetAddress() + " closed");
                    } catch (VSConnectionException e) {
                        System.out.println(e.getMessage());
                    } catch (IOException e) {
                        System.out.println("Verbindung zu " + clientSocket.getInetAddress() + " verloren");
                    } catch (Exception e) {
                        // alles andere
                        System.out.println(e.getMessage());
                    } finally {
                        // Socket schließen, wenn die Verbindung verloren geht oder ein Fehler auftritt
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}
                        
