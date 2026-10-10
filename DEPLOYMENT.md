# Deploy FinanceTracker with GitHub Actions and Render

This guide takes the backend from this GitHub repository to a Docker image in
GitHub Container Registry (GHCR), then runs that image as a Render Web Service.
The workflow builds and publishes the image; Render runs it. The app's database
and runtime credentials are configured in Render, not stored in GitHub or the
database.

## 1. Accounts and repository

1. Create or sign in to a [GitHub account](https://github.com/).
2. Make sure you have access to the `blackdotorigin/FinanceTracker` repository.
   If starting from scratch, create a repository and push this project to it.
3. Create or sign in to a [Render account](https://render.com/).
4. Confirm the repository's default deployment branch is `main`, because the
   workflow publishes images when code is pushed to `main`.

## 2. Add and run the GitHub Actions workflow

The workflow is already in
`.github/workflows/publish-image.yml`. Commit and push it, along with the
application changes, to `main`:

```sh
git add .github/workflows/publish-image.yml Dockerfile .dockerignore \
  README.md DEPLOYMENT.md src/main/resources/application.properties \
  src/main/resources/application-dev.properties
git commit -m "Add GitHub Actions Docker publishing"
git push origin main
```

In GitHub, open the repository and select **Actions**. Select **Build and
publish Docker image** and wait for the run to finish successfully. The workflow
uses GitHub's automatically provided `GITHUB_TOKEN` to publish the image; you
do not need a Docker Hub account or a separate registry password.

The workflow compiles and packages the app but skips test execution, so it
does not need a database connection. The deployed app connects to Neon using
the environment variables configured in Render. The frontend does not need to
be deployed to build or publish the backend image.

The published image names are:

```text
ghcr.io/blackdotorigin/financetracker:latest
ghcr.io/blackdotorigin/financetracker:sha-<commit>
```

The `latest` tag is updated by each successful push to `main`. The SHA tag
identifies a specific build.

## 3. Make the GHCR image available to Render

After the first successful Actions run:

1. In GitHub, open the owner account or organization **blackdotorigin**, then
   open **Packages** and select the `financetracker` container package. It may
   also be linked from the repository's package section.
2. Open the package settings and choose **Change visibility** to make it
   **Public**. Confirm the change. This lets Render pull the image without a
   registry username or password.

If the package must stay private, configure Render's private image registry
credentials instead. Do not use a workflow's `GITHUB_TOKEN` for Render: that
token is temporary and only available to the Actions run.

## 4. Create the Render Web Service

1. In the Render dashboard, select **New +** → **Web Service**.
2. Choose the option to deploy an **Existing Image** (sometimes shown as
   **Deploy an existing image from a registry**).
3. Enter:

   ```text
   ghcr.io/blackdotorigin/financetracker:latest
   ```

4. Name the service, choose a region, and choose a plan with at most **512 MiB
   RAM** if that plan is available for your account.
5. Set the service's port to **8080** (or set `PORT=8080` in its environment).
   The application listens on Render's `PORT` value and defaults to 8080.
6. Create the service. It will initially fail until its runtime environment
   variables are configured in the next step; add those before relying on the
   service.

## 5. Add the backend environment variables in Render

Open the service's **Environment** page and add these variables. Use the
current password from Neon; do not put credentials in source code, this guide,
or GitHub Actions:

| Key | Value |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<Neon-host>/financeTracker?sslmode=require&channel_binding=require` |
| `DB_USERNAME` | The database username shown by Neon |
| `DB_PASSWORD` | The current Neon database password |
| `JWT_SECRET` | A newly generated random secret of at least 32 bytes |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID, if Google sign-in is used |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret, if Google sign-in is used |
| `GITHUB_CLIENT_ID` | GitHub OAuth client ID, if GitHub sign-in is used |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth client secret, if GitHub sign-in is used |
| `FRONTEND_URL` | The deployed frontend's exact origin, such as `https://app.example.com` |
| `COOKIE_SECURE` | `true` when using HTTPS |
| `PORT` | `8080` |

In Neon, copy the host and database name from its connection details. Use the
JDBC prefix `jdbc:postgresql://` for `DB_URL`; do not paste the full
`postgresql://user:password@...` URI into `DB_URL`. Keep the username and
password in their separate variables.

For OAuth sign-in, also update each provider's authorized callback URL to the
backend's Render URL and the provider's callback path used by this app:

```text
https://<your-render-service>.onrender.com/login/oauth2/code/google
https://<your-render-service>.onrender.com/login/oauth2/code/github
```

Save the environment changes and redeploy the service. Flyway applies the
application's database migrations when the backend starts. Check the Render
service logs for startup errors.

## 6. Automatically deploy new images (optional)

The Actions workflow can call a Render deploy hook after publishing each image:

1. In the Render service, open **Settings** and create/copy a **Deploy Hook**.
2. In the GitHub repository, open **Settings** → **Secrets and variables** →
   **Actions** → **New repository secret**.
3. Name the secret `RENDER_DEPLOY_HOOK_URL` and paste the Render hook URL as
   its value.
4. Push a change to `main`. Actions rebuilds and publishes `latest`, then calls
   the hook to start a Render deploy.

Do not add the database password, JWT secret, or OAuth client secrets as GitHub
Actions secrets for this workflow; the running application needs them in
Render's Environment settings. The only optional Actions secret here is the
Render deploy-hook URL.

## Resource limits

The image configures Java with a 160 MiB maximum heap, 192 MiB maximum metaspace,
bounded native memory, two visible processors, and a 20-thread Tomcat request
pool. Select a Render
instance with no more than 512 MiB RAM to enforce the container memory limit;
the Docker image cannot set the hosting provider's memory plan.
