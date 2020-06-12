package org.kpax.winfoom.exception;

public class MissingResourceException extends RuntimeException {
    public MissingResourceException(String message) {
        super(message);
    }

    public MissingResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingResourceException(Throwable cause) {
        super(cause);
    }
}
