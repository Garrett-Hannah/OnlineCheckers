package chkNetwork.Server;

import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkMVC.chModel.Checkers.Position;
import chkNetwork.CLIENT_REQUEST_CODES;
import chkNetwork.Client.ClientRequest;
import chkNetwork.SERVER_RESPONSE_CODES;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The ClientHandler Thread is the main source for handling the clients.
 * What it does is open up the socket then continually read through the
 * socket.
 * <p>
 * it will then respond accordingly, which the clients again will
 * handle in their own, unique way.
 * <p>
 * this is so that we can handle multiple clients.
 */
class ClientHandlerThread implements Runnable {

    private final Socket socket;
    private final CheckerNetworkService networkService; // Reference to the parent server
    private PrintWriter out;
    private BufferedReader in;
    private volatile String username; // Make username volatile as it's set after thread start
    private volatile boolean clientRunning = true;

    private Gson gson = new Gson();

    public ClientHandlerThread (Socket socket, CheckerNetworkService networkService) {
        this.socket = socket;
        this.networkService = networkService; // Store the server instance
    }

    public String getUsername () {
        return username;
    }


    public void sendServerResponse (ServerResponse message) {
        if (out != null && clientRunning) { // Check if output stream is ready and running
            try {
                String jsonResponse = gson.toJson(message);
                out.println(jsonResponse);
            } catch (Exception e) {
                System.err.println("Error resializing response to json format: " + e.getMessage());
            }
        }
    }


