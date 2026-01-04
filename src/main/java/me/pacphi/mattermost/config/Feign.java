package me.pacphi.mattermost.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.pacphi.mattermost.model.PostMetadata;
import me.pacphi.mattermost.model.PostMetadataImagesInner;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class Feign {

    @Bean
    public FeignBuilderCustomizer feignBuilderCustomizer(ObjectMapper objectMapper) {
        objectMapper.registerModule(new MattermostModule());
        return builder -> {
            // The decoder configuration is handled by Spring Cloud OpenFeign autoconfiguration
            // which automatically picks up the customized ObjectMapper
        };
    }

    public static class MattermostModule extends SimpleModule {
        public MattermostModule() {
            super("MattermostModule");
            setNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            addDeserializer(
                    PostMetadata.class,
                    new PostMetadataDeserializer()
            );
        }
    }

    public static class PostMetadataDeserializer extends JsonDeserializer<PostMetadata> {
        @Override
        public PostMetadata deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            PostMetadata metadata = new PostMetadata();

            JsonNode imagesNode = node.get("images");
            if (imagesNode != null) {
                List<PostMetadataImagesInner> images = new ArrayList<>();
                if (imagesNode.isObject()) {
                    // Single object case
                    PostMetadataImagesInner image = p.getCodec().treeToValue(imagesNode, PostMetadataImagesInner.class);
                    images.add(image);
                } else if (imagesNode.isArray()) {
                    // Array case
                    for (JsonNode imageNode : imagesNode) {
                        PostMetadataImagesInner image = p.getCodec().treeToValue(imageNode, PostMetadataImagesInner.class);
                        images.add(image);
                    }
                }
                metadata.setImages(images);
            }

            return metadata;
        }
    }
}
