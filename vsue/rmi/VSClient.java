package vsue.rmi;

import java.io.IOException;
import java.net.Socket;

import vsue.rmi.*;
import vsue.communication.VSObjectConnection;


public class VSClient {
    public static void main(String[] args) {
        try {
            String hostname = args.length > 0 ? args[0] : "localhost";
            Socket socket = new Socket(hostname, 1111);
            System.out.println("Verbunden mit VSServer auf " + hostname + ":1111");

            VSObjectConnection connection = new VSObjectConnection(socket);
            
            // Gibt ne recht lange Ausgabe
            int longStringLength = 1024 * 1024;
            StringBuilder longString = new StringBuilder(longStringLength);
            for (int i = 0; i < longStringLength; i++) {
                longString.append("A");
            }
            VSTestMessage testMessage = new VSTestMessage(0, longString.toString(), null);
            // connection.sendObjectWithDebugOutput(testMessage);
            VSTestMessage response = (VSTestMessage) connection.receiveObject();
            System.out.println(testMessage.equals(response));
            // Stringlänge lange Strings 64 Bit
            // Präfix 7c statt 74
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
