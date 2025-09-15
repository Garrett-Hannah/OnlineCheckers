package chkNetwork.Client;

import chkMVC.chModel.Checkers.BoardModel;
import chkMVC.chModel.Checkers.CheckersGameModel;
import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkMVC.chModel.Checkers.Position;
import chkNetwork.CLIENT_REQUEST_CODES;
import chkNetwork.Client.gui.CheckersGameGUI;
import chkNetwork.Client.gui.LobbyWindow;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ApplicationController implements ClientEventListener {

    private final ClientModel clientModel;
    private LobbyWindow lobbyWindow; // Can be null after game starts
    private CheckersGameGUI gameGUI; // Null until game starts
    private CheckersGameModel checkersGameModel; // Null until game starts

    private PIECE_TEAM clientTeam = PIECE_TEAM.SPECTATOR; // Track the client's role

    public ApplicationController (ClientModel clientModel) {
        this.clientModel = Objects.requireNonNull(clientModel, "ClientModel cannot be null");
        // Register this controller as the primary listener
        this.clientModel.addListener(this);
    }

    // Method to initialize and show the lobby
    public void startLobby () {
        if (lobbyWindow == null) {
            // Pass 'this' controller to the lobby window
            lobbyWindow = new LobbyWindow(this);
            lobbyWindow.showWindow(); // Add a method to make it visible
        } else {
            lobbyWindow.showWindow();
        }
    }

    //Run at the start of the program.
    public void attemptLogin (String username) {
        if (username == null || username.trim().isEmpty()) {
            if (lobbyWindow != null) {
                lobbyWindow.displayError("Login Error", "Username cannot be empty.");
            } else {
                System.err.println("Login attempt with no lobby window visible.");
            }
            return;
        }
        boolean success = clientModel.connectToServer(username);
        if (!success && lobbyWindow != null) {

        }

    }

    public void sendChatMessage (String message) {
        if (message != null && !message.trim().isEmpty()) {
            System.out.println("Controller sending CHAT request");
            clientModel.sendClientRequest(CLIENT_REQUEST_CODES.SEND_CHAT, Collections.singletonList(message));
        }
    }

    public void requestStartGame () {
        // Check if user is host (this logic might need refinement based on state)
        System.out.println("Controller sending START_GAME request");
        clientModel.sendClientRequest(CLIENT_REQUEST_CODES.HOST_BEGIN_GAME, Collections.emptyList());
    }

    public void requestAssignTeam (String selectedUser, PIECE_TEAM team) {
        if (selectedUser == null || team == null) {
            if (lobbyWindow != null) {
                lobbyWindow.displayError("Assignment Error", "No user selected or invalid team.");
            }
            return;
        }

        String targetUsername = selectedUser.split(" ")[0]; //Probably should be done better.

        System.out.println("Controller requesting assignment of " + targetUsername + " to " + team);
        clientModel.sendClientRequest(CLIENT_REQUEST_CODES.HOST_ASSIGN_TEAM, List.of(targetUsername, team.name())); // Use enum name
    }

    public void attemptMove (Position from, Position to) {
        if (clientTeam == PIECE_TEAM.SPECTATOR) {
            if (gameGUI != null) {
                gameGUI.showError("Move Error", "Spectators cannot move pieces."); // Delegate error display
            }
            System.err.println("Spectator attempted move.");
            return;
        }

        System.out.println("Controller sending MOVE_PIECE request for " + from + " -> " + to);
        clientModel.sendClientRequest(CLIENT_REQUEST_CODES.MOVE_PIECE, List.of(from.toString(), to.toString()));
    }

    //Close out the clientMOde.
    public void disconnect () {
        clientModel.disconnect();
    }


    //Implementation of gameEventListener.
    @Override
    public void onGameStart () {
        System.out.println("ApplicationController: Received onGameStart");
        if (gameGUI != null) {
            System.out.println("Game already seems to be started.");
            return;
        }

        // Create game components
        SwingUtilities.invokeLater(() -> {
            System.out.println("Initializing game components...");
            BoardModel boardModel = new BoardModel(8); // Standard 8x8 board
            checkersGameModel = new CheckersGameModel(boardModel); // Initialize with setup


            // Create the Game GUI, passing the controller needed for user actions
            gameGUI = new CheckersGameGUI(this); // Pass ApplicationController for board clicks

            //Add the gui to the boardModel as a listener.
            checkersGameModel.addListener(gameGUI.getEventListener());

            // Initial board update
            checkersGameModel.notifyGameListeners(l -> l.onBoardUpdate(checkersGameModel.getBoardModel().getAllPieces()));
            checkersGameModel.notifyGameListeners(l -> l.onTurnChange(checkersGameModel.getCurrentTurn()));


            System.out.println("Game GUI should be visible now.");
        });
    }

    public void setLobbyVisible (boolean visible) {
        if (visible) lobbyWindow.showWindow();
        else lobbyWindow.hideWindow();
    }

    @Override
    public void onTeamAssign (PIECE_TEAM team) {
        System.out.println("ApplicationController: Received onTeamAssign: " + team);
        this.clientTeam = (team == null) ? PIECE_TEAM.SPECTATOR : team;
        System.out.println("Client team set to: " + this.clientTeam);

        // Update relevant view(s) if they exist
        SwingUtilities.invokeLater(() -> {
            if (lobbyWindow != null) {
                // Maybe update a status label in the lobby?
                lobbyWindow.updateStatus("You are assigned as: " + this.clientTeam); // Add method
            }
            if (gameGUI != null) {
                // Update game GUI title or status bar
                gameGUI.updatePlayerStatus(this.clientTeam); // Add method
            }
        });
    }

    @Override
    public void showErrorMessage (String title, String message) {
        // Show error on the currently relevant window
        SwingUtilities.invokeLater(() -> {
            JFrame parent = (gameGUI != null && gameGUI.isWindowVisible()) ? gameGUI.getFrame() // Add getFrame() method
                    : (lobbyWindow != null && lobbyWindow.isWindowVisible()) ? lobbyWindow.getFrame() // Add getFrame() method
                    : null;
            if (parent != null) {
                JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
            } else {
                // Fallback if no window is visible/available
                System.err.println("Error [" + title + "]: " + message + " (No active window to display)");
                // Might show a standalone JOptionPane
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void setWindowTitle (String title) {
        // Update title on the relevant window
        SwingUtilities.invokeLater(() -> {
            if (gameGUI != null && gameGUI.isWindowVisible()) {
                gameGUI.setTitle(title);
            } else if (lobbyWindow != null && lobbyWindow.isWindowVisible()) {
                lobbyWindow.setTitle(title);
            }
        });
    }

    @Override
    public void appendMessage (String message) {
        // Append chat message only to lobby window if it's visible
        SwingUtilities.invokeLater(() -> {
            if (lobbyWindow != null) {
                lobbyWindow.displayMessage(message); // Rename appendMessage in LobbyWindow
            } else if (gameGUI != null && gameGUI.isWindowVisible()) {
                // Maybe the game GUI also has a mini-chat?
                gameGUI.displayChatMessage(message); // Add method if needed
            }
        });
    }

    @Override
    public void updateUserList (List<String> users) {
        // Update user list only in the lobby window if it's visible
        SwingUtilities.invokeLater(() -> {
            if (lobbyWindow != null && lobbyWindow.isWindowVisible()) {
                lobbyWindow.refreshUserList(users); // Keep method name
            }
        });
    }

    @Override
    public void setHostView (boolean isHost) {
        System.out.println("ApplicationController: setHostView(" + isHost + ")");
        // Enable/disable host controls only in the lobby window if it's visible
        SwingUtilities.invokeLater(() -> {
            if (lobbyWindow != null && lobbyWindow.isWindowVisible()) {
                lobbyWindow.enableHostControls(isHost); // Keep method name
            }
        });
    }

    // *** Implement the crucial move confirmation ***
    // This method belongs to ClientEventListener interface (add it if missing from previous step)
    @Override
    public void onServerMoveConfirmed (Position from, Position to) {
        System.out.println("ApplicationController: Received onServerMoveConfirmed: " + from + " -> " + to);
        if (checkersGameModel != null && gameGUI != null) {
            // Apply the move to the local game model
            SwingUtilities.invokeLater(() -> {
                try {
                    System.out.println("Applying server confirmed move to local model...");
                    // This should now handle piece removal/promotion internally if applicable
                    checkersGameModel.applyServerConfirmedMove(from, to);

                    System.out.println("Applied server move and notified game listeners (implicitly via model).");
                } catch (Exception e) {
                    System.err.println("Error applying server confirmed move in Controller: " + e.getMessage());
                    e.printStackTrace();
                    showErrorMessage("Game Error", "Failed to update board after server move: " + e.getMessage());
                }
            });
        } else {
            System.err.println("Controller received onServerMoveConfirmed but game components are null.");
        }
    }

    @Override
    public void onRoundUpdate (PIECE_TEAM nextTurn) {
        checkersGameModel.updateNewTurn(nextTurn);
    }

    @Override
    public void onServerRemoveConfirmed (Position removePosition) {
        if (checkersGameModel != null && gameGUI != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    System.out.println("Applying server confirmed move to local model...");

                    checkersGameModel.applyServerRemovePiece(removePosition);

                    System.out.println("Applied server move and notified game listeners (implicitly via model).");
                } catch (Exception e) {
                    System.err.println("Error applying server confirmed move in Controller: " + e.getMessage());
                    e.printStackTrace();
                    showErrorMessage("Game Error", "Failed to update board after server move: " + e.getMessage());
                }
            });
        } else {
            System.err.println("Controller received onServerMoveConfirmed but game components are null.");
        }

    }

    @Override
    public void onGameEnd (PIECE_TEAM team) {
        SwingUtilities.invokeLater(() -> {
            String message = (team != null)
                    ? team + " wins the game!"
                    : "It's a draw!";

            JOptionPane.showMessageDialog(
                    null,
                    message,
                    "Game Over",
                    JOptionPane.INFORMATION_MESSAGE
            );


            //Reset the local stuff and bring back to window.
            checkersGameModel = null;
            gameGUI.disposeWindow();
            gameGUI = null;
            lobbyWindow.showWindow();

        });
    }


    @Override
    public void onDisconnect () {
        System.out.println("ApplicationController: Received onDisconnect");
        // Clean up: Close windows, nullify references
        SwingUtilities.invokeLater(() -> {
            if (gameGUI != null) {
                gameGUI.disposeWindow(); // Add method to close/dispose
                gameGUI = null;
            }
            if (lobbyWindow != null) {
                // Show disconnected message before closing?
                lobbyWindow.displayError("Disconnected", "Connection to server lost or closed.");
                lobbyWindow.disposeWindow(); // Add method
                lobbyWindow = null;
            }
            // Reset game state if needed
            checkersGameModel = null;
            clientTeam = PIECE_TEAM.SPECTATOR;

            System.out.println("ApplicationController: Disconnect cleanup complete.");

        });
    }

    public PIECE_TEAM getTeam () {
        return this.clientTeam;
    }

    public void resignGame () {
        if (clientTeam == PIECE_TEAM.SPECTATOR) {
            if (gameGUI != null) {
                gameGUI.showError("Resign Error", "Spectators cannot resign from the game.");
            }
            System.err.println("Spectator attempted to resign.");
            return;
        }

        PIECE_TEAM winner = (clientTeam == PIECE_TEAM.WHITE) ? PIECE_TEAM.BLACK : PIECE_TEAM.WHITE; // Determine the winner based on the client's team

        System.out.println("Controller sending RESIGN_GAME request");
        clientModel.sendClientRequest(CLIENT_REQUEST_CODES.RESIGN_GAME, Collections.emptyList());

    }
}