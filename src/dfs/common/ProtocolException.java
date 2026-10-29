package dfs.common;

// thrown when a request is invalid, e.g. unknown command or bad arguments
public class ProtocolException extends RuntimeException {
    public ProtocolException(String message) {
        super(message);
    }
}