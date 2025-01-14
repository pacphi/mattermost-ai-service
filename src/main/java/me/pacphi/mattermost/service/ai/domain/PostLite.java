package me.pacphi.mattermost.service.ai.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record PostLite(String channel, String message, String username, LocalDateTime created, LocalDateTime updated) {

    public String asResponse() {
        String createdAt = created().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return String.format("%s [ on %s created %s by %s ]", message(), channel(),  createdAt, username());
    }
}
