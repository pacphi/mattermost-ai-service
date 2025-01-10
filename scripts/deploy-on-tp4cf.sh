#!/usr/bin/env bash

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Get the parent (root) directory
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Change to the root directory
cd "$ROOT_DIR" || exit 1

APP_NAME="mattermost-ai-service"
APP_VERSION="0.0.1-SNAPSHOT"

COMMAND=$1

GENAI_CHAT_SERVICE_NAME="mais-llm"
GENAI_CHAT_PLAN_NAME="llama3.1" # plan must have chat capability

GENAI_EMBEDDINGS_SERVICE_NAME="mais-embedding"
GENAI_EMBEDDINGS_PLAN_NAME="nomic-emded-text" # plan must have Embeddings capability

PGVECTOR_SERVICE_NAME="mais-db"
PGVECTOR_PLAN_NAME="on-demand-postgres-db"

MATTERMOST_PROVIDER_SERVICE_NAME="mais-provider"
MATTERMOST_PROVIDER_PLAN_NAME="default"

# Verify the JAR exists before proceeding
JAR_PATH="build/libs/$APP_NAME-$APP_VERSION.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo "Error: JAR file not found at $JAR_PATH"
    echo "Current directory: $(pwd)"
    exit 1
fi

# Easiest thing to do for demo purposes is to spin up an instance of Mattermost on StackHero (https://www.stackhero.io/en/)
if  [[ -f ${HOME}/.mattermost/config ]]; then
    echo "Mattermost configuration file found."

# Source the $HOME/.mattermost/config file
# This file should contain at a minimum the following key-value environment variable pairs:
# export MATTERMOST_BASE_URL=<mattermost-base-url>
# export MATTERMOST_USERNAME='<your_admin_account_username>'
# export MATTERMOST_PASSWORD='<your_admin_account_password>'
# export MATTERMOST_PERSONAL_ACCESS_TOKEN='<your_admin_readonly_personal_access_token>'

    source $HOME/.mattermost/config
fi

case $COMMAND in

setup)

  echo && printf "\e[37mℹ️  Creating services ...\e[m\n" && echo

  cf create-service postgres $PGVECTOR_PLAN_NAME $PGVECTOR_SERVICE_NAME -w
	printf "Waiting for service $PGVECTOR_SERVICE_NAME to create."
	while [ `cf services | grep 'in progress' | wc -l | sed 's/ //g'` != 0 ]; do
  	printf "."
  	sleep 5
	done
	echo "$PGVECTOR_SERVICE_NAME creation completed."

    if [[ -n "$MATTERMOST_BASE_URL" ]]; then
      # Use jq to safely create the JSON configuration
      MATTERMOST_CREDHUB_CONFIG=$(jq -n \
          --arg baseurl "$MATTERMOST_BASE_URL" \
          --arg username "$MATTERMOST_USERNAME" \
          --arg password "$MATTERMOST_PASSWORD" \
          '{
              "MATTERMOST_BASE_URL": baseurl,
              "MATTERMOST_USERNAME": $username,
              "MATTERMOST_PASSWORD": $password
          }')

      echo && printf "\e[37mℹ️  Creating $MATTERMOST_PROVIDER_SERVICE_NAME Mattermost service configuration...\e[m\n" && echo

      cf create-service credhub "$MATTERMOST_PROVIDER_PLAN_NAME" "$MATTERMOST_PROVIDER_SERVICE_NAME" -c "$MATTERMOST_CREDHUB_CONFIG"
    fi

    echo && printf "\e[37mℹ️  Creating $GENAI_CHAT_SERVICE_NAME and $GENAI_EMBEDDINGS_SERVICE_NAME GenAI services ...\e[m\n" && echo
    cf create-service genai $GENAI_CHAT_PLAN_NAME $GENAI_CHAT_SERVICE_NAME
    cf create-service genai $GENAI_EMBEDDINGS_PLAN_NAME $GENAI_EMBEDDINGS_SERVICE_NAME

    echo && printf "\e[37mℹ️  Deploying $APP_NAME application ...\e[m\n" && echo
    cf push $APP_NAME -k 1GB -m 2GB -p $JAR_PATH --no-start --random-route

    echo && printf "\e[37mℹ️  Binding services ...\e[m\n" && echo
    cf bind-service $APP_NAME $PGVECTOR_SERVICE_NAME
    cf bind-service $APP_NAME $GENAI_CHAT_SERVICE_NAME
    cf bind-service $APP_NAME $GENAI_EMBEDDINGS_SERVICE_NAME
    cf bind-service $APP_NAME $MATTERMOST_PROVIDER_SERVICE_NAME

    echo && printf "\e[37mℹ️  Setting environment variables for use by $APP_NAME application ...\e[m\n" && echo
    cf set-env $APP_NAME JAVA_OPTS "-Djava.security.egd=file:///dev/urandom -XX:+UseZGC -XX:+UseStringDeduplication"
    cf set-env $APP_NAME SPRING_PROFILES_ACTIVE "default,cloud,openai,pgvector"
    cf set-env $APP_NAME JBP_CONFIG_OPEN_JDK_JRE "{ jre: { version: 21.+ } }"
    cf set-env $APP_NAME JBP_CONFIG_SPRING_AUTO_RECONFIGURATION "{ enabled: false }"

    echo && printf "\e[37mℹ️  Starting $APP_NAME application ...\e[m\n" && echo
    cf start $APP_NAME
    ;;

teardown)
    cf unbind-service $APP_NAME $MATTERMOST_PROVIDER_SERVICE_NAME
    cf unbind-service $APP_NAME $PGVECTOR_SERVICE_NAME
    cf unbind-service $APP_NAME $GENAI_CHAT_SERVICE_NAME
    cf unbind-service $APP_NAME $GENAI_EMBEDDINGS_SERVICE_NAME

    cf delete-service $MATTERMOST_PROVIDER_SERVICE_NAME -f
    cf delete-service $PGVECTOR_SERVICE_NAME -f
    cf delete-service $GENAI_CHAT_SERVICE_NAME -f
    cf delete-service $GENAI_EMBEDDINGS_SERVICE_NAME -f

    cf delete $APP_NAME -f -r
    ;;

*)
    echo && printf "\e[31m⏹  Usage: setup/teardown \e[m\n" && echo
    ;;
esac
