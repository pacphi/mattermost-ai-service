# Migration Guide: Feign to Spring HTTP Interface

This document captures the research, scope, recommendations, and citations for migrating from Spring Cloud OpenFeign to Spring's native HTTP Interface client.

## Background

Spring Cloud OpenFeign entered **maintenance mode** starting with Spring Cloud 2022.0.0. While bug fixes and security patches continue, no new features are being added. Spring Framework 6+ provides a native alternative: **HTTP Interface** clients using `@HttpExchange` annotations.

## Why Migrate?

1. **Future-proofing**: Spring Cloud OpenFeign is feature-frozen
2. **Native Spring support**: HTTP Interface is part of core Spring Framework
3. **Simpler dependencies**: No need for Spring Cloud Feign starter
4. **Consistent patterns**: Aligns with Spring's declarative programming model
5. **Flexibility**: Supports WebClient, RestClient, or RestTemplate as underlying HTTP client

## OpenAPI Generator Configuration

### Before (Feign-based)

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.18.0</version>
    <configuration>
        <generatorName>spring</generatorName>
        <library>spring-cloud</library>
        <configOptions>
            <interfaceOnly>false</interfaceOnly>
            <useJakartaEe>true</useJakartaEe>
            <useTags>true</useTags>
            <!-- other options -->
        </configOptions>
    </configuration>
</plugin>
```

### After (HTTP Interface)

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.18.0</version>
    <configuration>
        <generatorName>spring</generatorName>
        <library>spring-http-interface</library>
        <configOptions>
            <useJakartaEe>true</useJakartaEe>
            <configPackage>me.pacphi.mattermost.configuration</configPackage>
            <!-- other options -->
        </configOptions>
    </configuration>
</plugin>
```

### Key Configuration Changes

| Setting | Feign (spring-cloud) | HTTP Interface |
|---------|---------------------|----------------|
| `generatorName` | `spring` | `spring` |
| `library` | `spring-cloud` | `spring-http-interface` |
| `configPackage` | optional | required (for generated configurator) |
| `interfaceOnly` | `false` | not applicable |
| `useTags` | `true` | not applicable |

## Generated Code Differences

### Feign Client Interface

```java
@FeignClient(name="${channels.name:channels}",
             url="${channels.url:http://localhost}",
             configuration = ClientConfiguration.class)
public interface ChannelsApiClient extends ChannelsApi {
}
```

### HTTP Interface

```java
public interface ChannelsApi {

    @HttpExchange(
        method = "GET",
        value = "/api/v4/channels",
        accept = { "application/json" }
    )
    ResponseEntity<List<Channel>> getChannels();

    // ... other methods
}
```

## Configuration Class

The `spring-http-interface` library generates an abstract configurator class that must be extended:

### Generated Abstract Class

```java
public abstract class HttpInterfacesAbstractConfigurator {
    private final WebClient webClient;

    public HttpInterfacesAbstractConfigurator(final WebClient webClient) {
        this.webClient = webClient;
    }

    @Bean(name = "configPackage.HttpInterfacesAbstractConfigurator.channelsApi")
    ChannelsApi channelsApiHttpProxy() {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builder(WebClientAdapter.forClient(webClient))
            .build();
        return factory.createClient(ChannelsApi.class);
    }

    // ... beans for other API interfaces
}
```

### Your Configuration Class

```java
@Configuration
public class MattermostHttpInterfacesConfiguration
        extends HttpInterfacesAbstractConfigurator {

    public MattermostHttpInterfacesConfiguration(
            @Value("${mattermost.base-url}") String baseUrl,
            AuthenticationService authService) {
        super(WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeaders(headers -> {
                String token = authService.authenticate();
                headers.setBearerAuth(token);
            })
            .build());
    }
}
```

## Dependencies

### Remove

```xml
<!-- Remove Feign dependencies -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### Add

```xml
<!-- Add WebFlux for WebClient -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## Service Class Updates

### Injection Changes

```java
// Before
@Autowired
private ChannelsApiClient channelsApiClient;

// After
@Autowired
private ChannelsApi channelsApi;
```

### Exception Handling Changes

```java
// Before (Feign)
try {
    return channelsApiClient.getChannels();
} catch (FeignException e) {
    throw new MattermostApiException("Failed to get channels", e);
}

// After (WebClient)
try {
    return channelsApi.getChannels();
} catch (WebClientResponseException e) {
    throw new MattermostApiException("Failed to get channels", e);
}
```

