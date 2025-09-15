package chkNetwork.Client.gui;

import chkMVC.chModel.Checkers.GameEventListener;
import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkMVC.chModel.Checkers.Pieces.AbstractPiece;
import chkMVC.chModel.Checkers.Pieces.KingPiece;
import chkMVC.chModel.Checkers.Position;
import chkNetwork.Client.ApplicationController; // Import

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * CheckersGameGUI
 * <p>
 * Helps Display the state
 * of the game. including some helpful things
 * like whos turn it is.
 * <p>
 * Also some important things like but not limited to:
 * Showing the other chat window:
 * Telling the controller to handle the two position inputs.
 */
public class CheckersGameGUI {

    private final Map<Position, JBoardCellButton> buttonMap = new HashMap<>();
    private JBoardCellButton firstButtonSelected = null;
    private final GameEventListener listener;

    private final ApplicationController appController;

    private JFrame frame;
    private JLabel statusLabel; // For turn/team info
    private JToggleButton viewChatButton;
    private JButton resignButton;
    private boolean lobbyVisible = true;

    // Take in the Application Controller.
    public CheckersGameGUI (ApplicationController appController) {
        this.appController = appController;

        // Listener implementation remains the same - it updates THIS GUI based on GAME MODEL events
        this.listener = new GameEventListener() {
            // onGameComplete, onMoveMade (maybe log?), onBoardUpdate, onTurnChange
            // Implementations look okay, onBoardUpdate is crucial
            @Override
            public void onGameComplete (PIECE_TEAM winningTeam) {
                SwingUtilities.invokeLater(() -> {
                    updateStatus("Game Over! Winner: " + winningTeam);
                    JOptionPane.showMessageDialog(frame, "Game Over! Winner: " + winningTeam, "Game Finished", JOptionPane.INFORMATION_MESSAGE);
                    // Disable board interaction maybe
                });
            }

            @Override
            public void onMoveMade (Position from, Position to) {
                // This is triggered by the *local* game model AFTER a move is applied.
                // We might not need specific UI action here if onBoardUpdate and onTurnChange cover it.
                System.out.println("GameGUI Listener: Move processed internally " + from + " -> " + to);
            }

            @Override
            public void onBoardUpdate (Map<Position, AbstractPiece> boardState) {

                System.err.println(boardState.size());

                // This seems correct - updates button appearances based on model state
                SwingUtilities.invokeLater(() -> {
                    System.out.println("GameGUI Listener: Updating board display...");
                    for (JBoardCellButton button : buttonMap.values()) {
                        button.clearPiece(); // Clear all first
                        button.setSelected(false); // Clear visual selection if any
                        button.setBorder(UIManager.getBorder("Button.border")); // Reset border
                    }
                    if (boardState != null) {
                        for (Map.Entry<Position, AbstractPiece> entry : boardState.entrySet()) {
                            Position p = entry.getKey();
                            AbstractPiece pieceInfo = entry.getValue();
                            JBoardCellButton button = buttonMap.get(p);
                            if (button != null && pieceInfo != null) {
                                button.setPieceInfo(pieceInfo.getTeam(), pieceInfo instanceof KingPiece);
                            } else {
                                System.err.println("Board update warning: Null button or piece for position " + p);
                            }
                        }
                    }
                    System.out.println("GameGUI Listener: Board display updated.");
                });
            }

            @Override
            public void onTurnChange (PIECE_TEAM currentTurn) { // Modify GameEventListener interface if needed
                SwingUtilities.invokeLater(() -> {

                    //Resign is only available when the other player is playing.
                    resignButton.setEnabled(currentTurn.getValue() == -appController.getTeam().getValue());

                    updateStatus("Current Turn: " + currentTurn + ((currentTurn == appController.getTeam()) ? " (Your Turn)" : ""));

                    System.out.println("GameGUI Listener: Turn changed to " + currentTurn);
                    // Clear selection when turn changes
                    if (firstButtonSelected != null) {
                        firstButtonSelected.setSelected(false);
                        firstButtonSelected.setBorder(UIManager.getBorder("Button.border")); // Reset border
                        firstButtonSelected = null;
                    }
                });
            }

            @Override
            public void onPieceRemoved (Position position) {
                System.out.println("Gui got a message that " + position + " Should be removed.");
            }
        };

        createAndShowGUI();
    }

