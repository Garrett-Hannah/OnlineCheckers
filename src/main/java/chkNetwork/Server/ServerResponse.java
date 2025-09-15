package chkNetwork.Server;

import chkNetwork.SERVER_RESPONSE_CODES;

import java.util.*;
import java.util.function.Function; // For payload extraction logic
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a structured response received from the server.
 * Provides static methods to parse a raw string message into a ServerResponse object
 * and to format a ServerResponse back into a string.
 * <p>
 * Expected Format: [CODE] (PayloadArguments...)
 * where PayloadArguments are typically comma-separated.
 */
public class ServerResponse {

    private final SERVER_RESPONSE_CODES type;
    private final List<String> payload; // Unmodifiable, never null
    private final String rawMessage;    // Original message for debugging/logging

    public String getPayloadAsStringSafe () {
        return payload.get(0);
    }

    // Helper structure to link Response Code, Pattern, and Payload Extraction Logic
    private static class ResponseFormat {
        final SERVER_RESPONSE_CODES code;
        final Pattern pattern;
        // Function takes a Matcher and returns the payload List<String>
        final Function<Matcher, List<String>> payloadExtractor;

        ResponseFormat (SERVER_RESPONSE_CODES code, Pattern pattern, Function<Matcher, List<String>> extractor) {
            this.code = code;
            this.pattern = pattern;
            this.payloadExtractor = extractor;
        }
    }

    // List to hold all known response formats. Use LinkedHashMap if order is critical for matching.
    private static final List<ResponseFormat> RESPONSE_FORMATS = new ArrayList<>();

    /**
     * Private constructor. Use factory methods like 'parse' or 'create'.
     * Ensures type is non-null and payload is an unmodifiable, non-null list.
     */
    ServerResponse (SERVER_RESPONSE_CODES type, List<String> payload, String rawMessage) {
        this.type = Objects.requireNonNull(type, "Response type cannot be null");
        // Ensure payload is stored as non-null, unmodifiable list
        this.payload = (payload != null)
                ? Collections.unmodifiableList(new ArrayList<>(payload))
                : Collections.emptyList();
        this.rawMessage = rawMessage; // Can be null if created directly
    }

    public SERVER_RESPONSE_CODES getType () {
        return type;
    }

    public List<String> getPayload () {
        return payload;
    } // Guaranteed non-null

    public String getRawMessage () {
        return rawMessage;
    } // Might be null if not from parse()

    @Override
    public String toString () {
        return this.type.toString() + ": " + rawMessage;
    }

    public static ServerResponse create (SERVER_RESPONSE_CODES type, List<String> payload) {
        // Raw message is null here as it wasn't parsed
        return new ServerResponse(type, payload, null);
    }
}