## Application Properties

### Remove (Feign-specific)

```yaml
# These are no longer needed
channels:
  url: ${mattermost.base-url}
  name: channels
posts:
  url: ${mattermost.base-url}
  name: posts
```

### Keep/Add

```yaml
mattermost:
  base-url: https://your-mattermost-server.com
```

## Alternative: RestClient Instead of WebClient

The default generated configuration uses `WebClient`. If you prefer `RestClient` (synchronous, non-reactive), you have two options:

### Option 1: Wait for PR #19710

[PR #19710](https://github.com/OpenAPITools/openapi-generator/pull/19710) adds `useHttpServiceProxyFactoryInterfacesConfigurator` option with support for RestClient. It was approved March 6, 2025 but not yet merged.

### Option 2: Custom Templates

1. Extract templates:

   ```bash
   openapi-generator-cli author template -g spring \
       --library spring-http-interface -o ./src/main/resources/templates
   ```

2. Modify `httpInterfacesConfiguration.mustache` to use RestClient:

   ```java
   public HttpInterfacesAbstractConfigurator(final RestClient restClient) {
       this.restClient = restClient;
   }

   @Bean
   ChannelsApi channelsApiHttpProxy() {
       HttpServiceProxyFactory factory = HttpServiceProxyFactory
           .builderFor(RestClientAdapter.create(restClient))
           .build();
       return factory.createClient(ChannelsApi.class);
   }
   ```

3. Configure Maven plugin:

   ```xml
   <templateDirectory>${project.basedir}/src/main/resources/templates</templateDirectory>
   ```

## Spring Boot 4 / Spring Framework 7 Compatibility

**Important**: The default OpenAPI Generator templates (as of 7.18.0) use outdated Spring Framework 6.0 API calls:

```java
// Old API (Spring Framework 6.0.x)
HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient)).build();
```

Spring Framework 7 (used by Spring Boot 4) changed the API to:

```java
// New API (Spring Framework 7.x)
HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
```

To fix this, you **must** use custom templates with the corrected API. Create:

`src/main/resources/templates/libraries/spring-http-interface/httpInterfacesConfiguration.mustache`:

```mustache
/*
* NOTE: This class is auto generated by OpenAPI Generator.
* Do not edit the class manually.
*/
package {{configPackage}};

{{#apiInfo}}
    {{#apis}}
import {{apiPackage}}.{{classname}};
    {{/apis}}
{{/apiInfo}}

import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

public abstract class HttpInterfacesAbstractConfigurator {

    private final WebClient webClient;

    public HttpInterfacesAbstractConfigurator(final WebClient webClient) {
        this.webClient = webClient;
    }

{{#apiInfo}}
{{#apis}}
    @Bean(name = "{{configPackage}}.HttpInterfacesAbstractConfigurator.{{classVarName}}")
    {{classname}} {{classVarName}}HttpProxy() {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
        return factory.createClient({{classname}}.class);
    }

{{/apis}}
{{/apiInfo}}

}
```

Then configure the plugin to use your templates:

```xml
<templateDirectory>${project.basedir}/src/main/resources/templates</templateDirectory>
```

## Jackson 3 Migration (Spring Boot 4)

Spring Boot 4 uses **Jackson 3** as its default JSON library. Jackson 3 introduces a major package rename from `com.fasterxml.jackson` to `tools.jackson`.

### The Problem: Spring AI 2.0.0-M1 Still Requires Jackson 2

When running with Spring Boot 4 and Spring AI 2.0.0-M1, you encounter:

```
No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available
```

**Root cause** (verified in Spring AI source code):
- `ChromaVectorStoreAutoConfiguration.java` imports `com.fasterxml.jackson.databind.ObjectMapper`
- Spring Boot 4 only auto-configures Jackson 3's `tools.jackson.databind.json.JsonMapper`
- Full Jackson 3 support is planned for Spring AI 2.0 GA (H1 2026)

### Solution: Provide Jackson 2 ObjectMapper Bean

Create a configuration class that provides Jackson 2's ObjectMapper for Spring AI:

```java
@Configuration
public class JacksonConfiguration {

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

Add Jackson 2 dependencies explicitly (Spring Boot 4 manages Jackson 3):

```xml
<!-- Jackson 2 dependencies for Spring AI compatibility -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.2</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>2.18.2</version>
</dependency>
```

### Package Namespace Changes (Jackson 2 to 3)

| Jackson 2 | Jackson 3 |
|-----------|-----------|
| `com.fasterxml.jackson.core` | `tools.jackson.core` |
| `com.fasterxml.jackson.databind` | `tools.jackson.databind` |
| `com.fasterxml.jackson.annotation` | `com.fasterxml.jackson.annotation` (unchanged!) |

### Key API Changes

| Jackson 2 | Jackson 3 |
|-----------|-----------|
| `ObjectMapper` | `JsonMapper` (preferred) |
| `JsonProcessingException` (checked) | `JacksonException` (unchecked!) |
| `JsonMappingException` | `DatabindException` |

### OpenRewrite Automated Migration

For your own code that uses Jackson directly:

```bash
mvn rewrite:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-jackson:RELEASE \
  -Drewrite.activeRecipes=org.openrewrite.java.jackson.UpgradeJackson_2_3
```

### Jackson 3 Resources

- [Jackson 3.0 Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [Migration Guide](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md)
- [Spring Blog - Introducing Jackson 3 Support](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring/)
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [OpenRewrite Jackson Migration Recipe](https://docs.openrewrite.org/recipes/java/jackson/upgradejackson_2_3)
- [Dan Vega - Jackson 3 in Spring Boot 4](https://www.danvega.dev/blog/2025/11/10/jackson-3-spring-boot-4)

---

## Research Sources

### Official Documentation

- [Spring Framework - HTTP Interface](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface) - Official Spring documentation for HTTP Interface clients
- [Spring Blog - HTTP Service Client Enhancements](https://spring.io/blog/2025/09/23/http-service-client-enhancements/) - Announcement of HTTP Interface improvements in Spring Framework 7
- [Spring Blog - The State of HTTP Clients in Spring](https://spring.io/blog/2025/09/30/the-state-of-http-clients-in-spring/) - Overview of HTTP client options and deprecation roadmap

### OpenAPI Generator

- [OpenAPI Generator Spring Docs](https://openapi-generator.tech/docs/generators/spring/) - Lists `spring-http-interface` as available library option
- [GitHub - JavaSpring/libraries/spring-http-interface](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator/src/main/resources/JavaSpring/libraries/spring-http-interface) - Mustache templates for HTTP Interface generation
- [GitHub Issue #17927](https://github.com/OpenAPITools/openapi-generator/issues/17927) - Feature request for Spring HTTP Interface support
- [GitHub PR #19710](https://github.com/OpenAPITools/openapi-generator/pull/19710) - RestClient adapter support (approved, pending merge)
- [GitHub Issue #19120](https://github.com/OpenAPITools/openapi-generator/issues/19120) - Feature request for configurable URL placeholders

### Community Resources

- [Maciej Walkowiak - Generating HTTP clients](https://maciejwalkowiak.com/blog/spring-boot-openapi-generate-client/) - Practical guide with examples
- [Baeldung - Spring 6 HTTP Interface](https://www.baeldung.com/spring-6-http-interface) - Tutorial on HTTP Interface basics

## Migration Checklist

- [ ] Update `pom.xml` - change `library` to `spring-http-interface`
- [ ] Add `configPackage` to `configOptions`
- [ ] Add `spring-boot-starter-webflux` dependency
- [ ] Remove Feign-related dependencies (if explicitly declared)
- [ ] Run `mvn clean generate-sources`
- [ ] Create configuration class extending `HttpInterfacesAbstractConfigurator`
- [ ] Update service class injections (`*ApiClient` → `*Api`)
- [ ] Update exception handling (`FeignException` → `WebClientResponseException`)
- [ ] Remove Feign-specific application properties
- [ ] Remove `@EnableFeignClients` annotation (if present)
- [ ] Build and test

## Version Compatibility

| Component | Minimum Version | Recommended |
|-----------|-----------------|-------------|
| Spring Boot | 3.0+ | 4.0.1 |
| Spring Framework | 6.0+ | 7.0+ |
| OpenAPI Generator | 7.0+ | 7.18.0 |
| Java | 17+ | 21 |

---

*Document created: January 2026*
*Last updated: January 2026*
