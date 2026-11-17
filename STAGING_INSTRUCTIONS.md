# Staging: instructions to continue (backend)

Snapshot of 2026-11-17. This file is the hand-off for continuing the staging work on another
machine: the agent context from the original machine does not travel, so everything needed is
here or in the linked files. Setup reference: [`docs/STAGING.md`](docs/STAGING.md). Rules:
[`AGENT_CONTEXT/CLAUDE.md`](AGENT_CONTEXT/CLAUDE.md). History: [`AGENT_CONTEXT/WORKLOG.md`](AGENT_CONTEXT/WORKLOG.md).

The same kind of file exists on the `staging` branch of the other two repos
(`Dusuan/smart-bussing-web`, `Dusuan/SmartBussingMobile`).

## 1. Environment map

| Piece | Staging | Production |
| --- | --- | --- |
| Backend | Render service `smart-bussing-back` (`srv-dbt15auc6qcc73aed9n0`, workspace "Dusuan's Workspace", Docker, free plan, Oregon). URL: https://smart-bussing-back-txi2.onrender.com. Tracks branch `staging`, auto-deploy on. | Railway `https://smart-bussing-back-production.up.railway.app` (Railway is being shut down). |
| Database | Supabase org `SmartBussingDusuan`, project "Dusuan's Project" (ref `vldfjrqwnlzrgxwnarou`, us-east-1, Postgres 17). | Supabase org `SmartBussing`, project SmartBussingREBUILD (ref `rbbjolrpjyfgnwrhhqmx`). |
| Web dashboard | Vercel preview of `smart-bussing-web` branch `staging`. **Not configured yet** (KAN-35). | https://smart-bussing.vercel.app |
| Mobile app | EAS build with `EXPO_PUBLIC_API_URL` on staging. **Not configured yet** (KAN-35). | Default URL in `services/apiConfig.ts`. |

Note: the mobile app's production default is `https://smart-bussing-back.onrender.com/api/v1`, the web
fallback is the Railway URL above. That Render URL is not in "Dusuan's Workspace"; its owner was not
verified. Decide the real production backend before the first `staging` → `main` merge.

## 2. Branch flow (mandatory, see `AGENT_CONTEXT/CLAUDE.md`)

```
feature/* (cut from staging) ──PR──▶ staging ──(audited PR)──▶ main
                                       │                         │
                              Render staging deploy          production
```

Tests first (red → green), never adjust tests to fit the code, never push without an explicit instruction.

## 3. State on 2026-11-17

- `staging` on GitHub: `539c541` (`render.yaml`, `docs/STAGING.md`, CI on `staging`), `f8b87ee` (workflow
  and test-first rules), plus this file.
- Staging database: tracked migrations `baseline_schema_from_prod_dump` (prod schema from
  `database/schema_backup.sql`) and `v1_add_rol_and_supabase_oauth_sync`. **No data. V2 not applied.**
- Render env vars already set (values only in Render, never in git):
  `PORT=8080`, `PROD_DB_URL` (JDBC to the staging session pooler
  `aws-1-us-east-1.pooler.supabase.com:5432`, `?sslmode=require`), `PROD_DB_USERNAME`
  (`postgres.vldfjrqwnlzrgxwnarou`), `PROD_DB_PASSWORD`, `MAPBOX_API_KEY` (a public `pk.` token is
  enough: the backend only calls the Directions API), `DLP_SERVICE_URL=http://localhost:8000/api/dlp/scan`
  (unreachable on purpose, so the local fallback filter is used at once), `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75`.
- Last verified deploy: `869925f`. `GET /api/v1/ruta` → 200 with an empty list; the staging DB showed the
  app's 5 pooled connections.
- Open PR: `feature/auth` → `staging` (Supabase/own JWT auth layer, BCrypt, `/user/me`, access matrix;
  KAN-14, KAN-16, KAN-21). **Not merged.**
- Only on the original machine, not on GitHub (lost unless pushed from there): commit `8f6dceb` on local
  `main` (CI uploads the prod schema dump as an artifact `schema-dump-<run>-<sha>`).

