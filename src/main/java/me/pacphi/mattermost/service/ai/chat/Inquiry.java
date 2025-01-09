package me.pacphi.mattermost.service.ai.chat;

import java.util.List;

public record Inquiry(String question, List<FilterMetadata> filter) {
}
