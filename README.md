# Dealflow360

Toolchain monorepo: Vite React TypeScript frontend, Spring Boot 3 (Gradle, JDBC, MySQL) backend, and Docker Compose. There is no sample business domain. The only application endpoint is operational health (`GET /api/health`), which reports backend process status and MySQL via JDBC `SELECT 1`.

Example payloads:

```json
{
  "success": true,
  "status": 200,
  "data": { "backend": "up", "database": "up" },
  "timestamp": "2026-09-05T03:40:00Z"
}
```

```json
{
  "success": false,
  "status": 503,
  "data": { "backend": "up", "database": "down" },
  "timestamp": "2026-09-05T03:40:00Z"
}
```

HTTP 200 when `success` is true; HTTP 503 when MySQL is down. Errors use the same envelope with `data.code`, `data.message`, and `data.path`. The JVM still starts if MySQL is unreachable (`spring.datasource.hikari.initialization-fail-timeout: -1`).

## Ports

| Service | Local | Docker host mapping |
| --- | --- | --- |
| Frontend (Vite) | `5178` | `18081` → preview `:5178` |
| Backend | `18080` | `18080` |
| MySQL | `3307` | `3307` → `:3306` |

## Environment

Backend (`application.properties`) reads:

- `MYSQL_HOST` default `127.0.0.1`
- `MYSQL_PORT` default `3307`
- `MYSQL_DATABASE` default `dealflow360`
- `MYSQL_USER` default `dealflow`
- `MYSQL_PASSWORD` default `dealflow360`
- `SERVER_PORT` default `18080`

Frontend: `VITE_API_URL` default `http://127.0.0.1:18080` (local Vite talks to the API on 18080). Docker UI builds with an empty `VITE_API_URL` so the browser calls `/api/...` and Vite preview proxies to the backend.

MySQL credentials in Compose: database `dealflow360`, user `dealflow` / `dealflow360`, root `dealflow360_root`.

## Frontend (local)

```bash
cd frontend
npm install
npm run dev
```

Vite binds `0.0.0.0:5178`.

## Backend (local)

Java 21. From `backend/`:

```bash
./gradlew bootRun
```

Point JDBC at Compose MySQL on the host (`127.0.0.1:3307`, defaults already match).

## Docker Compose

Requires a Docker daemon. From `dealflow360/`:

MySQL only:

```bash
docker compose up -d mysql
docker compose ps
```

Wait until `mysql` is healthy, then health-check a locally running API:

```bash
curl -sS http://127.0.0.1:18080/api/health
```

MySQL + backend images:

```bash
docker compose up -d --build mysql backend
curl -sS http://127.0.0.1:18080/api/health
```

Full stack including Vite preview frontend:

```bash
docker compose up -d --build
```

- API: `http://127.0.0.1:18080/api/health`
- UI: `http://127.0.0.1:18081`
- MySQL: `127.0.0.1:3307`

If `docker` is not installed or the daemon is down, Compose files still apply; run MySQL elsewhere on `3307` or accept `database: down` until a server is available.

## Layout

- `frontend/src/app` — providers and router shell
- `frontend/src/pages` — Home
- `frontend/src/components/{ui,layout,common}`
- `frontend/src/{hooks,services,stores,types,utils,constants,assets}`
- `backend` package root `com.dealflow360`
  - `app` Spring Boot entry
  - `config` Spring Security filter chain, CORS, JSON 401/403 handlers
  - `shared.{api,exception}` `ApiResponse` envelope, `GlobalExceptionHandler`
  - `health.{controller,service,repository,dto,model}` health slice; repository runs SQL with JDBC
