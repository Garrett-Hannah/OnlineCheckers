package chkMVC.chModel.Checkers;

import chkMVC.chModel.Checkers.Pieces.AbstractPiece;
import chkMVC.chModel.Math.Vector2i;

import java.util.*;


//This will not run a full game but it does provide data for working well.
public class BoardModel {

    private final Map<Position, AbstractPiece> boardSpace; // Use Map To work.
    private final int width;
    private final int height;

    public BoardModel (int width, int height) {
        if (width < 8 || height < 8) {
            throw new IllegalArgumentException("Board dimensions must be positive.");
        }
        this.width = width;
        this.height = height;
        this.boardSpace = new HashMap<>();
        // TODO: Add initial piece setup
    }

    public BoardModel (int size) {
        this(size, size); // Square board constructor
    }

    public int getWidth () {
        return this.width;
    }

    public int getHeight () {
        return this.height;
    }

    public int getNumberOfPieces () {
        return boardSpace.size();
    }

    // Get piece using Optional to avoid null checks elsewhere
    public Optional<AbstractPiece> getPieceOptional (Position position) {
        return Optional.ofNullable(boardSpace.get(position));
    }

    // Get piece, returning null (less safe, but sometimes needed)
    public AbstractPiece getPieceAt (Position position) {
        return boardSpace.get(position);
    }


    public boolean isOccupied (Position position) {
        return boardSpace.containsKey(position);
    }

    // Internal method to place a piece - used during setup or potentially moves
    public void addPiece (AbstractPiece piece, Position position) {
        boardSpace.put(position, piece);
    }

    // Removes a piece - returns true if a piece was removed
    public boolean removePiece (Position position) {

        return boardSpace.remove(position) != null;
    }

    // Moves a piece - handles removal and placement, checks if 'from' is occupied
    //Warning: not used when handling turn updates.
    public void movePiece (Position from, Position to) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("Cannot move piece to the same position: " + from);
        }

        AbstractPiece piece = getPieceAt(from);

        if (piece == null) {
            throw new IllegalStateException("No piece found at starting position: " + from);
        }
        if (isOccupied(to)) {
            throw new IllegalStateException("Cannot move to occupied position: " + to);
        }

        // Remove from old, place at new, update piece's internal state
        boardSpace.remove(from);
        boardSpace.put(to, piece);
    }


    @Override
    public String toString () {
        // Basic toString, printBoard is better for visualization
        return "BoardModel{" +
                "width=" + width +
                ", height=" + height +
                ", pieces=" + boardSpace.size() +
                '}';
    }

    // Using for debugging... (Keep this for testing/console use)
    public void printBoard () {
        System.out.println("Board State (" + width + "x" + height + "):");
        // Assuming (0,0) is top-left for printing
        // Column Headers (A, B, C...)
        System.out.print("  ");
        for (int j = 0; j < width; j++) {
            System.out.print(" " + (char) ('A' + j));
        }
        System.out.println();
        System.out.print("  ");
        for (int j = 0; j < width; j++) {
            System.out.print("--");
        }
        System.out.println("-");


        for (int i = 0; i < height; i++) { // Rows 1 to height
            System.out.printf("%d|", i + 1); // Row label

            for (int j = 0; j < width; j++) { // Columns 1 to width
                try {
                    Position currentPos = new Position(j, i);
                    Optional<AbstractPiece> pieceOpt = getPieceOptional(currentPos);
                    if (pieceOpt.isPresent()) {
                        AbstractPiece piece = pieceOpt.get();
                        System.out.print(" " + (piece.getTeam() == PIECE_TEAM.WHITE ? 'W' : 'B'));
                    } else {
                        System.out.print(" .");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.print(" X"); // Should not happen if loop is correct
                }
            }
            System.out.println(" |"); // End of row
        }
        System.out.print("  ");
        for (int j = 0; j < width; j++) {
            System.out.print("--");
        }
        System.out.println("-");
    }

    public Position createPosition (int x, int y) throws IllegalArgumentException {
        Position newPos = null;
        try {
            newPos = new Position(x, y);
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Trying to Initialize A position out of bounds");
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid Positon Initialized.");
        }
        return newPos;
    }


    public Map<Position, AbstractPiece> getAllPieces () {
        return this.boardSpace;
    }
}