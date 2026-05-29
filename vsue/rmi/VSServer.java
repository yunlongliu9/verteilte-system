package vsue.rmi;

// Server socket + threads
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vsue.rmi.*;
import vsue.communication.*;

// Simple echo service - schickt alle erhaltenen objekte zurück an den Client
public class VSServer {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(11);

        try (ServerSocket serverSocket = new ServerSocket(1111)) {
            System.out.println("VSServer läuft auf Port " + 1111);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Auf eingehende Verbindungen warten
                System.out.println("Neue Verbindung von " + clientSocket.getInetAddress());

                // Neue Verbindung in einem separaten Thread behandeln
                executor.submit(() -> {
                    try {
                        // Alle daten unverändert zurückschicken während client die Verbindung offen hält
                        VSObjectConnection connection = new VSObjectConnection(clientSocket);
                        
                        while (true) {
                            Serializable receivedObject = connection.receiveObject();
                            if (receivedObject != null) {
                                System.out.println("Empfangen: " + receivedObject.getClass().getName() + " von " + clientSocket.getInetAddress());
                            }
                            connection.sendObject(receivedObject); // Echo zurück an den Client
                        }
                    } catch (VSConnectionEndOfFile e) {
                        System.out.println("Connection to " + clientSocket.getInetAddress() + " closed");
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
                        
