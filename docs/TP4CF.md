## How to run on Tanzu Platform for Cloud Foundry

### Target a foundation

```bash
cf api {cloud_foundry_foundation_api_endpoint}
```

> Replace `{cloud_foundry_foundation_api_endpoint}` above with an API endpoint

Sample interaction

```bash
cf api api.sys.dhaka.cf-app.com
```

### Authenticate

Interactively

```bash
cf login
```

With single sign-on

```bash
cf login --sso
```

With a username and password

```bash
cf login -u {username} -p "{password}"
```

> Replace `{username}` and `{password}` above respectively with your account's username and password.

### Target space

If your user account has `OrgManager` and `SpaceManager` permissions, then you can create your own organization and space with

```bash
cf create-org {organization_name}
cf create-space -o {organization_name} {space_name}
```

> Replace `{organization_name}` and `{space_name}` above with names of your design

To target a space

```bash
cf target -o {organization_name} -s {space_name}
```

> Replace `{organization_name}` and `{space_name}` above with an existing organization and space your account has access to

Sample interaction

```bash
cf create-org zoolabs
cf create-space -o zoolabs dev
cf target -o zoolabs -s dev
```

### Verify services

Verify that the foundation has the service offerings required

```bash
cf m -e genai
cf m -e postgres
cf -m e credhub
```

Sample interaction

```bash
❯ cf m -e genai
Getting service plan information for service offering genai in org zoolabs / space dev as chris.phillipson@broadcom.com...

broker: genai-service
   plan               description                                                                                       free or paid   costs
   llama3.1           Access to the llama3.1 model. Capabilities: chat, tools. Aliases: gpt-turbo-3.5.                  free
   llava              Access to the llava model. Capabilities: chat, vision.                                            free
   nomic-embed-text   Access to the nomic-embed-text model. Capabilities: embedding. Aliases: text-ada-embedding-002.   free

❯ cf m -e postgres
Getting service plan information for service offering postgres in org zoolabs / space dev as chris.phillipson@broadcom.com...

broker: postgres-odb
   plan                       description                             free or paid   costs
   on-demand-postgres-small   A single e2-micro with 2GB of storage   free

❯ cf m -e credhub
Getting service plan information for service offering credhub in org zoolabs / space dev as chris.phillipson@broadcom.com...

broker: credhub-broker
   plan      description                                           free or paid   costs
   default   Stores configuration parameters securely in CredHub   free
```

### Create a Mattermost instance

Visit [StackHero](https://www.stackhero.io/en/), create an account, a project, and launch an instance of Mattermost.

> This is an optional step and can be skipped if you already have and administrator credentials to an instance along with a base URL.
> When you sign-up for a new account, you'll receive a USD 50 credit that you can use over a 1-month duration.  You can launch new Hobby instances of Mattermost.  If you don't supply a payment method each time you launch an instance, it will be automatically destroyed after 24-hours.

### Create configuration for Mattermost instance

Then, create a configuration file

```bash
mkdir -p $HOME/.mattermost
touch $HOME/.mattermost/config
```

Edit the above file and make sure it has either comination of the following environment variables exported:

> Replace the values below enclosed in `<>` with appropriate values for your instance.  Note: if the instance is hosted on Stackhero, the first attempt to login will force you to create a new account which will grant admin access.

#### with username and password

```python
export MATTERMOST_BASE_URL=<mattermost-base-url>
export MATTERMOST_USERNAME='<your_admin_account_username>'
export MATTERMOST_PASSWORD='<your_admin_account_password>'
```

#### with personal access token

```python
export MATTERMOST_BASE_URL=<mattermost-base-url>
export MATTERMOST_PERSONAL_ACCESS_TOKEN='<your_perosnal_access_token>'
```

### Clone and build the app

```bash
gh repo clone pacphi/mattermost-ai-service
cd mattermost-ai-service
mvn clean package -Dvector-db-provider=pgvector
```

### Deploy

Take a look at the deployment script

```bash
cat scripts/deploy-on-tp4cf.sh
```

> Make any required edits to the environment variables for the services and plans.

Execute the deployment script

```bash
./scripts/deploy-on-tp4cf.sh setup
```

To teardown, execute

```bash
./scripts/deploy-on-tp4cf.sh teardown
```

### Inspect and/or update the PgVector store database instance

Create a service key for the service instance, with:

```bash
cf create-service-key mais-db cf-psql
```

Sample interaction

```bash
❯ cf create-service-key mais-db cf-psql
Creating service key cf-psql for service instance mais-db as chris.phillipson@broadcom.com...
OK

❯ cf service-key mais-db cf-psql
Getting key cf-psql for service instance mais-db as chris.phillipson@broadcom.com...

{
  "credentials": {
    "db": "postgres",
    "hosts": [
      "q-s0.postgres-instance.dhaka-services-subnet.service-instance-967aa687-1b73-4448-8505-dca0fa2ee079.bosh"
    ],
    "jdbcUrl": "jdbc:postgresql://q-s0.postgres-instance.dhaka-services-subnet.service-instance-967aa687-1b73-4448-8505-dca0fa2ee079.bosh:5432/postgres?user=pgadmin&password=Z8ybS105mdY7i6h923H4",
...
```

Open two terminal sessions.

In the first session, execute:

```bash
❯ cf ssh -L 55432:q-s0.postgres-instance.dhaka-services-subnet.service-instance-967aa687-1b73-4448-8505-dca0fa2ee079.bosh:5432 mais
vcap@128bacbc-b0f1-46b5-64cb-709c:~$
```

> We are creating a tunnel between the host and the service instance via the application. The host will listen on port 55432.

Switch to the second session, then execute:

```bash
❯ psql -U pgadmin -W postgres -h 127.0.0.1 -p 55432
Password:
```

Enter the password.  See that it is specified at the end fo the "jdbcUrl" JSON fragment above.

And you should see:

```bash
psql (12.9 (Ubuntu 12.9-0ubuntu0.20.04.1), server 15.6)
WARNING: psql major version 12, server major version 15.
         Some psql features might not work.
Type "help" for help.

postgres=#
```

From here you can show tables with `\dt`

```bash
postgres=# \dt
            List of relations
 Schema |     Name     | Type  |  Owner
--------+--------------+-------+---------
 public | vector_store | table | pgadmin
(1 row)
```

You can describe the table with `\d vector_store`

```bash
postgres=# \d vector_store
                     Table "public.vector_store"
  Column   |     Type     | Collation | Nullable |      Default
-----------+--------------+-----------+----------+--------------------
 id        | uuid         |           | not null | uuid_generate_v4()
 content   | text         |           |          |
 metadata  | json         |           |          |
 embedding | vector(1536) |           |          |
Indexes:
    "vector_store_pkey" PRIMARY KEY, btree (id)
    "spring_ai_vector_index" hnsw (embedding vector_cosine_ops)
```

And you can execute arbitrary SQL (e.g., `SELECT * from vector_store`).

If you need to ALTER the dimensions of the `embedding` column to adapt to the limits of an embedding model you chose, then you could, for example, execute:

```bash
-- Step 1: Drop the existing index
DROP INDEX IF EXISTS spring_ai_vector_index;

-- Step 2: Drop the existing column
ALTER TABLE public.vector_store DROP COLUMN embedding;

-- Step 3: Add the new column with the desired vector size
ALTER TABLE public.vector_store ADD COLUMN embedding vector(768);

-- Step 4: Recreate the index
CREATE INDEX spring_ai_vector_index ON public.vector_store USING hnsw (embedding vector_cosine_ops);
```

To exit, just type `exit`.
