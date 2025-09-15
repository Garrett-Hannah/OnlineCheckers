import chkNetwork.Client.ApplicationController;
import chkNetwork.Client.ClientModel;

import javax.swing.*;


public class StartChatWindow {

    public static void main (String[] args) {
        SwingUtilities.invokeLater(() -> {
            //Get a username from the user.
            String username = JOptionPane.showInputDialog("Enter your username:");
            if (username == null || username.trim().isEmpty()) {
                System.out.println("Username cancelled or empty. Exiting.");
                System.exit(0);
            }

            //Create the client logic model to communicate.
            ClientModel clientLogic = new ClientModel("localhost", 5000); // By Default.

            // Create the app controller.
            ApplicationController appController = new ApplicationController(clientLogic);

            // tell it to start the lobby.
            appController.startLobby();

            // Log into the server
            appController.attemptLogin(username.trim());

            System.out.println("Application setup initiated. Controller manages connection and UI.");
        });
    }
}