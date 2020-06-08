package org.kpax.winfoom.exception;

public class ResetNotSupportedException extends RuntimeException {

    public ResetNotSupportedException() {
    }

    public ResetNotSupportedException(String message) {
        super(message);
    }

    public ResetNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResetNotSupportedException(Throwable cause) {
        super(cause);
    }
}
