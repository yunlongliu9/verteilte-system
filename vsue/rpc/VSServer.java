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
import vsue.faults.VSRPCSemanticType;
import java.util.HashMap;
import java.util.Map;
import java.lang.Thread;
import java.util.Iterator;

public class VSServer {

    private int port = 1111; // Standardport für den VSRemoteObjectManager
    private boolean useBuggyConnections = false;
    private HashMap<String, Response> responses = new HashMap<String, Response>();
    
    public VSServer(int port) {
        this.port = port;
    }

    public VSServer(int port, boolean useBuggyConnections) {
        this.port = port;
        this.useBuggyConnections = useBuggyConnections;
    }

    class AMOGC extends Thread
    {
        long stepMillis;
        
        AMOGC(long stepMillis)
        {
            this.stepMillis = stepMillis;
        }
        
        public void run()
        {
            long limit = System.currentTimeMillis();
            int numberOfRemovedItems;
            while (true) {
                try {
                    Thread.sleep(stepMillis);
                } catch (InterruptedException e) {
                    System.err.println("AMOGC was interrupted");
                    break;
                }
                numberOfRemovedItems = 0;
                synchronized (responses) {
                    if (responses.size() > 0) {
                        Iterator<Map.Entry<String, Response>> i = responses.entrySet().iterator();
                        while (i.hasNext()) {
                            Map.Entry<String, Response> next = i.next();
                            Response r = next.getValue();
                            if (r.timeWhenThisWasSaved < limit) {
                                i.remove();
                                numberOfRemovedItems++;
                            }
                        }
                    }
                }
                System.out.println("AMOGC: Removed " + numberOfRemovedItems + " entries in this pass");
                limit += stepMillis;
            }
        }
    }
    
    public void start() {
        // retries * socket timeout + slack
        // Bei zu kleinem Slack (z.B. 100) kann es sein, dass der GC Antworten zu früh wegschmeißt
        new AMOGC(5 * 100 + 1000).start();
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true); // Ermöglicht schnelleres Neustarten des Servers
            System.out.println("VSServer läuft auf Port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Auf eingehende Verbindungen warten
                clientSocket.setTcpNoDelay(true); // Nagle-Algorithmus deaktivieren für geringere Latenz
                
                // Neue Verbindung in einem separaten Thread behandeln
                executor.submit(() -> {
                    try {
                        VSObjectConnection connection;
                        if (useBuggyConnections) {
                            connection = new VSBuggyObjectConnection(clientSocket);
                        } else {
                            connection = new VSObjectConnection(clientSocket);
                        }
                        
                        while (true) {
                            // Nachricht empfangen und verarbeiten
                            Serializable receivedObject = connection.receiveObject();
                            
                            if (!(receivedObject instanceof Request)) {
                                System.out.println("Fehler: Erwartet Request, erhalten " + receivedObject.getClass().getName());
                                continue;
                            }
                            
                            Request request = (Request) receivedObject;
                            VSRequestID vsRequestID = request.getRequestID();
                            
                            // AMO wird separat behandelt
                            if (request.semantic == VSRPCSemanticType.AT_MOST_ONCE) {
                                // Die AMOcallID ist die Sequenznummer, die jeder Client für seine Anfragen
                                // verwaltet. Nicht zu verwechseln mit den anderen IDs.
                                System.out.println("\t\tServer: callID == " + request.AMOcallID);
                                
                                String user = request.user;
                                Response response;
                                String id = vsRequestID.getCallID();
                                
                                synchronized (responses) {
                                    response = responses.get(user);
                                    // Optional (für kleinere kritische Abschnitte):
                                    // Hier Wächterobjekt in Map ablegen, das anderen Threads signalisiert, dass für diesen User gerade eine Antwort generiert wird.
                                    // Wenn man als Thread auf so ein Objekt trifft könnte man entweder auf die Antwort warten, oder eine Antwort von wegen „Ausführung in Bearbeitung“ an den Client schicken.
                                    // In ihrer Musterlösung haben die Tutoren das mit Warten gelöst.
                                    // Danach nur noch am Responseobjekt synchronisieren (oder ist das dann sogar unnötig?)
                                    
                                    if (response != null && request.AMOcallID < response.AMOcallID) {
                                        System.out.println("\t\tServer: Ignoriere alte Anfrage");
                                        continue;
                                    }
                                    
                                    if (response != null && id.equals(response.ID)) {
                                        System.out.println("\t\tServer: Nutze gespeicherte Antwort");
                                    } else {
                                        System.out.println("\t\tServer: Generiere neue Antwort");
                                        // Fast identisch zum Nicht-AMO-Fall
                                        // Könnte man später evtl. zusammenführen
                                        try {
                                            VSRemoteObjectManager manager = VSRemoteObjectManager.getInstance("localhost", port, useBuggyConnections);
                                            Object result = manager.invokeMethod(
                                                request.getObjectId(),
                                                request.getMethodName(),
                                                request.getParameters()
                                            );
                                    
                                            if (result instanceof Remote && !Proxy.isProxyClass(result.getClass())){
                                                result = VSRemoteObjectManager.getInstance().lookUpStub((Remote) result);
                                            }																			

                                            // Bei AMO bräuchte man die RequestID nicht unbedingt.
                                            response = new Response(result, null, vsRequestID);
                                        } catch (Exception e) {
                                            System.out.println("Fehler bei Methodenaufruf: " + e.getMessage());
                                            response = new Response(null, e, vsRequestID);
                                        }
                                        response.timeWhenThisWasSaved = System.currentTimeMillis();
                                        response.ID = id;
                                        response.AMOcallID = request.AMOcallID;
                                        Response alt = responses.put(user, response);
                                        if (alt == null) {
                                            System.out.println("\t\tServer: Speichere neue Antwort");
                                        } else {
                                            System.out.println("\t\tServer: Überschreibe alte Antwort");
                                        }
                                    }
                                }
                                connection.sendObject(response);
                            } else {
                                Response reply;
                                try {
                                    // Methode über Manager aufrufen
                                    // Manager wird automatisch erstellt falls er noch nicht existiert
                                    VSRemoteObjectManager manager = VSRemoteObjectManager.getInstance("localhost", port, useBuggyConnections);
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
                        }
                    } catch (VSConnectionEndOfFile e) {
                        // Das wird recht häufig gespammt
                        // System.out.println("Server: VSConnectionEndOfFile");
                    } catch (Exception e) {
                        System.out.println("\t\tServer: " + e.toString());
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
                        
