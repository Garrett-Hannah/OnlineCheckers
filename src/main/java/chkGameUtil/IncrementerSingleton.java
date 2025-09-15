package chkGameUtil;

//This probably didnt need to be included but it could have been useful for something?
public class IncrementerSingleton {
    private static IncrementerSingleton instance;
    private int i = 0; // Counter


    private IncrementerSingleton () {
    }

    public static IncrementerSingleton getInstance () {
        if (instance == null) {
            instance = new IncrementerSingleton();
        }
        return instance;
    }

    public int increment () {
        return ++i;
    }

    // Method to get the current value
    public int getValue () {
        return i;
    }
}
