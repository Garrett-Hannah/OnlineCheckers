package chkMVC.chModel.Checkers;

import chkMVC.chModel.Checkers.Pieces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public class CheckersGameModel {

    private List<GameEventListener> listeners = new ArrayList<>();
    private PIECE_TEAM currentTurn;
    private final BoardModel boardModel;

    // Setter for the View (GUI)
    public void addListener (GameEventListener listener) {
        this.listeners.add(listener);
    }

    public void removeClientEventListener (GameEventListener listener) {
        listeners.remove(listener);
    }

    public void notifyGameListeners (Consumer<GameEventListener> callback) {
        for (GameEventListener listener : listeners) {
            callback.accept(listener);
        }
    }


    public CheckersGameModel (BoardModel boardModel) {
        this(boardModel, true);
    }

    public CheckersGameModel (BoardModel boardModel, boolean initialSetup) {
        this.currentTurn = PIECE_TEAM.WHITE; // White moves first
        this.boardModel = boardModel;
        if (initialSetup) setupInitialBoard();
    }

    private void setupInitialBoard () {
        int boardSize = 8;
        setupBoardSideHelper(0, 2, PIECE_TEAM.WHITE);
        setupBoardSideHelper(5, 7, PIECE_TEAM.BLACK);
        notifyGameListeners(l -> l.onBoardUpdate(this.boardModel.getAllPieces()));
    }

    //This does the checker board pattern for placing pieces. starts @ the start (inclusive) and goes until the end. (inclusice)
    private void setupBoardSideHelper (int startRow, int endRow, PIECE_TEAM team) {
        int BOARD_SIZE_TEMP = 8;

        for (int y = startRow; y <= endRow; y++) {
            for (int x = 0; x < BOARD_SIZE_TEMP; x++) {

                if ((x + y) % 2 != 0) {
                    boardModel.addPiece(new SerfPiece(team), new Position(x, y));
                }
            }
        }
    }


    public boolean makeMove (Position from, Position to) {
        Optional<AbstractPiece> pieceOpt = boardModel.getPieceOptional(from);
        if (pieceOpt.isEmpty()) {
            return false;
        }

        AbstractPiece piece = pieceOpt.get();
        if (piece.getTeam() != currentTurn) {
            return false;
        }

        // Perform the move
        boardModel.movePiece(from, to);

        boolean isJumpMove = Math.abs(to.getX() - from.getX()) == 2;


        if (isJumpMove) {
            Position jumpedPositon = boardModel.createPosition((from.getX() + to.getX()) / 2, (from.getY() + to.getY()) / 2);

            boardModel.removePiece(jumpedPositon);
            notifyGameListeners(l -> l.onPieceRemoved(jumpedPositon));

            System.out.println("Removed @ " + jumpedPositon.toString());

            notifyGameListeners(l -> l.onMoveMade(from, to));

            if (getWinner() != null) notifyGameListeners(l -> l.onGameComplete(currentTurn));

            return true;
        }

        currentTurn = (currentTurn == PIECE_TEAM.WHITE) ? PIECE_TEAM.BLACK : PIECE_TEAM.WHITE;

        notifyGameListeners(l -> l.onTurnChange(currentTurn));
        notifyGameListeners(l -> l.onMoveMade(from, to));

        return true;
    }


    private void checkJump (List<Position> validMoves, Position from, int dx, int dy) {

        Position middle = boardModel.createPosition(from.getX() + dx, from.getY() + dy);

        Position to = boardModel.createPosition(from.getX() + 2 * dx, from.getY() + 2 * dy);

        if (middle == null || to == null) return; // Return iff either the jump or move is out of bounds!


        if (isValidPosition(to) && !boardModel.isOccupied(to) && boardModel.isOccupied(middle)) {
            Optional<AbstractPiece> middlePieceOpt = boardModel.getPieceOptional(middle);
            if (middlePieceOpt.isPresent() && middlePieceOpt.get().getTeam() != currentTurn) {
                validMoves.add(to);
            }
        }
    }

    private boolean isValidPosition (Position pos) {
        return pos.getX() >= 0 && pos.getX() < 8 && pos.getY() >= 0 && pos.getY() < 8;
    }

    public PIECE_TEAM getCurrentTurn () {
        return currentTurn;
    }

    public boolean isGameOver () {
        return getPiecesOfTeam(PIECE_TEAM.WHITE).isEmpty() ||
                getPiecesOfTeam(PIECE_TEAM.BLACK).isEmpty();
    }

    private List<AbstractPiece> getPiecesOfTeam (PIECE_TEAM team) {

        List<AbstractPiece> pieceList = new ArrayList<>();

        for (Position p : boardModel.getAllPieces().keySet()) {
            if (boardModel.getPieceAt(p).getTeam() == team) {
                pieceList.add(boardModel.getPieceAt(p));
            }
        }

        return pieceList;
    }

    public BoardModel getBoardModel () {
        return this.boardModel;
    }

    public void setCurrentTurn (PIECE_TEAM peiceTeam) {
        currentTurn = peiceTeam;
        notifyGameListeners(l -> l.onTurnChange(peiceTeam));
    }

    public PIECE_TEAM getWinner () {
        if (isGameOver()) {
            notifyGameListeners(l -> l.onGameComplete(this.currentTurn));
            return this.currentTurn;
        } else return null;
    }

    public boolean canMakeMove (Position from, Position to) {
        if (!boardModel.isOccupied(from) || boardModel.isOccupied(to)) {
            return false;
        }

        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();

        // Simple move: one diagonal step
        if (Math.abs(dx) == 1 && Math.abs(dy) == 1) {
            return true;
        }

        // Jump move: two diagonal steps
        if (Math.abs(dx) == 2 && Math.abs(dy) == 2) {
            int midX = from.getX() + dx / 2;
            int midY = from.getY() + dy / 2;
            Position middle = new Position(midX, midY);

            return boardModel.isOccupied(middle);
        }

        // Invalid move otherwise
        return false;
    }

    public void applyServerConfirmedMove (Position from, Position to) {
        this.boardModel.movePiece(from, to);

        notifyGameListeners(l -> l.onBoardUpdate(boardModel.getAllPieces()));

        System.out.println("Current Turn: " + currentTurn.toString());

    }

    public void applyServerRemovePiece (Position position) {

        System.out.println("Removing piece @ " + position.toString());
        this.boardModel.removePiece(position);

        notifyGameListeners(l -> l.onBoardUpdate(boardModel.getAllPieces()));


    }

    public void updateNewTurn (PIECE_TEAM nextTurn) {
        //Updating the new turn..
        this.currentTurn = nextTurn;
        notifyGameListeners(l -> l.onTurnChange(nextTurn));
    }
}
