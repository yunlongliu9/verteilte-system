package vsue.faults;

import java.io.IOException;

import vsue.faults.VSObjectConnection;

public class VSBuggyObjectConnection extends VSObjectConnection{
    private boolean enableDelay = false;
    private boolean enableDrop = false;
    private boolean enableCrash = false;

    private int delayMs = 2000; // Default delay of 4 seconds
    private double dropRate = 0.8; // Default drop rate of 10%
    private double crashRate = 0.1; // Default crash rate of 10%

    public VSBuggyObjectConnection(String host, int port) throws Exception {
        super(host, port);
    }
    
    public VSBuggyObjectConnection(String host, int port, boolean enableDelay, boolean enableDrop, boolean enableCrash) throws Exception {
        super(host, port);
        this.enableDelay = enableDelay;
        this.enableDrop = enableDrop;
        this.enableCrash = enableCrash;
    }


    @Override
    public void sendObject(Object obj) throws Exception {
        if (enableCrash && Math.random() < crashRate) {
            super.conn.close();
            throw new IOException(
                "Simulated crash during send"
            );
        }
        if (enableDrop && Math.random() < dropRate) {
            return; // Simulate message drop by not sending the object
        }
        if (enableDelay) {
            Thread.sleep(delayMs); // Simulate network delay
        }
        super.sendObject(obj); // Call the original sendObject method to actually send the object
    }

}
