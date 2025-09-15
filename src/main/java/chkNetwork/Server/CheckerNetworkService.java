package chkNetwork.Server;

import chkMVC.chModel.Checkers.BoardModel;
import chkMVC.chModel.Checkers.CheckersGameModel;
import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkMVC.chModel.Checkers.Pieces.AbstractPiece;
import chkMVC.chModel.Checkers.Position;
import chkNetwork.SERVER_RESPONSE_CODES;

import java.io.*;
import java.net.*;
import java.util.*;

public class CheckerNetworkService {

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false; // Use volatile for visibility across threads

    private final Set<ClientHandlerThread> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, ClientHandlerThread> clients = Collections.synchronizedMap(new HashMap<>());

    //Keep a reference to specific Client handler threads.
    private volatile ClientHandlerThread host = null;
    private volatile ClientHandlerThread white = null;
    private volatile ClientHandlerThread black = null;


    private CheckersGameModel authGameModel; // Keep track of an internal game
    private boolean isGameActive;
    private final Object gameLock = new Object(); //A lock for game state listener;.

    //Initializer Method.
    public CheckerNetworkService (int port) {
        this.port = port;
    }

    //Have a start function. this opens up the server for accepting connections.
    public void start () throws IOException {
        if (isRunning) {
            System.out.println("Server is already running on port " + port);
            return;
        }
        System.out.println("Starting Checkers server on port " + port + "...");
        serverSocket = new ServerSocket(port);
        isRunning = true; // Set running flag *before* starting the accept loop

        System.out.println("Server start sequence complete. Entering accept loop...");
        run(); // Accept loop runs in the thread that called start()
        System.out.println("Server accept loop has exited.");
    }

    void resetAuthGameModel () {
        synchronized (gameLock) {
            System.err.println("Warning: Restting the game.");
            this.authGameModel = null;
            this.isGameActive = false;
        }
    }


    //This function stops the serverfrom running.
    public void stop () {
        if (!isRunning) {
            System.out.println("Server is not running.");
            return;
        }

        System.out.println("Stopping Checkers server...");
        isRunning = false; // Signal the accept loop and handlers to stop

        // Close the server socket to interrupt the accept() call
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                System.out.println("Closing server socket...");
                serverSocket.close(); // This will cause accept() to throw a SocketException
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        //Close out the server threads
        Set<ClientHandlerThread> handlersToClose;
        synchronized (clientHandlers) {
            handlersToClose = new HashSet<>(clientHandlers);
        }
        System.out.println("Closing " + handlersToClose.size() + " client connections...");
        for (ClientHandlerThread handler : handlersToClose) {
            handler.closeConnection("Server shutting down");
        }

        // Clear the collections after attempting to close all handlers
        clientHandlers.clear();
        clients.clear();

        System.out.println("Server stopped.");
    }

