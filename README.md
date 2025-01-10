# Mattermost AI Service

[![GA](https://img.shields.io/badge/Release-Alpha-darkred)](https://img.shields.io/badge/Release-Alpha-darkred) ![Github Action CI Workflow Status](https://github.com/pacphi/mattermost-ai-service/actions/workflows/ci.yml/badge.svg) [![Known Vulnerabilities](https://snyk.io/test/github/pacphi/mattermost-ai-service/badge.svg?style=plastic)](https://snyk.io/test/github/pacphi/mattermost-ai-service)

* [Background](#background)
* [Getting started](#getting-started)
* [Prerequisites](#prerequisites)
* How to
  * [Clone](#how-to-clone)
  * [Build](#how-to-build)
  * [Run](#how-to-run)
  * [Deploy to Tanzu Platform for Cloud Foundry](#how-to-deploy-to-tanzu-platform-for-cloud-foundry)

## Background

Demonstrate two types of chat interactions with a Mattermost instance leveraging Mattermost OpenAPI v3 spec and Spring AI.

Use-cases:

* [x] Ingest a Mattermost channel into a VectorStore, then chat with that point-in-time corpus of knowledge
* [ ] Chat by configuring tool-calling to ask for insights in real-time

## Getting started

I got started with:

* A Github [account](https://github.com/signup)
* A Stackhero [account](https://stackhero.io)
* This Spring Initializr [configuration](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.4.1&packaging=jar&jvmVersion=21&groupId=me.pacphi&artifactId=mattermost-ai-service&name=Mattermost%20AI%20Service&description=Demonstrate%20two%20types%20of%20chat%20interactions%20with%20a%20Mattermost%20instance%20leveraging%20Mattermost%20OpenAPI%20v3%20spec%20and%20Spring%20AI&packageName=me.pacphi.mattermost&dependencies=spring-ai-openai,web,configuration-processor,devtools,docker-compose)
* Mattermost 
  * credentials (i.g., username, password)
* An LLM provider
  * one of Groq Cloud, OpenAI, or Ollama

## Prerequisites

* Docker Compose
* Git CLI
* An Open AI or Groq Cloud account
* Java SDK 21
* Maven 3.9.9
* Mattermost OpenAPI v3 [spec](https://api.mattermost.com/v3/static/mattermost-openapi-v3.yaml)
* yq CLI

## How to clone

with Git CLI

```bash
git clone https://github.com/pacphi/mattermost-ai-service
```

with Github CLI

```bash
gh repo clone pacphi/mattermost-ai-service
```

## Keeping up-to-date with OpenAPI spec changes

Visit https://api.mattermost.com/ in your favourite browser periodically to grab updates that may have been introduced to the Mattermost OpenAPI specification.

Click on the `Download` button next to the `Download OpenAPI specification` label at the top of the page.

Copy and overwrite the file named `openapi.json` into the resources directory [here](src/main/resrouces/openapi).

Execute the following command in a terminal shell in that directory:

```bash
cat openapi.json | yq eval -P '.' > mattermost-openapi-v3.yml
```

> It's strongly encouraged that you compare the version in Git commit history with the version you just fetched.  There are likely to be breaking changes that result in compilation failures for generated source.

## How to build

Open a terminal shell, then execute:

```bash
cd mattermost-ai-service
mvn clean package
```

For more exotic build and packaging alternatives, refer to the guide [here](docs/BUILD.md).

## How to run

Set these environment variables

### with username and password

```bash
export MATTERMOST_BASE_URL=
export MATTERMOST_USERNAME=
export MATTERMOST_PASSWORD=
```

> Add appropriate values for each of the required `MATTERMOST_` prefixed environment variables above.

### with a personal access token

```bash
export MATTERMOST_BASE_URL=
export MATTERMOST_PERSONAL_ACCESS_TOKEN=
```

> Likewise, add appropriate values for each of the required `MATTERMOST_` prefixed environment variables above.

Refer to [this guide](docs/RUN.md) to understand the various run configuration alternatives available.

If you chose to launch containers with `docker compose` you will first need to create a Mattermost account.  Visit http://localhost:8065 to do that.

> Make sure to use the same credentials as you had exported above.  Or if you forgot to set the environment variables, just use the defaults as declared in [application.yml](src/main/resources/application.yml).

Open your favorite browser and visit `http://localhost:8080`.

> Back in the terminal shell, press Ctrl+C to shutdown.

In case you launched containers with `docker compose`, you can clean everything up by running:

```bash
docker stop $(docker ps -a -q) && docker rm $(docker ps -a -q) && docker volume rm $(docker volume ls -q)
```

## How to deploy to Tanzu Platform for Cloud Foundry

Refer to the instructions [here](docs/TP4CF.md).
