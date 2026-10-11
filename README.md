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

To load local development values from an ignored `.env` file, run:

```sh
set -a
source .env
set +a
SPRING_PROFILES_ACTIVE=dev bash ./mvnw spring-boot:run
```

Use a JDBC URL (`jdbc:postgresql://host/database?...`) in `DB_URL`; keep the
database username and password in their own variables. `.env` is ignored by
Git and must never be committed.

Set `JWT_SECRET` to a random value of at least 32 bytes. OAuth sign-in requires
`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, and
`GITHUB_CLIENT_SECRET`. The frontend origin can be configured with `FRONTEND_URL`. Set
`COOKIE_SECURE=true` when serving over HTTPS; this also sets the refresh cookie
to `SameSite=None` for cross-site production frontend/API deployments. Keep it
false for local HTTP development, which uses `SameSite=Lax`.

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

The frontend must include credentials in login, refresh, and logout requests
(`credentials: "include"` with `fetch`, or `withCredentials: true` with Axios)
so the browser stores and sends the HttpOnly refresh cookie.

Select a Render instance with no more than 512 MiB of memory. The Docker image
limits the JVM heap to 160 MiB and metaspace to 192 MiB, and bounds code cache, direct memory,
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

The JVM is configured with a 160 MiB maximum heap and 192 MiB maximum metaspace,
plus bounded code cache and direct memory to leave room for native memory under the
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

## Transactions

Every transaction has a required `paymentMode`: `CASH`, `CREDIT`, or
`BANK_ACCOUNT`. Include it when creating or updating a transaction; it is also
returned with transaction data. Existing transactions are assigned `CASH` by
the database migration.

`GET /api/v1/transactions` supports paginated filters for `id`, `type`
(`INCOME` or `EXPENSE`), `paymentMode`, `from`, `to`, `categoryId`, and
`subCategoryId`. Filters can be combined. Dates are inclusive and use
`YYYY-MM-DD`; when both `from` and `to` are provided, the range cannot exceed
366 days.
Without an ID, type, payment mode, category, or sub-category filter, the
endpoint retains its existing current-month default. Pagination and sorting
continue to use `page`, `size`, `sortBy`, and `direction`.

## Budget limits

All routes below require authentication. Each request operates only on the
authenticated user's limits; do not send a `userId`. `categoryId` is required
for every limit. Omit `subCategoryId` or send it as `null` for a category-wide
limit. Include a `subCategoryId` for a sub-category limit. The sub-category
must belong to the supplied category. A user can have a category limit and
separate limits for each of its sub-categories.

`limit` is a positive JSON number with up to 15 integer digits and 4 decimal
places. The API stores the numeric amount as provided; it does not accept a
currency field or perform currency conversion. Only active categories and
active sub-categories can be selected. An unset limit has no row and is
therefore absent from the list response.

### List all configured limits

`GET /api/v1/budget-limits`

Success: `200 OK`. The response is an array; it is empty (`[]`) if the user has
not configured any limits.

```json
[
  {
    "id": "a33d58fc-92ba-4f0c-86ac-25f48dcd4a0e",
    "categoryId": "994039c0-f227-4b19-94e2-022e7744cf14",
    "subCategoryId": null,
    "limit": 5000.00
  },
  {
    "id": "69eb19f5-1e22-47df-8996-5af542f3ae8c",
    "categoryId": "994039c0-f227-4b19-94e2-022e7744cf14",
    "subCategoryId": "27d36f40-e982-4c57-8f18-f67029f75cb5",
    "limit": 1200.00
  }
]
```

Each item contains:

| Field | Type | Meaning |
| --- | --- | --- |
| `id` | UUID string | Identifier for this configured limit |
| `categoryId` | UUID string | Category for the limit; present for both scopes |
| `subCategoryId` | UUID string or `null` | `null` for a category-wide limit; otherwise the limited sub-category |
| `limit` | number | Positive configured amount |

### Create or set a limit

`POST /api/v1/budget-limits`

This endpoint creates a limit if the scope is new, or updates the existing
limit for the same user/category/sub-category scope. The existing `id` remains
the same on update. The request body for a category-wide limit is:

```json
{
  "categoryId": "994039c0-f227-4b19-94e2-022e7744cf14",
  "limit": 5000
}
```

The request body for a sub-category limit is:

```json
{
  "categoryId": "994039c0-f227-4b19-94e2-022e7744cf14",
  "subCategoryId": "27d36f40-e982-4c57-8f18-f67029f75cb5",
  "limit": 1200
}
```

Success: `200 OK`, returning the created or updated limit in the same shape as
one list item:

```json
{
  "id": "a33d58fc-92ba-4f0c-86ac-25f48dcd4a0e",
  "categoryId": "994039c0-f227-4b19-94e2-022e7744cf14",
  "subCategoryId": null,
  "limit": 5000.00
}
```

### Replace a limit

`PUT /api/v1/budget-limits`

The request and response formats and behavior are the same as `POST`: it
creates the scope if absent, or replaces its amount if present.

### Remove a limit

`DELETE /api/v1/budget-limits?categoryId=<category-uuid>`

Removes the current user's category-wide limit. To remove a sub-category
limit, include its ID:

`DELETE /api/v1/budget-limits?categoryId=<category-uuid>&subCategoryId=<sub-category-uuid>`

Success: `204 No Content` (empty response body). If the current user has no
limit for the requested scope, the API returns `404 Not Found`.

### Validation and authentication

- Missing/invalid IDs, a non-positive amount, more than 15 integer digits or
  more than 4 decimal places, an inactive category, or an inactive/unrelated
  sub-category is rejected as a bad request (`400`).
- Requests without valid authentication are rejected (`401`).
- The list is user-specific: limits belonging to another user are never
  returned or changed.
