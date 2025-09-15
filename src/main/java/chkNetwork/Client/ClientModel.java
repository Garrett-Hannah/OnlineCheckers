package chkNetwork.Client;

import javax.swing.SwingUtilities;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets; // Explicit charset
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkMVC.chModel.Checkers.Position;
import chkNetwork.Server.ServerResponse;
import chkNetwork.CLIENT_REQUEST_CODES;
import com.google.gson.Gson;

/**
 * ClientModel handles network communication with the server.
 * It connects, sends requests, listens for responses, and notifies
 * its listener (typically an ApplicationController) of network events and data.
 * It does not manage game state or specific UI views directly.
 */
public class ClientModel {

    private final String host;
    private final int port;
    private String username;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean isRunning = false; // volatile for thread visibility
    private final List<ClientEventListener> listeners = new ArrayList<>(); // Should ideally only contain ApplicationController
    private final Gson gson = new Gson();

    public ClientModel (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void addListener (ClientEventListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
                System.out.println("Listener added: " + listener.getClass().getName());
            }
        }
    }

    public void removeClientEventListener (ClientEventListener clientEventListener) {
        synchronized (listeners) {
            listeners.remove(clientEventListener);
            System.out.println("Listener removed: " + clientEventListener.getClass().getName());
        }
    }

    private void notifyListeners (Consumer<ClientEventListener> callback) {
        List<ClientEventListener> listenerCopy;
        synchronized (listeners) {
            // Create copy to avoid ConcurrentModificationException if listener removes itself during notification
            listenerCopy = new ArrayList<>(listeners);
        }

        if (listenerCopy.isEmpty()) {
            System.out.println("No listeners registered to notify.");
            return;
        }

        for (ClientEventListener listener : listenerCopy) {
            try {
                // Let the callback execute. It should handle EDT
                callback.accept(listener);
            } catch (Exception e) {
                System.err.println("Error notifying listener " + listener.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace(); // Log listener errors but continue notifying others
            }
        }
    }

    // Helper to notify listener for events that will likely update UI
    private void notifyListenersOnEDT (Consumer<ClientEventListener> callback) {
        SwingUtilities.invokeLater(() -> notifyListeners(callback));
    }


    private void notifyErrorMessage (String title, String message) {
        // Error messages should always be shown on the EDT
        notifyListenersOnEDT(l -> l.showErrorMessage(title, message));
    }

    public boolean connectToServer (String username) {
        if (isRunning) {
            System.err.println("Already connected or attempting to connect.");
            return true;
        }

        if (username == null || username.trim().isEmpty()) {
            // Use notifyErrorMessage which handles EDT
            notifyErrorMessage("Login Error", "Username cannot be empty.");
            return false;
        }

        this.username = username.trim();

        try {
            System.out.println("Connecting to " + host + ":" + port + " as " + this.username + "...");
            this.socket = new Socket(host, port);
            // Explicitly use UTF-8 for consistency
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            ClientRequest clientRequest = new ClientRequest(CLIENT_REQUEST_CODES.JOIN, Collections.singletonList(this.username));
            String jsonRequest = gson.toJson(clientRequest);
            System.out.println("Sending JOIN request: " + jsonRequest);
            this.out.println(jsonRequest);
            if (out.checkError()) {
                throw new IOException("PrintWriter error after sending JOIN request.");
            }

            isRunning = true; // Set before starting thread

            Thread listenerThread = new Thread(this::listenToServer);
            listenerThread.setDaemon(true);
            listenerThread.setName("Client-Listener-" + this.username);
            listenerThread.start();

            final String title = this.username + "'s Lobby - Connected";
            // Notify listener (Controller) to update title - Use EDT-safe notification
            notifyListenersOnEDT(l -> l.setWindowTitle(title));

            System.out.println("Connection successful as " + this.username + ". Listener thread started.");
            return true;

        } catch (UnknownHostException e) {
            notifyErrorMessage("Connection Error", "Unknown host: " + host);
            cleanupFailedConnection();
            return false;
        } catch (IOException e) {
            notifyErrorMessage("Connection Error", "Couldn't connect to server: " + e.getMessage());
            cleanupFailedConnection();
            return false;
        } catch (Exception e) { // Catch broader exceptions during setup
            notifyErrorMessage("Connection Error", "Unexpected error during connection: " + e.getMessage());
            e.printStackTrace();
            cleanupFailedConnection();
            return false;
        }
    }

    // Helper to clean up resources if connection fails during setup
    private void cleanupFailedConnection () {
        isRunning = false;
        closeResources(); // Close any partially opened resources
        System.err.println("Connection attempt failed. Resources cleaned up.");
    }

    private void listenToServer () {
        System.out.println("Listener thread started for " + username);
        try {
            String serverResponseString;
            // Check isRunning *before* blocking readLine call
            while (isRunning && (serverResponseString = in.readLine()) != null) {
                System.out.println("Raw <<< Server: " + serverResponseString);
                try {
                    ServerResponse response = gson.fromJson(serverResponseString, ServerResponse.class);
                    if (response == null) {
                        System.err.println("Received null response object after parsing: " + serverResponseString);
                        continue;
                    }
                    // Process the valid response on this network thread.
                    // handleResponse will use notifyListenersOnEDT if the event typically requires UI updates.
                    handleResponse(response);
                } catch (com.google.gson.JsonSyntaxException e) {
                    System.err.println("Error parsing JSON from server: '" + serverResponseString + "' - " + e.getMessage());
                    // notifyErrorMessage("Network Error", "Received malformed data from server.");
                } catch (Exception processError) {
                    System.err.println("Error processing server message: '" + serverResponseString + "' - " + processError.getMessage());
                    processError.printStackTrace();
                }
            }
        } catch (SocketException e) {
            if (isRunning) {
                System.err.println("SocketException in listener (connection likely lost): " + e.getMessage());
                notifyErrorMessage("Connection Lost", "Connection to the server was lost or reset.");
            } else {
                System.out.println("Socket closed normally during disconnect process for " + username);
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("IOException in listener thread for " + username + ": " + e.getMessage());
                e.printStackTrace();
                notifyErrorMessage("Network Error", "Error reading data from server: " + e.getMessage());
            } else {
                System.out.println("IOException likely due to closing stream during disconnect for " + username);
            }
        } catch (Exception e) {
            System.err.println("Unexpected exception in listener thread for " + username + ":");
            e.printStackTrace();
            if (isRunning) {
                notifyErrorMessage("Critical Error", "An unexpected error occurred in the network listener.");
            }
        } finally {
            System.out.println("Listener thread loop terminating for " + username);
            // If loop terminates unexpectedly while 'isRunning' is true, initiate disconnect.
            if (isRunning) {
                System.out.println("Listener thread ended unexpectedly. Initiating disconnect cleanup...");
                disconnect(); // Ensures state update and listener notification
            }
            System.out.println("Server listener thread finished execution for " + username);
        }
    }

    // Handles dispatching server responses based on type.
    private void handleResponse (ServerResponse response) {
        try {
            switch (response.getType()) {
                case SUCCESS:
                    // Log success, specific actions depend on context (handled by controller if needed)
                    System.out.println("Server acknowledged SUCCESS. Payload: " + response.getPayload());
                    break;

                case NOTIFICATION:
                    final String notification = response.getPayloadAsStringSafe(); // Use safe method if available
                    System.out.println("Received NOTIFICATION: " + notification);
                    // Notifications often update UI -> Use EDT-safe notification
                    notifyListenersOnEDT(l -> l.appendMessage(notification));
                    break;

                case CLIENT_LIST:
                    final List<String> userList = response.getPayload(); // Expecting List<String>
                    System.out.println("Received CLIENT_LIST: " + userList);
                    // User list updates UI -> Use EDT-safe notification
                    notifyListenersOnEDT(l -> l.updateUserList(userList != null ? userList : Collections.emptyList()));
                    break;

                case ERROR:
                    final String errorMessage = response.getPayloadAsStringSafe();
                    System.err.println("Received ERROR from server: " + errorMessage);
                    notifyErrorMessage("Server Error", errorMessage); // Already uses EDT
                    break;

                case CHAT_MESSAGE:
                    List<String> msgData = response.getPayload();
                    if (msgData != null && msgData.size() >= 2) {
                        // Assuming format: [Sender, Message]
                        final String chatMsg = msgData.get(0) + ": " + msgData.get(1);
                        System.out.println("Received CHAT_MESSAGE: " + chatMsg);
                        // Chat messages update UI -> Use EDT-safe notification
                        notifyListenersOnEDT(l -> l.appendMessage(chatMsg));
                    } else {
                        System.err.println("Received malformed CHAT_MESSAGE payload: " + response.getPayload());
                    }
                    break;

                case ROLE_ASSIGN:
                    System.out.println("Received ROLE_ASSIGN: " + response.getPayload());
                    handleRoleAssign(response.getPayload()); // This will call notifyListenersOnEDT
                    break;

                case GAME_START:
                    System.out.println("Received GAME_START");
                    handleGameStart(); // This will call notifyListenersOnEDT
                    break;

                case MOVE_PIECE:
                    System.out.println("Received MOVE_PIECE: " + response.getPayload());
                    handleMovePiece(response.getPayload());
                    break;

                // Handle other relevant SERVER_RESPONSE_CODES like GAME_END, ROUND_UPDATE etc.
                case GAME_END:
                    System.out.println("Received GAME_END: " + response.getPayload());

                    PIECE_TEAM team = PIECE_TEAM.valueOf(response.getPayload().getFirst());
                    notifyListenersOnEDT(l -> l.onGameEnd(team));
                    break;

                case ROUND_UPDATE:

                    handleRoundUpdate(response);

                    System.out.println("Received ROUND_UPDATE: " + response.getPayload());
                    break;

                case REMOVE_PIECE:
                    handleRemovePiece(response);
                    System.out.println("Received REMOVE_PIECE: " + response.getPayload());
                    break;

                //case GAME_END:


                default:
                    System.out.println("Received unhandled server response type: " + response.getType() + " | Payload: " + response.getPayload());
                    break;
            }
        } catch (Exception e) {
            // Catch errors during handling of a specific response type
            System.err.println("Error processing response type " + response.getType() + ": " + e.getMessage());
            e.printStackTrace();
            notifyErrorMessage("Processing Error", "Failed to process message from server: " + response.getType());
        }
    }

    private void handleRemovePiece (ServerResponse response) {

        Position removePosition = Position.fromString(response.getPayload().getFirst());

        System.out.println("Removing Piece @ " + removePosition.toString());

        notifyListeners(l -> l.onServerRemoveConfirmed(removePosition));

    }

    private void handleRoundUpdate (ServerResponse response) {
        List<String> turnInfo = response.getPayload();
        if (turnInfo != null && !turnInfo.isEmpty()) {
            try {
                PIECE_TEAM nextTurn = PIECE_TEAM.valueOf(turnInfo.get(0));
                //
                // notifyListenersOnEDT(l -> l.(nextTurn)); // Add to interface if needed
                System.out.println("FIND A WAY TO UPADTE THE CLIENT ABOUT THE FACT THAT THE ROUND JUST ENDED!!!!!");
                notifyListeners(l -> l.onRoundUpdate(nextTurn));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid team in ROUND_UPDATE");
            }
        }
    }

    // Parses role code and notifies listener (on EDT)
    private void handleRoleAssign (List<String> payload) {
        if (payload == null || payload.isEmpty()) {
            System.err.println("Received empty ROLE_ASSIGN payload.");
            return;
        }
        String roleCode = payload.getFirst();


        final boolean isHost[] = {false}; // Use array trick for mutable boolean in lambda
        final PIECE_TEAM team[] = {PIECE_TEAM.SPECTATOR}; // Default to spectator

        switch (roleCode) {
            case "H":
                isHost[0] = true;
                System.out.println("Role assigned: Host");
                // Notify host status separately (can be host AND player)
                notifyListenersOnEDT(l -> l.setHostView(true));
                break; // Host assignment might be followed by team assignment if host plays
            case "W":
                team[0] = PIECE_TEAM.WHITE;
                System.out.println("Role assigned: White");
                notifyListenersOnEDT(l -> l.onTeamAssign(PIECE_TEAM.WHITE));
                break;
            case "B":
                team[0] = PIECE_TEAM.BLACK;
                System.out.println("Role assigned: Black");
                notifyListenersOnEDT(l -> l.onTeamAssign(PIECE_TEAM.BLACK));
                break;
            case "S":
                team[0] = null; // Explicitly spectator (null team)
                System.out.println("Role assigned: Spectator");
                notifyListenersOnEDT(l -> l.onTeamAssign(null));
                break;
            default:
                System.err.println("Unknown role code received: " + roleCode);
                break; // Handle wrong roles.
        }
    }

    //Handle a move piece
    private void handleMovePiece (List<String> payload) {
        if (payload == null || payload.size() < 2) {
            System.err.println("Received malformed MOVE_PIECE payload: " + payload);
            notifyErrorMessage("Network Error", "Received invalid move data from server.");
            return;
        }

        try {

            Position from = Position.fromString(payload.get(0));
            Position to = Position.fromString(payload.get(1));

            System.out.println("Parsed server move: " + from + " -> " + to);

            //Notify the movement on the event thread.
            notifyListenersOnEDT(l -> l.onServerMoveConfirmed(from, to));

        } catch (IllegalArgumentException e) {
            System.err.println("Error parsing position strings in MOVE_PIECE payload '" + payload + "': " + e.getMessage());
            notifyErrorMessage("Data Error", "Received unparseable move data from server.");
        } catch (Exception e) {
            System.err.println("Unexpected error processing MOVE_PIECE payload: " + e.getMessage());
            e.printStackTrace();
            notifyErrorMessage("Internal Error", "Failed to handle game move message.");
        }
    }


    // Notifies listener (on EDT) that game should start
    private void handleGameStart () {
        notifyListenersOnEDT(ClientEventListener::onGameStart);
    }

    // REMOVED assignTeam and assignHost - logic is now inside handleRoleAssign


    /**
     * SendClientRequest
     * <p>
     * Sends a clientRequest. puts the username included in the message.
     */
    public void sendClientRequest (CLIENT_REQUEST_CODES request, List<String> args) {
        if (!isRunning || out == null || socket == null || socket.isClosed()) {
            System.err.println("Cannot send request - not connected or connection closed.");
            notifyErrorMessage("Network Error", "Not connected to server. Cannot send: " + request);
            return;
        }

        List<String> fullPayload = new ArrayList<>();
        if (this.username == null) {
            System.err.println("Critical Error: Attempting to send request while username is null.");
            notifyErrorMessage("Internal Error", "Cannot send request: Client username missing.");
            return;
        }
        // Convention: Add so it is [Code] (user, a, b, c, ...). //but now its json so that doesnt matter.
        fullPayload.add(this.username);

        if (args != null && !args.isEmpty()) {
            fullPayload.addAll(args);
        }

        ClientRequest clientRequestObj = new ClientRequest(request, fullPayload);

        try {
            String jsonRequest = gson.toJson(clientRequestObj);
            //Print the raw json being sent to the server.
            System.out.println("Raw >>> Server: " + jsonRequest);
            //Send the jsonRequest to the server.
            out.println(jsonRequest);
            // Check for errors *after* sending
            if (out.checkError()) {
                System.err.println("PrintWriter error occurred after sending request. Connection might be broken.");
                // This might indicate the other side closed the connection.
                notifyErrorMessage("Network Error", "Failed to send data. Connection may be lost.");
                // Consider initiating disconnect if this happens repeatedly
                // disconnect();
            }
        } catch (Exception e) { // Catch Gson serialization errors etc.
            System.err.println("Error serializing or sending request to JSON: " + request + " - " + e.getMessage());
            e.printStackTrace();
            notifyErrorMessage("Serialization Error", "Could not format/send request: " + request);
        }
    }

    // Overload for requests without additional arguments (username is still added)
    public void sendClientRequest (CLIENT_REQUEST_CODES request) {
        sendClientRequest(request, null);
    }

    // Disconnects the client and cleans up resources
    public void disconnect () {
        // Prevent multiple disconnect calls
        if (!isRunning) {
            System.out.println("Disconnect called but client " + (username != null ? username : "") + " is not running.");
            return;
        }

        System.out.println("Initiating disconnection process for " + (username != null ? username : "client") + "...");

        isRunning = false; // *** Signal listener thread to stop FIRST ***

        closeResources();

        // Notify listener (Controller) *after* cleanup, on the EDT
        final String title = (this.username != null ? this.username + "'s" : "Client") + " - Disconnected";
        notifyListenersOnEDT(l -> {
            l.setWindowTitle(title);
            l.updateUserList(Collections.emptyList()); // Clear user list via listener
            l.onDisconnect(); // Signal disconnection is complete
            System.out.println("UI listener notified of disconnection for " + username);
        });

        System.out.println("Client " + (username != null ? username : "") + " disconnected successfully.");
    }

    // Helper method to close network resources quietly
    private void closeResources () {
        System.out.println("Closing network resources for " + (username != null ? username : "client") + "...");
        // Close streams first (PrintWriter before BufferedReader)
        if (out != null) {
            try {
                out.close(); // This also flushes
            } catch (Exception e) {
                // Log quietly, as we are shutting down anyway
                System.err.println("Error closing output stream: " + e.getMessage());
            } finally {
                out = null;
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                System.err.println("Error closing input stream: " + e.getMessage());
            } finally {
                in = null;
            }
        }
        // Finally, close the socket
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                System.out.println("Socket closed for " + (username != null ? username : "client") + ".");
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            } finally {
                socket = null;
            }
        }
        System.out.println("Network resources closed for " + (username != null ? username : "client") + ".");
    }

    // --- Getters ---
    public String getUsername () {
        return this.username;
    }

    public boolean isRunning () {
        return this.isRunning;
    }
}