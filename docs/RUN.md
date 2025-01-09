## Alternative run configurations

### with OpenAI

Build and run a version of the utility that is compatible for use with [OpenAI](https://openai.com).  You will need to [obtain an API key](https://platform.openai.com/settings/profile?tab=api-keys).

Before launching the app:

* Create a `config` folder which would be a sibling of the `build` folder.  Create a file named `creds.yml` inside that folder.  Add your own API key into that file.

```yaml
spring:
  ai:
    openai:
      api-key: {REDACTED}
```
> Replace `{REDACTED}` above with your OpenAI API key

Open a terminal shell and execute

```bash
./mvnw package spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=openai
```

or if you want vector store support delivered via a Docker Compose

```bash
docker compose -f docker/docker-compose.mattermost.yml -f docker/docker-compose.{vector_db_provider}.yml up -d
./mvnw package spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=openai,{vector_db_provider} -Dvector-db-provider={vector_db_provider}
```

> Replace `{vector_db_provider}` with one of [ `chroma`, `pgvector`, `redis`, `weaviate` ]

### with Groq Cloud

Build and run a version of the utility that is compatible for use with [Groq Cloud](https://groq.com).  You will need to [obtain an API key](https://console.groq.com/keys).
Note that Groq does not currently have support for text embedding. So if you intend to run with the `groq-cloud` Spring profile activated, you will also need to provide additional credentials

Before launching the app:

* Create a `config` folder which would be a sibling of the `build` folder.  Create a file named `creds.yml` inside that folder.  Add your own API key into that file.

```yaml
spring:
  ai:
    openai:
      api-key: {REDACTED-1}
      embedding:
        api-key: {REDACTED-2}
```
> Replace `{REDACTED-1}` and `{REDACTED-2}` above with your Groq Cloud API and OpenAI keys respectively.

Open a terminal shell and execute

```bash
./mvnw package spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=groq-cloud
```

or if you want vector store support delivered via a Docker Compose

```bash
docker compose -f docker/docker-compose.mattermost.yml -f docker/docker-compose.{vector_db_provider}.yml up -d
./mvnw package spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=groq-cloud,{vector_db_provider} -Dvector-db-provider={vector_db_provider}
```

> Replace `{vector_db_provider}` with one of [ `chroma`, `pgvector`, `redis`, `weaviate` ]

### with Ollama

Open a terminal shell and execute:

```bash
ollama pull mistral
ollama pull nomic-embed-text
ollama run mistral
```

> You may choose to pull and run other [Ollama](https://ollama.com/search) or [HuggingFace GGUF](https://huggingface.co/models?sort=trending&search=gguf) models.

Open another terminal shell and execute

```bash
./mvnw package bootRun -Dspring.profiles.active=ollama -Dmodel-api-provider=ollama
```

^ If you want to override the chat model you could add `-Dspring.ai.ollama.chat.options.model={model_name}` to the above and replace `{chat_model_name}` with a supported model.  Likewise, you may override the embedding model with `-Dspring.ai.ollama.embedding.options.model={embedding_model_name}`.

Or if you want vector store support delivered via a Docker Compose

```bash
docker compose -f docker/docker-compose.mattermost.yml -f docker/docker-compose.{vector_db_provider}.yml up -d
./mvnw package spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=ollama,{vector_db_provider} -Dvector-db-provider={vector_db_provider}
```

> Replace `{vector_db_provider}` with one of [ `chroma`, `pgvector`, `redis`, `weaviate` ]

### with Vector database

This setup launches either an instance of Chroma, PgVector, Redis Stack, or Weaviate for use by the VectorStore.

A key thing to note is that **you must activate a combination** of Spring profiles, like:

* an LLM provider (i.e., `openai`, `groq-cloud` or `ollama`)
* a Vector database provider (i.e., `chroma`, `pgvector`, `redis`, or `weaviate`)

and arguments, like:

* `-Dmodel-api-provider=ollama`
* `-Dvector-db-provider=chroma` or `-Dvector-db-provider=pgvector` or `-Dvector-db-provider=redis` or `-Dvector-db-provider=weaviate`

#### Chroma

```bash
docker compose -f docker/docker-compose.mattermost.yml -f docker/docker-compose.chroma.yml up -d
./mvnw package spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=openai,chroma -Dvector-db-provider=chroma
```
> You also have the option of building with `-Dmodel-api-provider=ollama` then replacing `openai` or `groq-cloud` in `--spring.profiles.active` with `ollama`.

#### PgVector

```bash
docker compose -f docker/docker-compose.mattermost.yml -f docker/docker-compose.pgvector.yml up -d
./mvnw package spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=groq-cloud,pgvector -Dvector-db-provider=pgvector
```
> You also have the option of building with `-Dmodel-api-provider=ollama` then replacing `openai` or `groq-cloud` in `--spring.profiles.active=` with `ollama`.

#### Redis Stack

```bash
docker compose -f docker/docker-compose.mattermost.yml -f docker/docker-compose.redis.yml up -d
./mvnw package spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=openai,redis -Dvector-db-provider=redis
```
> You also have the option of building with `-Dmodel-api-provider=ollama` then replacing `openai` or `groq-cloud` in `--spring.profiles.active=` with `ollama`.

#### Weaviate

```bash
docker compose -f docker/docker-compose.mattermost.yml -f docker/docker-compose.weaviate.yml up -d
./mvnw package spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=openai,weaviate -Dvector-db-provider=weaviate
```
> You also have the option of building with `-Dmodel-api-provider=ollama` then replacing `openai` or `groq-cloud` in `--spring.profiles.active=` with `ollama`.
