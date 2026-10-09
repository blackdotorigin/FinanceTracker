# FinanceTracker

FinanceTracker is a Spring Boot application for managing users, categories, and
financial transactions.

## Configuration

The application uses PostgreSQL and Flyway migrations. Create the database
before starting the application; the default connection targets
`jdbc:postgresql://localhost:5432/financeTracker` with the `postgres` user.
Override these defaults with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

Set `JWT_SECRET` to a random value of at least 32 bytes. OAuth sign-in requires
`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, and
`GITHUB_CLIENT_SECRET`. The frontend origin can be configured with
`FRONTEND_URL`; set `COOKIE_SECURE=true` when serving over HTTPS.

Do not commit production credentials. Users are created through the normal
registration flow; the application no longer creates a bootstrap user.
