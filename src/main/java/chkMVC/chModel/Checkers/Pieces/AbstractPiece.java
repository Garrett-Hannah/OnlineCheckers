package chkMVC.chModel.Checkers.Pieces;

import chkGameUtil.IncrementerSingleton;
import chkMVC.chModel.Checkers.BoardModel;
import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkMVC.chModel.Checkers.Position;
import chkMVC.chModel.Math.Vector2i;

import java.util.ArrayList;
import java.util.List;

//Note: abstract pieces were originally planned to be used for when
//moves are recommended by the machine.
//However, that became difficult to implement.
//So it is underutilized.
public abstract class AbstractPiece {
    private int id;
    protected PIECE_TEAM team;


    AbstractPiece (PIECE_TEAM team) {
        this.id = IncrementerSingleton.getInstance().increment();
        this.team = team;
    }

    public int getId () {
        return id;
    }

    public PIECE_TEAM getTeam () {
        return this.team;
    }

    public int getDirection () {
        return this.team.getValue();
    }

    public abstract AbstractPiece promote ();


}


