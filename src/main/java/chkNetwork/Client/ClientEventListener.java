package chkNetwork.Client;

import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkMVC.chModel.Checkers.Pieces.AbstractPiece;
import chkMVC.chModel.Checkers.Position;

import java.util.HashMap;
import java.util.List;

public interface ClientEventListener {

    default void onGameStart () {
    }

    default void onTeamAssign (PIECE_TEAM team) {
    }

    default void showErrorMessage (String loginError, String s) {
    }

    default void setWindowTitle (String title) {
    }

    default void appendMessage (String message) {
    }

    default void updateUserList (List<String> users) {

    }

    default void setHostView (boolean state) {
    }


    void onDisconnect ();

    void onServerMoveConfirmed (Position from, Position to);

    void onRoundUpdate (PIECE_TEAM nextTurn);

    void onServerRemoveConfirmed (Position removePosition);

    void onGameEnd (PIECE_TEAM team);
}
