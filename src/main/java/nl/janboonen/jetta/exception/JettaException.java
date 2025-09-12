package nl.janboonen.jetta.exception;

public class JettaException extends RuntimeException {

    public JettaException(String message) {
        super(message);
    }

    public JettaException(String message, Throwable cause) {
        super(message, cause);
    }

}
