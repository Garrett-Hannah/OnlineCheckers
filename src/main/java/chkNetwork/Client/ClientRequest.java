package chkNetwork.Client;

import chkNetwork.CLIENT_REQUEST_CODES;

import java.util.*;


//Note: a lot of this was created by gemini2.5, as there was a lot of small issues with parsing commands in the correct way. (I then learnt that json would have made things easier)
//I hope that this is not an issue, as that wasnt what we explicitly learnt in class, however the server implementation was coded by human hands.

/**
 * Represents a structured request sent from the client to the server.
 * Provides static methods to parse a raw string message (typically on the server)
 * and to format a ClientRequest object into a string (typically on the client).
 * <p>
 * Expected Format: [CODE] (PayloadArgument1, PayloadArgument2, ...)
 */
public class ClientRequest {

    private final CLIENT_REQUEST_CODES type;
    private final List<String> payload; // Unmodifiable, never null

    /**
     * Constructs a ClientRequest. Sender might be relevant for client-side logic.
     * RawMessage is typically non-null only when created via parsing.
     */
    public ClientRequest (CLIENT_REQUEST_CODES type, List<String> payload) {
        this.type = Objects.requireNonNull(type, "Request Cannot be null");
        this.payload = (payload != null)
                ? Collections.unmodifiableList(new ArrayList<>(payload))
                : Collections.emptyList();
    }

    // --- Getters ---

    public CLIENT_REQUEST_CODES getType () {
        return type;
    }

    public List<String> getPayload () {
        return payload;
    }


}