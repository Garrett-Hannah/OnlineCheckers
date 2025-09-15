package chkMVC.chModel.Checkers;

import chkMVC.chModel.Checkers.Pieces.AbstractPiece;

import java.util.Map;

//Game Event Listener, Should update on important thigns like when the game is done etc.
public interface GameEventListener {


    void onGameComplete (PIECE_TEAM currentTurn);

    void onMoveMade (Position from, Position to);


    void onBoardUpdate (Map<Position, AbstractPiece> boardState);

    void onTurnChange (PIECE_TEAM currentTurn);

    void onPieceRemoved (Position position);
}
