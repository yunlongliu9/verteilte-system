package vsue.faults;

import vsue.faults.VSObjectConnection;

public class VSBuggyObjectConnection extends VSObjectConnection{
    private boolean enableDelay;
    private boolean enableDrop;
    private boolean enableCrash;
    private int delayMs;
    private double dropRate;
    private double crashRate;

    public VSBuggyObjectConnection(String host, int port) throws Exception {
        super(host, port);
    }

    @Override
    public void sendObject(Object obj) throws Exception {
        if (enableCrash && Math.random() < crashRate) {
            throw new Exception("Simulated crash during send");
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
