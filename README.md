# FinanceTracker

FinanceTracker is a Spring Boot application for managing users, categories, and
financial transactions.

## Configuration

The application uses PostgreSQL and Flyway migrations. Create the database
before starting the application; the default connection targets
`jdbc:postgresql://localhost:5432/financeTracker` with the `postgres` user.
Override these defaults with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

For local development against Neon, activate the `dev` profile with
`SPRING_PROFILES_ACTIVE=dev`. The profile sets the Neon URL and username; supply
the database password through `DB_PASSWORD` in the environment rather than
committing it to a properties file.

Set `JWT_SECRET` to a random value of at least 32 bytes. OAuth sign-in requires
`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, and
`GITHUB_CLIENT_SECRET`. The frontend origin can be configured with
`FRONTEND_URL`; set `COOKIE_SECURE=true` when serving over HTTPS.

### GitHub Actions and Render deployment

For the full account setup and deployment walkthrough, see
[DEPLOYMENT.md](./DEPLOYMENT.md).

The GitHub Actions workflow at `.github/workflows/publish-image.yml` compiles
and packages the app with Maven (test execution is skipped), builds the Docker
image, and publishes it to GitHub Container Registry (GHCR) on pushes to
`main`. In Render, create a Web Service using the published Docker image
`ghcr.io/blackdotorigin/financetracker:latest`. Make the GHCR package public or
configure Render with credentials that can read the private package.

To trigger a Render deployment after each successful image publish, add the
Render deploy-hook URL as the GitHub Actions repository secret
`RENDER_DEPLOY_HOOK_URL`. Without that secret, publish the new image and deploy
it from Render.

Set these values in the Render service's Environment settings; do not commit
them to GitHub:

- `DB_URL`: Neon JDBC URL (`jdbc:postgresql://...` with SSL enabled)
- `DB_USERNAME` and `DB_PASSWORD`
- `JWT_SECRET`: a random value of at least 32 bytes
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`
- `FRONTEND_URL` and `COOKIE_SECURE=true` for the deployed frontend

Select a Render instance with no more than 512 MiB of memory. The Docker image
limits the JVM heap to 192 MiB and bounds metaspace, code cache, direct memory,
and thread-stack size. Tomcat is configured for at most 20 request threads;
the JVM sees two processors to keep its internal pools small. The Render
instance setting enforces the container memory limit.

### Building and running with Docker locally

Build the multi-stage image from the repository root:

```sh
docker build -t financetracker .
```

Run it with a 512 MiB container memory limit:

```sh
docker run --rm --name financetracker --memory=512m --cpus=2 -p 8080:8080 \
  --env DB_URL --env DB_USERNAME --env DB_PASSWORD --env JWT_SECRET \
  --env GOOGLE_CLIENT_ID --env GOOGLE_CLIENT_SECRET \
  --env GITHUB_CLIENT_ID --env GITHUB_CLIENT_SECRET \
  financetracker
```

The JVM is configured with a 192 MiB maximum heap and bounded metaspace,
code cache, and direct memory to leave room for native memory under the
container limit. Export database and auth settings in the shell before running
the command; do not add secret values to the command or the repository.
`localhost` inside the container refers to the container itself.

## User activity

The authenticated endpoint `GET /api/v1/user-activity` returns daily activity
for the current user for the last 30 UTC dates, including inactive dates. Set
`days` to request between 1 and 366 dates, for example
`GET /api/v1/user-activity?days=30`. To request a calendar year, use
`GET /api/v1/user-activity?year=2020`; `days` and `year` cannot be combined.
A date is active when the user logged in or has at least one transaction dated
that day. Transaction totals are maintained as transactions are created,
moved to another date, or deleted.

The response includes `from`, `to`, and a `days` array. Each day contains
`date`, `active`, `transactionCount`, and `loginCount`. Activity is stored once
per user/date; the V9 migration also backfills dates from existing transactions.
Historical login activity is unavailable before login tracking was introduced.

## Transaction graph

The authenticated endpoint `GET /api/v1/transactions/graph?period=week`
returns up to 10 of the current user's transactions in the current calendar
week, ordered by transaction date descending. Use `period=month` for the
current calendar month or `period=year` for the current calendar year. Weeks
start on Monday; invalid period values return a bad request.