## 4. Before merging `feature/auth` into `staging` (order matters)

1. **Render env vars (KAN-33).** Render → service `smart-bussing-back` (KAN-33 calls it
   `smart-bussing-back-staging`; the real name is `smart-bussing-back`) → Environment:
   - `APP_JWT_SECRET`: `openssl rand -base64 48`, a value different from production.
   - `SUPABASE_URL`: `https://vldfjrqwnlzrgxwnarou.supabase.co`
   - `CORS_ALLOWED_ORIGINS`: `<web staging preview origin>,http://localhost:5173`

   Without `APP_JWT_SECRET` and `SUPABASE_URL` the app refuses to start, so the deploy after the merge fails.
2. **V2 migration on the staging DB (KAN-32).** Supabase → "Dusuan's Project" → SQL Editor → paste
   `database/migrations/V2__oauth_users_without_password.sql` (on `feature/auth`) → Run. Then
   `SELECT count(*) FROM public.usuario WHERE password = 'OAUTH_GOOGLE';` → `0`.
3. **Review and merge the PR.** Check that the 8 existing test files it changes (listed in the PR) match
   the decisions taken (for example 403 → 401 when there is no token).
4. **Verify the deploy:** `curl -i https://smart-bussing-back-txi2.onrender.com/api/v1/ruta` → 200;
   `curl -i https://smart-bussing-back-txi2.onrender.com/api/v1/user` → 401.
5. Continue with KAN-35 (Google OAuth on staging Supabase, first ADMIN, web and mobile env vars), then the
   web and mobile `feature/auth` PRs, which need this backend.

## 5. Other open items

- Confirm that pushes to `staging` trigger a Render deploy on their own (`render deploys list srv-dbt15auc6qcc73aed9n0`).
  The `render.yaml` Blueprint is unused: the repo belongs to the personal account `Civza`, which does not
  have the Render GitHub App. Ask the owner to install it if auto-deploy does not work.
- Health check is not set: `render services update srv-dbt15auc6qcc73aed9n0 --health-check-path /api/v1/ruta`
  (that endpoint queries the DB, so a DB outage also fails the check).
- Region: service in Oregon, DB in us-east-1 (about 60–70 ms per query). Changing it means a new service in Virginia.
- Seed data: staging is empty. Fake routes and stops are needed for end-to-end tests.
- `GET /` and `/favicon.ico` returned 500 on `869925f` (`GlobalControllerAdvice` turns
  `NoResourceFoundException` into a server error instead of 404). Re-check after the auth merge.
- Before `staging` → `main`: KAN-34 (signing keys on prod Supabase), KAN-33 and KAN-32 for production,
  and the production backend host (see section 1).
- Refresh the staging schema when prod changes: steps in `docs/STAGING.md`.

## 6. Setting up the other machine

```sh
git clone https://github.com/Civza/smart-bussing-back.git && cd smart-bussing-back
git switch staging            # or: gh pr checkout <PR number> to review feature/auth
```

- Java 17 (for example via SDKMAN), Maven 3.9 or `./mvnw`, Docker (the integration tests use the Postgres
  from `docker-compose-test.yml`), GitHub CLI (`gh auth login`).
- Render CLI: `render login`, then `render workspace set` → "Dusuan's Workspace". Useful:
  `render services`, `render deploys list srv-dbt15auc6qcc73aed9n0`, `render logs -r srv-dbt15auc6qcc73aed9n0 --limit 200`.
- Claude connectors: the Supabase connector must be authorized for the org `SmartBussingDusuan` (staging).
  Jira: site `meta-42156173.atlassian.net`, project `KAN`.
- Secrets live only in Render and Supabase: DB password (Supabase → Project Settings → Database, reset it if
  lost), Mapbox `pk.` token, `APP_JWT_SECRET`.

## 7. Jira

KAN-31 staging environment (In Progress) · KAN-32 V2 migration · KAN-33 backend env vars ·
KAN-34 prod Supabase signing keys · KAN-35 staging OAuth, Vercel/EAS vars, first ADMIN.
