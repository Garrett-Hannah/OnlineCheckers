package chkMVC.chModel.Checkers.Pieces;

import chkMVC.chModel.Checkers.PIECE_TEAM;

public class SerfPiece extends AbstractPiece {

    public SerfPiece (PIECE_TEAM team) {
        super(team);
    }

    @Override
    public AbstractPiece promote () {
        return new KingPiece(team);
    }
}