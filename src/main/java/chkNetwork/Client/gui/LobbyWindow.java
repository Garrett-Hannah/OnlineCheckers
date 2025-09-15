package chkNetwork.Client.gui;// In chkNetwork.Client.gui.LobbyWindow.java

// import chkMVC.chModel.Checkers.PIECE_TEAM; // Keep if needed for button actions
// import chkNetwork.CLIENT_REQUEST_CODES; // Remove
// import chkNetwork.Client.ClientEventListener; // Remove
// import chkNetwork.Client.ClientModel; // Remove

import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkNetwork.Client.ApplicationController; // Import the controller

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;


public class LobbyWindow {

    private JFrame frame;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JTextArea messageArea;
    private JLabel userCountLabel;
    private JLabel statusLabel; // Added for team/status info
    private JSplitPane splitPane;
    private JScrollPane userListScrollPane;
    private JScrollPane messageScrollPane;
    private JPanel userPanel;
    private JTextField messageInputField;
    private JButton sendButton;
    private JPanel buttonPanel; // Special stuff for host controls.

    //Store a refernce to the appController.
    private final ApplicationController controller;

    public LobbyWindow (ApplicationController controller) {
        this.controller = controller;
        initializeGUI();
    }

    private void initializeGUI () {
        frame = new JFrame("Lobby - Waiting for Connection..."); // Initial title
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);


        userPanel = new JPanel(new BorderLayout());
        userCountLabel = new JLabel(" (0) clients connected.");
        userCountLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        userPanel.add(userCountLabel, BorderLayout.NORTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userListScrollPane = new JScrollPane(userList);
        userPanel.add(userListScrollPane, BorderLayout.CENTER);

        // Status Label
        statusLabel = new JLabel("Status: Not Assigned");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        userPanel.add(statusLabel, BorderLayout.SOUTH); // Add status label

        // Right Panel - Message Area + Input Field
        JPanel messagePanel = new JPanel(new BorderLayout());
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setBorder(BorderFactory.createTitledBorder("Messages"));
        messagePanel.add(messageScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        messageInputField = new JTextField();
        messageInputField.setBorder(BorderFactory.createTitledBorder("Type a message"));
        sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(80, 40));
        inputPanel.add(messageInputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        messagePanel.add(inputPanel, BorderLayout.SOUTH);

        // Split Pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userPanel, messagePanel);
        splitPane.setDividerLocation(180); // Adjusted for status label
        splitPane.setResizeWeight(0.3);

        // Button Panel (Host Controls)
        buttonPanel = new JPanel(new FlowLayout());
        JButton startGameButton = new JButton("Start Game");
        JButton assignWhiteButton = new JButton("Assign White");
        JButton assignBlackButton = new JButton("Assign Black");
        buttonPanel.add(startGameButton);
        buttonPanel.add(assignWhiteButton);
        buttonPanel.add(assignBlackButton);
        buttonPanel.setVisible(false); // Initially hidden

        // Add components to frame
        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        //Listeners are supposed to call on controller,
        ActionListener sendAction = e -> {
            controller.sendChatMessage(messageInputField.getText());
            messageInputField.setText("");
        };
        messageInputField.addActionListener(sendAction);
        sendButton.addActionListener(sendAction);

        startGameButton.addActionListener(e -> controller.requestStartGame());
        assignWhiteButton.addActionListener(e -> controller.requestAssignTeam(userList.getSelectedValue(), PIECE_TEAM.WHITE));
        assignBlackButton.addActionListener(e -> controller.requestAssignTeam(userList.getSelectedValue(), PIECE_TEAM.BLACK));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing (WindowEvent windowEvent) {
                // Ask the controller to handle disconnection
                controller.disconnect();
            }
        });
    }

    public void showWindow () {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public void hideWindow () {
        SwingUtilities.invokeLater(() -> frame.setVisible(false));
    }

    public void disposeWindow () {
        SwingUtilities.invokeLater(() -> frame.dispose());
    }

    public boolean isWindowVisible () {
        return frame != null && frame.isVisible();
    }

    public JFrame getFrame () {
        return frame;
    }

    public void displayMessage (String message) {
        SwingUtilities.invokeLater(() -> {
            if (messageArea != null) {
                messageArea.append(message + "\n");
                messageArea.setCaretPosition(messageArea.getDocument().getLength());
            }
        });
    }

    public void refreshUserList (List<String> users) {
        SwingUtilities.invokeLater(() -> {
            if (userListModel != null && userCountLabel != null && users != null) {
                userCountLabel.setText("(" + users.size() + ") clients connected.");
                List<String> sortedUsers = new ArrayList<>(users);
                sortedUsers.sort(String.CASE_INSENSITIVE_ORDER);
                userListModel.clear();
                for (String user : sortedUsers) {
                    userListModel.addElement(user);
                }
            }
        });
    }

    public void updateStatus (String status) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText("Status: " + status);
            }
        });
    }

    public void displayError (String title, String message) {
        SwingUtilities.invokeLater(() -> {
            if (frame != null && frame.isDisplayable()) {
                JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public void setTitle (String title) {
        SwingUtilities.invokeLater(() -> {
            if (frame != null) {
                frame.setTitle(title);
            }
        });
    }

    public void enableHostControls (boolean enable) {
        SwingUtilities.invokeLater(() -> {
            if (buttonPanel != null) {
                buttonPanel.setVisible(enable);
            }
        });
    }
}