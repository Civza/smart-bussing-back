# Staging environment

Staging is a full copy of the backend running on Render against its own Supabase
database (same schema as prod, no data). Use it to check changes before they reach
production.

## Branch flow

```
feature/* ──PR──▶ staging ──(audited PR)──▶ main
                     │                        │
             auto-deploys to            production
             Render staging
```

1. Branch off `staging` (or `main`) and open a PR into `staging`. CI (`.github/workflows/tests.yml`)
   runs on PRs to `staging` and on pushes to it.
2. Merging into `staging` auto-deploys the Render service `smart-bussing-back-staging`.
3. Once staging looks good, open a PR `staging` → `main`. That PR is the audit/review gate for production.

## What lives where

| In git | In Render (per service, env vars) |
| --- | --- |
| `render.yaml` (Blueprint: service, branch, region, plan, health check, env var *names*) | `PROD_DB_URL`, `PROD_DB_USERNAME`, `PROD_DB_PASSWORD` |
| `Dockerfile`, app code, `application.properties` (reads env vars only) | `MAPBOX_API_KEY` |
| `database/schema_backup.sql`, `database/migrations/` | `DLP_SERVICE_URL` |

Every secret is declared with `sync: false` in `render.yaml`, so Render asks for the value
when the Blueprint is first applied and never overwrites it from git. Never commit a value.
`PORT=8080` is the only literal value in the Blueprint (it tells Render which port Spring listens on).

## Staging database (Supabase)

- Project ref: `vldfjrqwnlzrgxwnarou` (region us-east-1; Render service is in `virginia` next to it).
- Schema loaded from `database/schema_backup.sql` + `database/migrations/V1__add_rol_and_supabase_oauth_sync.sql`. No data.
- Connect through the **Session pooler** (Supabase dashboard → Connect → Session pooler), port 5432.

Set these in Render → `smart-bussing-back-staging` → Environment:

| Variable | Value |
| --- | --- |
| `PROD_DB_URL` | `jdbc:postgresql://<session-pooler-host>:5432/postgres?sslmode=require` |
| `PROD_DB_USERNAME` | `postgres.vldfjrqwnlzrgxwnarou` |
| `PROD_DB_PASSWORD` | the staging project's DB password (Supabase → Project Settings → Database) |
| `MAPBOX_API_KEY` | a Mapbox token (ideally a separate staging token) |
| `DLP_SERVICE_URL` | full URL of a reachable dlp-service, e.g. `https://<dlp-host>/api/dlp/scan` |

The variables are named `PROD_*` because that is what `application.properties` reads; on the
staging service they must hold **staging** values.

### DLP service

`dlp-service/` is not part of the Blueprint. The backend calls it with a 1.2 s connect / 2 s read
timeout and, on any failure, falls back to a local word filter. A free Render instance sleeps
when idle and takes far longer than 2 s to wake, so a free `dlp-service` would almost always
trigger the fallback anyway. Options:

- Leave `DLP_SERVICE_URL` pointing at an unreachable/placeholder URL and accept the local fallback (fine for most staging tests).
- Deploy `dlp-service` separately (its own Dockerfile, port 8000, `GET /health`) on a paid/always-on instance and set the URL.

## Refreshing the staging schema from prod

1. In your own shell (never commit it), export the **prod** connection URL in libpq form:
   ```sh
   export PG_DB_URL='postgresql://<user>:<password>@<prod-host>:5432/postgres'
   docker compose -f docker-compose-test.yml run --rm schema-dump
   ```
   This overwrites `database/schema_backup.sql` (schema only, `public` schema). Review the diff.
2. Apply it to staging (a staging URL, also in libpq form):
   ```sh
   export STAGING_PG_URL='postgresql://postgres.vldfjrqwnlzrgxwnarou:<password>@<session-pooler-host>:5432/postgres?sslmode=require'
   docker run --rm -i postgres:17-alpine psql "$STAGING_PG_URL" -v ON_ERROR_STOP=1 < database/schema_backup.sql
   ```
   Staging already has the tables, so drop the old objects first (e.g. recreate the `public`
   schema and restore Supabase's default grants on it) or apply only the delta. Then apply any
   migration in `database/migrations/` that is not already in the dump.
3. Unset both variables (`unset PG_DB_URL STAGING_PG_URL`) and redeploy / restart the Render service.

## Gotchas

- **Never point staging at the prod DB URL.** Double-check `PROD_DB_URL` contains `vldfjrqwnlzrgxwnarou` / the staging pooler host.
- **`spring.jpa.hibernate.ddl-auto=validate`**: if the staging schema drifts from the JPA entities
  (missing table/column, wrong type), the app fails at startup and the deploy fails the health check.
  Refresh the schema or add a migration.
- **Free Supabase projects pause** after a period of inactivity. Restore the project from the
  Supabase dashboard if the app suddenly can't connect.
- **Free Render web services sleep** after ~15 min without traffic; the first request afterwards
  takes up to a minute while the container starts. Free instances also have 512 MB RAM.
- Health check path is `GET /api/v1/ruta` (unauthenticated, hits the DB). The app's rate limiter
  allows 100 req/min per IP on that path.
