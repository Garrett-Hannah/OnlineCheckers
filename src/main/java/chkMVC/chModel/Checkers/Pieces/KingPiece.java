package chkMVC.chModel.Checkers.Pieces;

import chkMVC.chModel.Checkers.PIECE_TEAM;

public class KingPiece extends AbstractPiece {
    public KingPiece (PIECE_TEAM team) {
        super(team);
    }

    @Override
    public AbstractPiece promote () {
        return this;
    }
}
