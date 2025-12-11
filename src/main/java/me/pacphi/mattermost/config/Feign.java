package me.pacphi.mattermost.config;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import me.pacphi.mattermost.model.PostMetadata;
import me.pacphi.mattermost.model.PostMetadataImagesInner;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class Feign {

    @Bean
    public FeignBuilderCustomizer feignBuilderCustomizer(ObjectMapper objectMapper) {
        objectMapper.registerModule(new MattermostModule());

        return builder -> {
            MappingJackson2HttpMessageConverter converter =
                    new MappingJackson2HttpMessageConverter(objectMapper);

            ObjectFactory<HttpMessageConverters> objectFactory =
                    () -> new HttpMessageConverters(converter);

            // Chain the decoders - first our custom response entity decoder, then the spring decoder
            builder.decoder(new ResponseEntityDecoder(new SpringDecoder(objectFactory)));
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

    public static class PostMetadataDeserializer extends StdDeserializer<PostMetadata> {
        public PostMetadataDeserializer() {
            super(PostMetadata.class);
        }

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