    private void createAndShowGUI () {
        this.frame = new JFrame("Checkers Game");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Controller handles close
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        //Status label.
        statusLabel = new JLabel("Status: Game Starting...");
        statusLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        //Hide or show chat.
        viewChatButton = new JToggleButton("View chat");

        viewChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e) {
                lobbyVisible = !lobbyVisible;

                viewChatButton.setText(!lobbyVisible ? "Hide Chat" : "Show Chat");

                appController.setLobbyVisible(lobbyVisible);

            }


        });

        // Resign button
        resignButton = new JButton("Resign");
        resignButton.addActionListener(e -> appController.resignGame());

        topPanel.add(statusLabel);
        topPanel.add(viewChatButton);
        topPanel.add(resignButton);

        frame.add(topPanel, BorderLayout.NORTH);


        JPanel boardPanel = buildBoardPanel(8);
        JPanel paddedPanel = new JPanel(new BorderLayout());
        paddedPanel.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        paddedPanel.add(boardPanel, BorderLayout.CENTER);

        frame.add(paddedPanel, BorderLayout.CENTER);
        frame.setPreferredSize(DEFAULT_FRAME_SIZE);
        frame.pack();
        frame.setLocationRelativeTo(null);


        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing (java.awt.event.WindowEvent windowEvent) {
                appController.disconnect(); // Ask controller to disconnect
            }
        });

        frame.setVisible(true); // Make visible upon creation
    }

    //Set Up the main panel for building. uses the JBoardCellButton class to put postions.
    private JPanel buildBoardPanel (int size) {
        JPanel boardPanel = new JPanel(new GridLayout(size, size)) {
            @Override
            public Dimension getPreferredSize () {
                Container parent = getParent();
                if (parent == null) return new Dimension(600, 600);
                int w = parent.getWidth();
                int h = parent.getHeight();
                int side = Math.min(w - 2 * PADDING, h - 2 * PADDING - statusLabel.getPreferredSize().height); // Adjust for padding/status
                return new Dimension(side, side);
            }
        };
        boardPanel.setBackground(Color.DARK_GRAY); // Background for gaps

        for (int row = size - 1; row >= 0; row--) { // Iterate rows 7 down to 0 for standard board layout
            for (int col = 0; col < size; col++) { // Iterate cols 0 to 7
                try {
                    // Position constructor expects 0-based index now based on its code
                    Position position = new Position(col, row);
                    JBoardCellButton button = new JBoardCellButton(position);
                    button.addActionListener(e -> handleButtonClick(button));
                    boardPanel.add(button);
                    buttonMap.put(position, button);
                } catch (IllegalArgumentException e) {
                    System.err.println("Error creating button/position for " + col + "," + row + ": " + e.getMessage());
                    // Add a placeholder or skip
                    boardPanel.add(new JLabel("ERR"));
                }
            }
        }
        return boardPanel;
    }


    private void handleButtonClick (JBoardCellButton clickedButton) {
        // Logic for selecting first/second piece and calling controller
        Position clickedPos = clickedButton.getPosition();
        System.out.println("Board click at: " + clickedPos);

        if (firstButtonSelected == null) {
            // Check if the clicked square has a piece belonging to the player? (Handled by server, but local check is nice)

            // Select the first piece
            firstButtonSelected = clickedButton;
            firstButtonSelected.setSelected(true); // Visually indicate selection (optional)
            firstButtonSelected.setBorder(BorderFactory.createLineBorder(COLOR_SELECTION_HIGHLIGHT, 3));
            System.out.println("Selected piece at " + clickedPos);
            // TODO: Optionally highlight valid moves from here? (Requires game logic access)

        } else {
            // Second click: Attempt the move via the controller
            Position from = firstButtonSelected.getPosition();
            Position to = clickedPos;

            System.out.println("Attempting move from " + from + " to " + to);

            // Reset visual selection immediately (server confirmation will update board state)
            firstButtonSelected.setSelected(false);
            firstButtonSelected.setBorder(UIManager.getBorder("Button.border")); // Reset border


            // *** Call the ApplicationController to send the move request ***
            appController.attemptMove(from, to);

            // Clear selection regardless of move validity (wait for server)
            firstButtonSelected = null;
        }
    }


    public GameEventListener getEventListener () {
        return this.listener;
    }

    public void setTitle (String s) {
        SwingUtilities.invokeLater(() -> this.frame.setTitle(s));
    }

    public void disposeWindow () {
        SwingUtilities.invokeLater(() -> this.frame.dispose());
    }

    public boolean isWindowVisible () {
        return frame != null && frame.isVisible();
    }

    public JFrame getFrame () {
        return frame;
    }

    public void updatePlayerStatus (PIECE_TEAM team) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                String currentTurnText = statusLabel.getText().split("\\|")[0].trim(); // Preserve turn info
                statusLabel.setText(currentTurnText + " | Your Team: " + team);
            }
        });
    }

    public void updateStatus (String text) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText("Status: " + text);
            }
        });
    }

    public void showError (String title, String message) {
        SwingUtilities.invokeLater(() -> {
            if (frame != null && frame.isDisplayable()) {
                JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public void displayChatMessage (String message) {
        // Add a chat area to the game GUI or ignore
        System.out.println("Game Chat: " + message); // Placeholder
        // if (gameChatArea != null) gameChatArea.append(message + "\n");
    }


    private static final Color COLOR_SELECTION_HIGHLIGHT = Color.CYAN;
    private static final int PADDING = 10;
    private static final Dimension DEFAULT_FRAME_SIZE = new Dimension(700, 750);

}