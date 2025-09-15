package chkMVC.chModel.Checkers;

import chkMVC.chModel.Math.Vector2i;

import java.util.Objects; // Use Objects.hash

public class Position {
    private final Vector2i position;

    static final int MIN_BOARD_HEIGHT = 8;
    static final int MAX_BOARD_HEIGHT = 8;


    /**
     * Creates a new Position.
     *
     * @param x The x-coordinate (column), 1-based.
     * @param y The y-coordinate (row), 1-based.
     * @throws IllegalArgumentException if x or y are out of board bounds [0, width/height -1].
     */
    public Position (int x, int y) {

        if (!isValidCoordinate(x)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid x position: %d. Must be from 1 and %d.", x, 8));
        }
        if (!isValidCoordinate(y)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid y position: %d. Must be from 1 and %d.", y, 8));
        }

        this.position = new Vector2i(x, y);
    }


    /**
     * Creates a new Position using Vector2i for coordinates and dimensions.
     *
     * @param positionVector Vector containing 0-based x and y coordinates.
     * @throws IllegalArgumentException if coordinates are out of bounds.
     */
    public Position (Vector2i positionVector) {
        this(positionVector.x, positionVector.y);
    }

    // Private helper for coordinate validation
    private boolean isValidCoordinate (int value) {
        return value >= 0 && value < 8;
    }

    // No setters needed as fields are final, promoting immutability

    @Override
    public boolean equals (Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position other = (Position) obj;
        // Positions are equal if coordinates are the same.
        // Board dimensions don't strictly need to match for equality,
        // but logically they should represent the same conceptual square.
        return this.getX() == other.getX() && this.getY() == other.getY();
        // Optional stricter check: && this.boardWidth == other.boardWidth && this.boardHeight == other.boardHeight;
    }

    @Override
    public int hashCode () {
        // Hash code depends only on coordinates for map lookups.
        return Objects.hash(this.getX(), this.getY());
        // Optional stricter hash: return Objects.hash(this.getX(), this.getY(), this.boardWidth, this.boardHeight);
    }

    @Override
    public String toString () {
        // Convert x to letters (A, B, ...)
        char column = (char) ('A' + this.getX()); // Adjust for 1-based index
        // Row is just the Y value
        int row = this.getY() + 1;
        // Return standard algebraic notation e.g., "A1", "H8"
        return String.format("%c%d", column, row);
    }

    public static Position fromString (String input) {
        if (input == null || input.length() < 2) {
            throw new IllegalArgumentException("Invalid position string: " + input);
        }

        // First char is column letter (e.g., 'A' → 0)
        char colChar = Character.toUpperCase(input.charAt(0));
        int x = colChar - 'A';

        // Rest of string is the row number (e.g., "8")
        int y;
        try {
            y = Integer.parseInt(input.substring(1)) - 1;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid row number in: " + input);
        }

        return new Position(x, y);
    }

    public int getY () {
        return this.position.y;
    }

    public int getX () {
        return this.position.x;
    }
}