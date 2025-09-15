package chkNetwork;

import java.util.HashMap;
import java.util.Map;

public enum CLIENT_REQUEST_CODES {

    // Connection requests
    JOIN(1000),
    DISCONNECT(1001),
    PING(1002),

    //These are lobby specific requests. available to all users.
    USER_LIST(1101),
    SEND_CHAT(1102),

    //host specific commands. These each are only for the host so expect that.
    HOST_BEGIN_GAME(1200),
    HOST_ASSIGN_TEAM(1201),

    //Play Commands. (right now its only needed that the piece can be moved since we are not doing move validations.
    MOVE_PIECE(1300),      // Move a piece (normal or jump)
    RESIGN_GAME(1301),     // Player resigns from the game

    //Other Error stuff:
    UNKNOWN_ERROR(1901);

    private final int code;

    CLIENT_REQUEST_CODES (int code) {
        this.code = code;
    }

    public int getCode () {
        return code;
    }

    // Mapping from string to enum
    private static final Map<String, CLIENT_REQUEST_CODES> stringToEnumMap = new HashMap<>();

    // Populate map statically
    static {
        for (CLIENT_REQUEST_CODES code : values()) {
            stringToEnumMap.put(code.name(), code);
        }
    }

    // Method to get enum from string
    public static CLIENT_REQUEST_CODES fromString (String code) {
        return stringToEnumMap.getOrDefault(code, UNKNOWN_ERROR);
    }
}
