package chkMVC.chModel.Checkers;


//Define the peice team as -1, and 1 so we can find opposites.
public enum PIECE_TEAM {
    WHITE(1),
    BLACK(-1),
    SPECTATOR(0);

    private final int value;

    PIECE_TEAM (int value) {
        this.value = value;
    }

    public int getValue () {
        return this.value;
    }
}
