package me.pacphi.mattermost.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Profile({ "chroma", "pgvector", "redis", "weaviate" })
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore store;
    private final ObjectMapper objectMapper;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public IngestionService(VectorStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public void ingest(Object object) {
        ingest(object, "UTF-8");
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
        JsonReader jsonReader;
        try {
            jsonReader = new JsonReader(resource, extractUniqueKeys(resource));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file", e);
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("file_name", resource.getFilename());
        List<Document> documents = jsonReader.get();
        List<Document> enrichedDocuments = new ArrayList<>();
        for (Document document : documents) {
            Map<String, Object> customMetadata = new HashMap<>(metadata);
            customMetadata.putAll(document.getMetadata());
            enrichedDocuments.add(new Document(document.getContent(), customMetadata));
        }
        return enrichedDocuments;
    }

    private String[] extractUniqueKeys(Resource resource) throws IOException {
        JsonNode rootNode = objectMapper.readTree(resource.getInputStream());
        Set<String> uniqueKeys = new HashSet<>();
        extractKeys(rootNode, "", uniqueKeys);
        return uniqueKeys.toArray(new String[0]);
    }

    private void extractKeys(JsonNode jsonNode, String currentPath, Set<String> keys) {
        if (jsonNode.isObject()) {
            jsonNode.fields().forEachRemaining(entry -> {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();
                keys.add(newPath);
                extractKeys(entry.getValue(), newPath, keys);
            });
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                extractKeys(jsonNode.get(i), currentPath + "[" + i + "]", keys);
            }
        }
    }

}
