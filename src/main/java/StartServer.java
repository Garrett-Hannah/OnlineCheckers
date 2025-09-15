import chkNetwork.Server.CheckerNetworkService;

import java.io.IOException;

//Use the CheckerNetworkService to start a server @ the default port (5000)
public class StartServer {
    public static void main (String[] args) {
        final int DEFAULT_PORT = 5000;
        CheckerNetworkService server = new CheckerNetworkService(DEFAULT_PORT);
        try {
            //Way to shut down the server from running.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown hook triggered. Stopping server...");
                server.stop();
            }, "ServerShutdownHook"));

            server.start(); //Start the server.

            //Log that the server is finished succesfully.
            System.out.println("Server has fully stopped.");

        } catch (IOException e) {
            System.err.println("Failed to start server on port " + DEFAULT_PORT + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1); // Exit if server cannot start
        }
    }
}
