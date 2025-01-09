## Alternative builds and packaging

The options below represent the collection of Maven [profiles](https://maven.apache.org/guides/introduction/introduction-to-profiles.html#Property) available in [pom.xml](../pom.xml).  These dependencies will be packaged in the resulting executable JAR.

> Note that an optional dependency on [spring-boot-docker-compose](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.docker-compose) is added to facilitate lifecycle management of Model API providers.

### Vector database providers

#### [Chroma](https://docs.trychroma.com/guides)

Adds dependency on:

* [spring-ai-chroma-store-spring-boot-starter](https://docs.spring.io/spring-ai/reference/api/vectordbs/chroma.html)


```bash
./mvnw package -Dvector-db-provider=chroma
```

#### [PgVector](https://github.com/pgvector/pgvector)

Adds dependency on:

* [spring-ai-pgvector-store-spring-boot-starter](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)

```bash
./mvnw package -Dvector-db-provider=pgvector
```

#### [Redis Stack](https://redis.io/about/about-stack/)

Adds dependency on:

* [spring-ai-redis-store-spring-boot-starter](https://docs.spring.io/spring-ai/reference/api/vectordbs/redis.html)

```bash
./mvnw package -Dvector-db-provider=redis
```

#### [Weaviate](https://weaviate.io/developers/weaviate)

Adds dependency on:

* [spring-ai-weaviate-store-spring-boot-starter](https://docs.spring.io/spring-ai/reference/api/vectordbs/weaviate.html)

```bash
./mvnw package -Dvector-db-provider=weaviate
```

### Model API providers

You have the ability to swap model API providers.  The default provider is OpenAI.  But you can override the default by setting the `-Dmodel-api-provider` argument.

You have the option of setting this property's value to:

* `ollama` - provides access to Ollama models
    * Adds a dependency on [spring-ai-ollama-spring-boot-starter](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html).  Work with [your choice](https://ollama.com/search) of Ollama LLMs.

E.g., with

```commandline
-Dmodel-api-provider=ollama
```