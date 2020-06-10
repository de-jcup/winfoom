package org.kpax.winfoom.exception;

import org.apache.http.HttpException;

/**
 * Signal that the proxy settings (as provided by {@link org.kpax.winfoom.config.ProxyConfig}) are invalid.
 */
public class InvalidProxySettingsException extends HttpException {

    public InvalidProxySettingsException(String message) {
        super(message);
    }

    public InvalidProxySettingsException(String message, Throwable cause) {
        super(message, cause);
    }
}
