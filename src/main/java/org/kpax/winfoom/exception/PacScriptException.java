package org.kpax.winfoom.exception;

/**
 * Signal a failed call to {@code FindProxyForURL} method of the PAC script.
 */
public class PacScriptException extends Exception {

    public PacScriptException() {
    }

    public PacScriptException(String message) {
        super(message);
    }

    public PacScriptException(String message, Throwable cause) {
        super(message, cause);
    }

    public PacScriptException(Throwable cause) {
        super(cause);
    }

}
