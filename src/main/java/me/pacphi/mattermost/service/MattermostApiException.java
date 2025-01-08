package me.pacphi.mattermost.service;

/**
 * Custom exception for Mattermost API errors
 */
public class MattermostApiException extends RuntimeException {
    public MattermostApiException(String message) {
        super(message);
    }

    public MattermostApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