    // This method runs the accept loop in the thread that calls it (e.g., the main thread)
    public void run () {
        System.out.println("Server accept loop started in thread: " + Thread.currentThread().getName());
        while (isRunning) {
            try {
                acceptSocketAndStartThread();
            } catch (SocketException e) {
                if (!isRunning) {
                    System.out.println("Server socket closed, accept loop terminating normally.");
                } else {
                    System.err.println("SocketException in accept loop (Server still running): " + e.getMessage());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {

                    }
                }
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("IOException in accept loop: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Server accept loop finished in thread: " + Thread.currentThread().getName());
    }

    private void acceptSocketAndStartThread () throws IOException {
        // This blocks until a connection arrives or the socket is closed
        Socket clientSocket = serverSocket.accept();
        System.out.println("Connection received from " + clientSocket.getRemoteSocketAddress());

        //Handle the client in a new thread.
        ClientHandlerThread handlerRunnable = new ClientHandlerThread(clientSocket, this);
        Thread clientThread = new Thread(handlerRunnable);
        clientThread.start(); // Start the handler in its own thread
    }

    //Handle Registering a client.
    void registerClient (ClientHandlerThread handler, String username) {
        synchronized (clients) {
            if (clients.containsKey(username)) {
                System.err.println("Attempt to register duplicate username: " + username);
                handler.closeConnection("Username already taken");
                return;
            }
            clients.put(username, handler);
        }
        synchronized (clientHandlers) {
            clientHandlers.add(handler);
        }
        assignRoles(handler);
        System.out.println(username + " successfully registered. Total clients: " + clients.size());
        broadcastUserListUpdate("User list updated after " + username + " joined.");
    }

    void unregisterClient (ClientHandlerThread handler) {
        String username = handler.getUsername();
        boolean removedClient = false;
        boolean removedHandler = false;
        if (username != null) {
            synchronized (clients) {
                removedClient = (clients.remove(username) != null);
            }
        }
        synchronized (clientHandlers) {
            removedHandler = clientHandlers.remove(handler);
        }
        releaseRoles(handler); // Add this method if needed
        if (removedClient || removedHandler) {
            System.out.println((username != null ? username : "Unknown user (" + handler.hashCode() + ")") + " unregistered. Remaining clients: " + clients.size());

            broadcastUserListUpdate("User list updated after " + (username != null ? username : "a user") + " left.");

        } else {
            System.out.println("Attempted to unregister a client that was already removed: " + (username != null ? username : "unknown user (" + handler.hashCode() + ")"));
        }

        //If the server is now empty restart the game.
        synchronized (clientHandlers) {
            if (clientHandlers.size() <= 0) {
                resetAuthGameModel();
            }

        }

    }

    // releasing roles from the network:
    private synchronized void releaseRoles (ClientHandlerThread handler) {
        if (handler == host) {
            host = null;
            System.out.println((handler.getUsername() != null ? handler.getUsername() : "User") + " released host role.");
        }
        if (handler == white) {
            white = null;
            System.out.println((handler.getUsername() != null ? handler.getUsername() : "User") + " released white role.");
        }
        if (handler == black) {
            black = null;
            System.out.println((handler.getUsername() != null ? handler.getUsername() : "User") + " released black role.");
        }
    }


    private void broadcastUserListUpdate (String reason) {
        List<String> currentUsers;
        synchronized (clients) {
            currentUsers = new ArrayList<>(getConnectedUsernamesWithRoles());
        }

        ServerResponse userListUpdate = ServerResponse.create(SERVER_RESPONSE_CODES.CLIENT_LIST, currentUsers);
        broadcastMessage(userListUpdate, null);
    }

    // Broadcast a message to all of the users.
    void broadcastMessage (ServerResponse message, ClientHandlerThread sender) {
        Set<ClientHandlerThread> handlersSnapshot;
        synchronized (clientHandlers) {
            handlersSnapshot = new HashSet<>(clientHandlers);
        }

        int i = 0;
        for (ClientHandlerThread handler : handlersSnapshot) {
            handler.sendServerResponse(message);
            i++;
        }

        //Log the message being sent to each client and the number of clients said message was sent to.
        System.out.println(message + " -> sent to (" + i + ") clients.");
    }

    //Assigning roles to the threads. this allows things like only the host starting a game.
    //And also only a client moving certain pieces.
    private synchronized void assignRoles (ClientHandlerThread newHandler) {
        if (host == null) {
            host = newHandler;
            newHandler.sendServerResponse(ServerResponse.create(SERVER_RESPONSE_CODES.ROLE_ASSIGN, List.of("H")));
            System.out.println(newHandler.getUsername() + " assigned as host.");
        }

        if (white == null) {
            white = newHandler;
            newHandler.sendServerResponse(ServerResponse.create(SERVER_RESPONSE_CODES.ROLE_ASSIGN, List.of("W")));
            System.out.println(newHandler.getUsername() + " assigned as White.");
        } else if (black == null) {
            black = newHandler;
            newHandler.sendServerResponse(ServerResponse.create(SERVER_RESPONSE_CODES.ROLE_ASSIGN, List.of("B")));
            System.out.println(newHandler.getUsername() + " assigned as Black.");
        } else {
            newHandler.sendServerResponse(ServerResponse.create(SERVER_RESPONSE_CODES.ROLE_ASSIGN, List.of("S")));
            System.out.println(newHandler.getUsername() + " is spectating.");
        }
    }

    public int getPort () {
        return port;
    }


    //This returns a version of the client list that includes the roles of each user.
    //This lets the users know who is who.
    public Set<String> getConnectedUsernamesWithRoles () {
        synchronized (clients) {
            Set<String> alteredClientList = new HashSet<>();

            String black = getBlackUsername();
            String white = getWhiteUsername();
            String host = getHostusername();

            for (String username : clients.keySet()) {
                StringBuilder label = new StringBuilder(username);

                if (black != null && username.equals(black)) {
                    label.append(" (Black)");
                }
                if (host != null && username.equals(host)) {
                    label.append(" (Host)");
                }
                if (white != null && username.equals(white)) {
                    label.append(" (White)");
                }

                alteredClientList.add(label.toString());
            }

            return alteredClientList;
        }
    }

    public int getUserCount () {
        synchronized (clientHandlers) {
            return clientHandlers.size();
        }
    }

    public String getWhiteUsername () {
        return white != null ? white.getUsername() : null;
    }

    public String getBlackUsername () {
        return black != null ? black.getUsername() : null;
    }

    public String getHostusername () {
        return host != null ? host.getUsername() : null;
    }


    public void assignRole (String player, PIECE_TEAM newTeamRole) {

        if (clients.containsKey(player)) System.out.println("Player is a client. can assign " + newTeamRole.toString());

        if (newTeamRole == PIECE_TEAM.WHITE) {
            if (white != null)
                white.sendServerResponse(new ServerResponse(SERVER_RESPONSE_CODES.ROLE_ASSIGN, List.of("S"), "assign as spectator."));
            this.white = clients.get(player);
            white.sendServerResponse(new ServerResponse(SERVER_RESPONSE_CODES.ROLE_ASSIGN, List.of("W"), "assign new one as color"));
        }

        if (newTeamRole == PIECE_TEAM.BLACK) {
            if (black != null)
                black.sendServerResponse(new ServerResponse(SERVER_RESPONSE_CODES.ROLE_ASSIGN, List.of("S"), "assign as spectator."));
            this.black = clients.get(player);
            black.sendServerResponse(new ServerResponse(SERVER_RESPONSE_CODES.ROLE_ASSIGN, List.of("B"), "assign new one as color"));
        }

        broadcastUserListUpdate("roles changed.");

    }

    void startGame () {
        synchronized (gameLock) {
            if (isGameActive) {
                System.out.println("Game Already Started");
                return;
            }

            System.out.println("Initializing Server-Side game model.");
            BoardModel boardModel = new BoardModel(8);
            this.authGameModel = new CheckersGameModel(boardModel);


            ServerGameListener serverGameListener = new ServerGameListener(this);

            this.authGameModel.addListener(serverGameListener);

            this.isGameActive = true;

            System.out.println("Server-side game initialzied. ");

            List<String> startPayload = List.of(authGameModel.getCurrentTurn().name());

            broadcastMessage(ServerResponse.create(SERVER_RESPONSE_CODES.GAME_START, startPayload), null);
        }
    }

    //The AttemptMove function looks over a high level check of the move in question.
    //It will check
    public ServerResponse attemptMove (String requestingUsername, Position from, Position to) {

        synchronized (gameLock) {

            if (!isGameActive || authGameModel == null) //Check if the conditions are right to take in a move.
            {
                System.err.println("Move: Rejected. Game not active, request from " + requestingUsername);
                return ServerResponse.create(SERVER_RESPONSE_CODES.ERROR, List.of("GameNotActiveError", "Game was not active when sending move."));
            }


            PIECE_TEAM expectedTurn = authGameModel.getCurrentTurn();


            ClientHandlerThread requestingHandler = clients.get(requestingUsername);
            PIECE_TEAM requestingPlayerTeam = PIECE_TEAM.SPECTATOR;


            if (requestingHandler == null) {
                System.err.println("Error; requested handler not found in clients");
                return ServerResponse.create(SERVER_RESPONSE_CODES.ERROR, List.of("Not A Player", "You Arent a player! Just wait!"));
            }

            if (requestingHandler == white) {
                requestingPlayerTeam = PIECE_TEAM.WHITE;
            } else if (requestingHandler == black) {
                requestingPlayerTeam = PIECE_TEAM.BLACK;
            }

            if (requestingPlayerTeam != expectedTurn) {
                System.err.println("Move Rejected: Out of turn; " + expectedTurn.toString() + " <- expected this team to go.");
                return ServerResponse.create(SERVER_RESPONSE_CODES.ERROR, List.of("Wait For your turn!", "It wasnt yet your turn. wait for the other player to go!"));
            }

            Optional<AbstractPiece> pieceOptional = authGameModel.getBoardModel().getPieceOptional(from);
            if (pieceOptional.isEmpty()) {
                System.err.println("Error: peice does not exist. ");
                return ServerResponse.create(SERVER_RESPONSE_CODES.ERROR, List.of("This Piece Doesnt Exist!", "You tried moving an empty piece..."));
            }

            if (!validatePieceOwnership(pieceOptional.get(), requestingPlayerTeam)) {
                System.err.println("Error: piece is not on expected team.");
                return ServerResponse.create(SERVER_RESPONSE_CODES.ERROR, List.of("Not your piece!", "You dont own that piece...."));
            }


            boolean isValidMove = authGameModel.canMakeMove(from, to);

            if (!isValidMove) {
                System.err.println("Error move is not a valid move.");
                return ServerResponse.create(SERVER_RESPONSE_CODES.ERROR, List.of("Invalid Move", "Something About the game didnt like your move... try again?"));
            }

            authGameModel.makeMove(from, to);


        }


        return ServerResponse.create(SERVER_RESPONSE_CODES.SUCCESS, List.of("Move Success!", "You Made your move!"));


    }


    boolean validatePieceOwnership (AbstractPiece piece, PIECE_TEAM team) {
        return piece.getTeam() == team;
    }


    public CheckersGameModel getAuthGameModel () {
        return this.authGameModel;
    }

    public void endGameDueToResignation (PIECE_TEAM winningTeam) {
        synchronized (gameLock) {
            if (!isGameActive) {
                System.out.println("No active game to end due to resignation.");
                return;
            }

            System.out.println("Ending game due to resignation. Winner: " + winningTeam);

            // Notify all clients about the game end
            ServerResponse gameEndResponse = new ServerResponse(SERVER_RESPONSE_CODES.GAME_END, List.of(winningTeam.name()), "Game ended due to resignation.");
            broadcastMessage(gameEndResponse, null);

            // Reset game state
            isGameActive = false;
            resetAuthGameModel();
        }
    }
}
