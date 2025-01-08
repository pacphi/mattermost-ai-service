package me.pacphi.mattermost.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Mattermost API connection.
 */
@ConfigurationProperties(prefix = "mattermost")
public record MattermostProperties(
        String baseUrl,
        Credentials credentials
) {}
