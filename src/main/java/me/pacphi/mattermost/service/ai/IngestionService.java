package me.pacphi.mattermost.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.pacphi.mattermost.api.ChannelsApiClient;
import me.pacphi.mattermost.api.UsersApiClient;
import me.pacphi.mattermost.model.Post;
import me.pacphi.mattermost.service.ai.domain.PostLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final ChannelsApiClient channelsApiClient;
    private final UsersApiClient usersApiClient;
    private final VectorStore store;
    private final ObjectMapper objectMapper;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public IngestionService(
            ChannelsApiClient channelsApiClient,
            UsersApiClient usersApiClient,
            VectorStore store,
            ObjectMapper objectMapper) {
        this.channelsApiClient = channelsApiClient;
        this.usersApiClient = usersApiClient;
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public void ingest(Post post) {
        String channel = channelsApiClient.getChannel(post.getChannelId()).getBody().getName();
        String username = usersApiClient.getUser(post.getUserId()).getBody().getUsername();
        PostLite postLite = new PostLite(
            channel,
            post.getMessage(),
            username,
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(post.getCreateAt()),
                ZoneId.systemDefault()),
            LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(post.getEditAt()),
                    ZoneId.systemDefault())
        );
        ingest(postLite, "UTF-8");
    }

    public void ingest(Object object, String charset) {
        try {
            String jsonString = objectMapper.writeValueAsString(object);
            ingest(new ByteArrayResource(jsonString.getBytes(charset)));
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
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
                        log.error("Failed to read JSON file", e);
                        return null;
                    }
                })
                .orElseThrow(() -> new RuntimeException("Failed to create JsonReader"));

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

        return Optional.ofNullable(document.getMetadata())
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
                        log.error("Failed to create document with metadata", e);
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
            if (jsonNode.size() > 0) {
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
            return node.size() > 0;
        }
        if (node.isObject()) {
            return node.size() > 0;
        }
        // For numbers, booleans, etc.
        return true;
    }
}