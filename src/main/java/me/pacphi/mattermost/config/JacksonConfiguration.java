package me.pacphi.mattermost.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 2 configuration for Spring AI compatibility.
 *
 * <p>Spring AI 2.0.0-M1 requires Jackson 2's {@link ObjectMapper} but Spring Boot 4
 * only auto-configures Jackson 3's {@code tools.jackson.databind.json.JsonMapper}.
 * This configuration provides a Jackson 2 ObjectMapper bean to satisfy Spring AI's
 * requirements.
 *
 * <p>This is a temporary workaround until Spring AI 2.0 GA adds full Jackson 3 support
 * (expected H1 2026).
 *
 * @see <a href="https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring/">Spring Jackson 3 Support</a>
 * @see <a href="https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md">Jackson 3 Migration Guide</a>
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