    /**
     * Run the Client Handler Thread. This endlessly listens from the client..
     */
    @Override
    public void run () {
        try {
            //Creates the in( for listening ) and the out (for sending)
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            ClientRequest receivedJoinRequest = gson.fromJson(in.readLine(), ClientRequest.class);

            if (receivedJoinRequest.getType() != CLIENT_REQUEST_CODES.JOIN) {
                System.out.println("Client disconnected before sending username.");
                return;
            }

            this.username = receivedJoinRequest.getPayload().getFirst();

            //register the client, via its username.
            networkService.registerClient(this, this.username);

            //Begin taking in messages.
            String message;
            while (clientRunning && (message = in.readLine()) != null) {

                //Handl the received message
                handleReceivedMessage(message);

            }
        } catch (SocketException e) {
            if (!clientRunning) {
                System.out.println("Client socket closed for " + (username != null ? username : "unknown user") + " as requested.");
            } else {
                System.err.println("SocketException for " + (username != null ? username : "unknown user") + ": " + e.getMessage() + " (Likely client disconnected abruptly)");
            }
        } catch (IOException e) {
            if (clientRunning) { // Avoid error message if we closed intentionally
                System.err.println("IOException for client " + (username != null ? username : "unknown user") + ": " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            closeConnection(null); //Clean up the connection after finish.
        }
        System.out.println("Client handler finished for: " + (username != null ? username : "unknown user"));
    }

    /**
     * Send a message to the client about why the connection was closed.
     *
     * @param reason
     */
    public void closeConnection (String reason) {
        if (!clientRunning) return; // Already closing/closed
        clientRunning = false; // Signal loops to stop

        System.out.println("Closing connection for " + (username != null ? username : "unknown user") + (reason != null ? ". Reason: " + reason : ""));

        // Unregister *before* closing socket if possible
        networkService.unregisterClient(this);

        try {
            if (socket != null && !socket.isClosed()) {
                // Maybe send a final "goodbye" message before closing?
                // sendMessage("SERVER: Disconnecting. " + (reason != null ? reason : ""));
                socket.close(); // This also closes associated streams (in/out)
            }
        } catch (IOException e) {
            System.err.println("Error closing socket for " + (username != null ? username : "unknown user") + ": " + e.getMessage());
        } finally {
            // Nullify resources
            in = null;
            out = null;
        }
    }


    //Function that handles anything recieved by the server.
    public void handleReceivedMessage (String message) {
        ClientRequest clientRequest = gson.fromJson(message, ClientRequest.class);
        CLIENT_REQUEST_CODES clientRequestCode = clientRequest.getType();

        switch (clientRequestCode) {
            case UNKNOWN_ERROR:
                handleUnknownError();
                break;

            case USER_LIST:
                handleUserListRequest();
                break;


            case SEND_CHAT:
                handleChatRequest(clientRequest);
                break;

            case HOST_BEGIN_GAME:
                handleHostBeginGameRequest(clientRequest);
                break;

            case RESIGN_GAME:
                handleResignGameRequest();
                break;

            case MOVE_PIECE:
                //Moving a piece takes in the initial and the final positions,
                //Then it attempts the move under the networkservice.

                synchronized (networkService) {
                    List<String> payload = clientRequest.getPayload();

                    Position from = Position.fromString(payload.get(1));
                    Position to = Position.fromString(payload.get(2));


                    ServerResponse request = networkService.attemptMove(this.username, from, to);
                    if (request.getType() != SERVER_RESPONSE_CODES.SUCCESS)
                        sendServerResponse(request);
                }

                break;

            case HOST_ASSIGN_TEAM:
                synchronized (networkService) {
                    List<String> requestPayload = clientRequest.getPayload();

                    System.out.println("Got a team assignment message...: " + requestPayload.toString());

                    if (!requestPayload.getFirst().equals(networkService.getHostusername())) {

                        this.sendServerResponse(new ServerResponse(SERVER_RESPONSE_CODES.ERROR, List.of("Host Error", "You are not the host."), "host error message."));
                        break;// if the sender is not the host. (shouldnt be possible anyway but oh well.

                    }


                    System.out.println(requestPayload.get(1) + requestPayload.get(2));

                    System.err.println("Error: Not implemented.");
                    networkService.assignRole(requestPayload.get(1), PIECE_TEAM.valueOf(requestPayload.get(2)));

                }
                break;


            default:
                System.out.println("Received Unimplemented Command. you should do that!");
                break;
        }
    }

    private void handleUnknownError () {
        ServerResponse unknownErrorResponse = new ServerResponse(SERVER_RESPONSE_CODES.ERROR, Collections.emptyList(), "Error parsing file.");
        networkService.broadcastMessage(unknownErrorResponse, null);
        System.err.println("ERROR RECEIVING MESSAGE!");
    }

    // Handle the case of a user list request
    private void handleUserListRequest () {
        System.out.println("Requested user list..");

        synchronized (networkService) {
            ArrayList<String> usernameSet = new ArrayList<>(networkService.getConnectedUsernamesWithRoles());
            ServerResponse userListResponse = new ServerResponse(SERVER_RESPONSE_CODES.CLIENT_LIST, usernameSet, "USER LIST RESPONSE!!!!!!");

            for (String u : usernameSet) {
                System.out.println(u);
            }

            networkService.broadcastMessage(userListResponse, this);
        }
    }

    // Handle chat message request
    private void handleChatRequest (ClientRequest clientRequest) {
        synchronized (networkService) {
            List<String> chatPayload = clientRequest.getPayload();
            System.out.println(chatPayload.getFirst());

            List<String> returnPayload = new ArrayList<>();
            returnPayload.add(chatPayload.getFirst());

            ServerResponse chat = new ServerResponse(SERVER_RESPONSE_CODES.CHAT_MESSAGE, chatPayload, chatPayload.getFirst());
            networkService.broadcastMessage(chat, this);
        }
    }

    // Handle host's game start request
    private void handleHostBeginGameRequest (ClientRequest clientRequest) {
        synchronized (networkService) {
            List<String> hostname = clientRequest.getPayload();
            System.out.println("Begin request asked by: " + hostname.getFirst());

            // Validate host asking for start is indeed the host:
            if (!ValidateAsHOST(hostname.getFirst())) {
                this.sendServerResponse(new ServerResponse(SERVER_RESPONSE_CODES.ERROR, List.of("Error: you are not the host"), "Error Message from server"));
                return;
            }

            if (networkService.getUserCount() < 2) {
                this.sendServerResponse(new ServerResponse(SERVER_RESPONSE_CODES.ERROR, List.of("Error: there are not enough players to start the game."), "Error message: not enough players"));
                return;
            }

            //Otherwise you can ask the network to start the game...
            networkService.startGame();


        }
    }

    private void handleResignGameRequest () {
        synchronized (networkService) {
            System.out.println("Received resign request from " + username);

            // Determine the opponent team
            PIECE_TEAM opponentTeam = ValidateAsWhite(username) ? PIECE_TEAM.BLACK : PIECE_TEAM.WHITE;

            // Notify the network service to end the game
            networkService.endGameDueToResignation(opponentTeam);
        }
    }

    // Handle move piece request
    private void handleMovePieceRequest (ClientRequest clientRequest) {
        synchronized (networkService) {
            System.out.println("Received a move request from " + clientRequest.getPayload().getFirst());
            List<String> payload = clientRequest.getPayload();

            String senderName = payload.getFirst();

            if (ValidateAsBlack(senderName) || ValidateAsWhite(senderName)) {
                Position p1 = Position.fromString(payload.get(1));
                Position p2 = Position.fromString(payload.get(2));

                System.out.println("Got a request to move from " + p1 + " to " + p2);
                System.out.println("Note this program is not validating the move. implement that!");

                synchronized (networkService) {
                    networkService.broadcastMessage(new ServerResponse(SERVER_RESPONSE_CODES.MOVE_PIECE, payload, "SendBack of the initial positoin."), null);
                }


            } else {
                System.err.println("WARNING: USER NAME IS NOT A VALID PLAYER!");
            }
        }
    }


    boolean ValidateAsWhite (String senderUsername) {
        synchronized (networkService) {
            return (networkService.getWhiteUsername() != null) && networkService.getWhiteUsername().equals(senderUsername);
        }
    }

    boolean ValidateAsBlack (String senderUsername) {
        synchronized (networkService) {
            return (networkService.getBlackUsername() != null) && networkService.getBlackUsername().equals(senderUsername);
        }
    }

    boolean ValidateAsHOST (String senderUsername) {
        synchronized (networkService) {
            return (networkService.getHostusername() != null) && networkService.getHostusername().equals(senderUsername);
        }
    }
}

