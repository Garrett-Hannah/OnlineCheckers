package chkMVC.chModel.Math;


public class Vector2i {
    public int x, y;

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector2i add(Vector2i other) {
        return new Vector2i(this.x + other.x, this.y + other.y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
