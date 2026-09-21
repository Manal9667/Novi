package com.novi.exception;

/**
 * Thrown when an optional external capability the caller explicitly asked for
 * is not configured/available - e.g. the computer-vision book scanner when no
 * vision model API key is present. Maps to HTTP 503 so the client can show a
 * clear "this feature isn't turned on" message rather than a generic error.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
