package me.pacphi.mattermost.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.pacphi.mattermost.api.ChannelsApi;
import me.pacphi.mattermost.api.TeamsApi;
import me.pacphi.mattermost.api.UsersApi;
import me.pacphi.mattermost.model.Channel;
import me.pacphi.mattermost.model.Post;
import me.pacphi.mattermost.model.Team;
import me.pacphi.mattermost.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final ChannelsApi channelsApi;
    private final TeamsApi teamsApi;
    private final UsersApi usersApi;
    private final VectorStore store;
    private final ObjectMapper objectMapper;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public IngestionService(
            ChannelsApi channelsApi,
            TeamsApi teamsApi,
            UsersApi usersApi,
            VectorStore store,
            ObjectMapper objectMapper) {
        this.channelsApi = channelsApi;
        this.teamsApi = teamsApi;
        this.usersApi = usersApi;
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public void ingest(Post post) throws JsonProcessingException, UnsupportedEncodingException {
        Assert.notNull(post, "Post cannot be null");
        Channel channel = channelsApi.getChannel(post.getChannelId()).getBody();
        Assert.notNull(channel, "Channel cannot be null");
        Team team = teamsApi.getTeam(channel.getTeamId()).getBody();
        Assert.notNull(team, "Team cannot be null");
        User user = usersApi.getUser(post.getUserId()).getBody();
        Assert.notNull(user, "User cannot be null");
        AttributedPost attributedPost = new AttributedPost(
            team, channel, post, user
        );
        ingest(attributedPost, "UTF-8");
    }

    private void ingest(Object object, String charset) throws JsonProcessingException, UnsupportedEncodingException {
        String jsonString = objectMapper.writeValueAsString(object);
        ingest(new ByteArrayResource(jsonString.getBytes(charset)));
    }

    private void ingest(Resource resource) {
        List<Document> documents = loadJson(resource);
        store.accept(splitter.apply(documents));
    }

    private List<Document> loadJson(Resource resource) {
        if (resource == null) {
            return Collections.emptyList();
        }

        JsonReader jsonReader = Optional.of(resource)
                .map(res -> {
                    try {
                        return new JsonReader(res, extractUniqueKeys(res));
                    } catch (IOException e) {
                        log.error("---- Failed to read JSON file", e);
                        return null;
                    }
                })
                .orElseThrow(() -> new RuntimeException("---- Failed to create JsonReader"));

        Map<String, Object> baseMetadata = Optional.ofNullable(resource.getFilename())
                .map(filename -> {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("file_name", filename);
                    return meta;
                })
                .orElse(new HashMap<>());

        return Optional.ofNullable(jsonReader.get())
                .map(docs -> docs.stream()
                        .filter(Objects::nonNull)
                        .map(document -> enrichDocument(document, baseMetadata))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private Document enrichDocument(Document document, Map<String, Object> baseMetadata) {
        if (document == null) {
            return null;
        }

        return Optional.of(document.getMetadata())
                .map(documentMetadata -> documentMetadata.entrySet().stream()
                        .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (v1, v2) -> v1,
                                () -> new HashMap<>(baseMetadata)
                        )))
                .map(enrichedMetadata -> {
                    try {
                        return new Document(
                                Optional.ofNullable(document.getText()).orElse(""),
                                enrichedMetadata
                        );
                    } catch (IllegalArgumentException e) {
                        log.error("---- Failed to create document with metadata", e);
                        return null;
                    }
                })
                .orElse(null);
    }

    private String[] extractUniqueKeys(Resource resource) throws IOException {
        JsonNode rootNode = objectMapper.readTree(resource.getInputStream());
        Set<String> uniqueKeys = new HashSet<>();
        extractKeys(rootNode, "", uniqueKeys);
        return uniqueKeys.toArray(new String[0]);
    }

    private void extractKeys(JsonNode jsonNode, String currentPath, Set<String> keys) {
        if (jsonNode == null || jsonNode.isNull()) {
            return;
        }

        if (jsonNode.isObject()) {
            jsonNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (!value.isNull()) {
                    String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();
                    // Only add the key if it has a non-null value
                    if (isValidValue(value)) {
                        keys.add(newPath);
                    }
                    extractKeys(value, newPath, keys);
                }
            });
        } else if (jsonNode.isArray()) {
            // Only process array if it's not empty
            if (!jsonNode.isEmpty()) {
                for (int i = 0; i < jsonNode.size(); i++) {
                    JsonNode element = jsonNode.get(i);
                    if (!element.isNull()) {
                        extractKeys(element, currentPath + "[" + i + "]", keys);
                    }
                }
            }
        }
    }

    private boolean isValidValue(JsonNode node) {
        if (node.isNull()) {
            return false;
        }
        if (node.isTextual()) {
            return !node.asText().isEmpty();
        }
        if (node.isArray()) {
            return !node.isEmpty();
        }
        if (node.isObject()) {
            return !node.isEmpty();
        }
        // For numbers, booleans, etc.
        return true;
    }
}