package me.pacphi.mattermost.service.ai;

import me.pacphi.mattermost.model.*;
import org.springframework.web.service.invoker.HttpRequestValues;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

record AttributedPost(
        String team, String channel, String message,
        String type, String hashtag, String priority,
        Integer numberOfAcknowledgements, Integer numberOfEmbeddings, Integer numberOfReactions,
        String firstName, String lastName, String username,
        LocalDateTime created, LocalDateTime updated) {

    AttributedPost(Team team, Channel channel, Post post, User user) {
        this(
                team.getName(),
                channel.getName(),
                post.getMessage(),
                post.getType(),
                post.getHashtag(),
                getPriority(post.getMetadata()),
                getMetadata(post.getMetadata(), "acknowledgements"),
                getMetadata(post.getMetadata(), "embeddings"),
                getMetadata(post.getMetadata(), "reactions"),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                post.getCreateAt() != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(post.getCreateAt()), ZoneId.systemDefault()): null,
                post.getUpdateAt() != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(post.getUpdateAt()), ZoneId.systemDefault()): null
        );
    }

    private static String getPriority(PostMetadata metadata) {
        if (metadata != null) {
            PostMetadataPriority pmp = metadata.getPriority();
            if (pmp != null) {
                return pmp.getPriority();
            }
        }
        return null;
    }

    private static Integer getMetadata(PostMetadata metadata, String key) {
        Integer result = 0;
        if (metadata != null) {
            result = switch (key) {
                case "acknowledgements" -> getMetadata(metadata.getAcknowledgements());
                case "embeddings" -> getMetadata(metadata.getEmbeds());
                case "reactions" -> getMetadata(metadata.getReactions());
                default -> result;
            };
        }
        return result;
    }
    private static <T> Integer getMetadata(List<T> metadata) {
        if (metadata != null) {
            if (metadata.isEmpty()) {
                return 0;
            }
            return metadata.size();
        }
        return 0;
    }
}