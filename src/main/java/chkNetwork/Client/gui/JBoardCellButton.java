package chkNetwork.Client.gui;

import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkMVC.chModel.Checkers.Position;

import javax.swing.*;
import java.awt.*;

/**
 * JBoardCellButton is a helper class that
 * keeps track of all the possible buttons
 * the user can click on the board.
 * <p>
 * it then helps tell the user where each button is so they
 * can send a message
 */
public class JBoardCellButton extends JButton {
    private final Position position;
    private PIECE_TEAM teamValue = null;
    private boolean isKing = false;

    Color COLOR_EVEN_SQUARE = new Color(0xEDCFA9);
    Color COLOR_ODD_SQUARE = new Color(0xE67554);

    public JBoardCellButton (Position position) {
        this.position = position;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(true);
    }

    public void setPieceInfo (PIECE_TEAM teamValue, boolean isKing) {
        this.teamValue = teamValue;
        this.isKing = isKing;
        repaint();
    }

    public void clearPiece () {
        this.teamValue = null;
        this.isKing = false;
        repaint();
    }

    public Position getPosition () {
        return position;
    }

    @Override
    protected void paintComponent (Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        g2.setColor(new Color(0x61FFE7C5, true));

        g2.fillRect(7, 7, getHeight() - 7, getWidth() - 7);


        // Base the background color on the position
        g2.setColor(((position.getY() + position.getX()) % 2 == 0) ? COLOR_EVEN_SQUARE : COLOR_ODD_SQUARE);


        int fillsize = Math.min(getHeight(), getWidth());


        g2.fillRect(0, 0, getWidth(), getHeight());

        if (teamValue != null && teamValue != PIECE_TEAM.SPECTATOR) {
            int margin = 10;
            int diameter = Math.min(getWidth(), getHeight()) - 2 * margin;

            // Draw shadow
            g2.setColor(new Color(0, 0, 0, 100)); // Semi-transparent black for shadow
            g2.fillOval(margin + 3, margin + 3, diameter, diameter); // Offset shadow

            // Piece base color
            if (teamValue == PIECE_TEAM.WHITE) {
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(Color.BLACK);
            }
            g2.fillOval(margin, margin, diameter, diameter);

            // King marker (simple crown color ring or symbol)
            if (isKing) {
                g2.setColor(Color.YELLOW); // Gold ring to indicate king
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(margin, margin, diameter, diameter);

                // Optional: draw a crown shape or symbol
                g2.setFont(new Font("Serif", Font.BOLD, 16));
                g2.drawString("♕", getWidth() / 2 - 5, getHeight() / 2 + 5);
            }
        }

        g2.dispose();
    }
}