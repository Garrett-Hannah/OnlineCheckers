package chkNetwork;

import java.util.HashMap;
import java.util.Map;

public enum SERVER_RESPONSE_CODES {

    // General success & error responses
    SUCCESS(2000),
    ERROR(2001),
    INVALID_REQUEST(2002),


    // Game state updates
    GAME_STATE_UPDATE(2100),  // Broadcast updated board state
    GAME_START(2101),         // Game officially starts
    GAME_END(2102),           // Game ends
    ROUND_UPDATE(2103),       // Round state update
    PLAYER_JOINED(2104),      // A new player has joined
    PLAYER_LEFT(2105),        // A player left the game
    CLIENT_LIST(2106),        //Listing all active clients.

    /**
     * Usage: [CODE] (sender, Position1, Position2)
     */
    MOVE_PIECE(2505),
    REMOVE_PIECE(2506),

    /**
     * Usage: [Code] (sender, message)
     **/
    CHAT_MESSAGE(2400),
    NOTIFICATION(2401),

    /**
     * Usage: [CODE] (assigned_role)
     **/
    ROLE_ASSIGN(2107);       //
    private final int code;

    SERVER_RESPONSE_CODES (int code) {
        this.code = code;
    }

    public int getCode () {
        return code;
    }

    // Mapping from string to enum
    private static final Map<String, SERVER_RESPONSE_CODES> stringToEnumMap = new HashMap<>();

    // Populate map statically
    static {
        for (SERVER_RESPONSE_CODES code : values()) {
            stringToEnumMap.put(code.name(), code);
        }
    }

    // Method to get enum from string
    public static SERVER_RESPONSE_CODES fromString (String code) {
        return stringToEnumMap.getOrDefault(code, ERROR);
    }
}
