package me.pacphi.mattermost.service;

/**
 * Custom exception for Mattermost authentication failures
 */
public class MattermostAuthenticationException extends RuntimeException {
    public MattermostAuthenticationException(String message) {
        super(message);
    }

    public MattermostAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
