# BureauZero Platform

BureauZero contains a Spring Boot dashboard and a Python FastAPI/Playwright worker.

## Prerequisites

- Java 21
- Maven 3.8+
- Python 3.10+
- Internet access for the initial Python package and Playwright browser installation

## Configure secrets

Copy `.env.example` to `.env` in the repository root and set `OPENROUTER_API_KEY` to your own key. The Python worker loads this file automatically. Set `OPENROUTER_MODEL` to a model ID available in your OpenRouter account; the default `openrouter/free` selects an available free model. `.env` is ignored by Git; commit only `.env.example`.

Live myScheme discovery does not require an OpenRouter key. Document extraction and legacy DOB correction do. OpenRouter free models are rate limited; choose a paid model ID if the free route is unavailable or too slow.

## Setup the Python worker

**Windows PowerShell** (from the repository root):

```powershell
Copy-Item .env.example .env
notepad .env
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
./python-ai-agent/setup.ps1
```

**macOS / Linux** (from the repository root):

```sh
cp .env.example .env
# Edit .env and add your key if needed.
./python-ai-agent/setup.sh
```

## Run the application

Start the worker in one terminal:

```powershell
# Windows PowerShell
./python-ai-agent/run.ps1
```

```sh
# macOS / Linux
./python-ai-agent/run.sh
```

Start the Spring Boot dashboard in another terminal, from `spring-boot-core`:

```sh
mvn spring-boot:run
```

Open <http://localhost:8080>. The FastAPI worker listens on <http://127.0.0.1:8001>.

Set `PLAYWRIGHT_HEADLESS=false` in `.env` to show the browser during automation.

## Deploy on a VPS with Docker Compose

The `deploy` directory contains Docker images and a Compose stack for the Spring Boot app and Playwright worker. The H2 receipt database is stored in the named `bureauzero-data` volume. The Python worker is reachable only on the Compose network; only the dashboard port is published.

On a Linux VPS with Docker Engine and the Compose plugin installed:

```sh
git clone <your-repository-url> bureauzero-platform
cd bureauzero-platform
cp deploy/.env.example deploy/.env
```

Edit `deploy/.env` and set `OPENROUTER_API_KEY` to enable document extraction and DOB correction. Set `OPENROUTER_MODEL` if you want a particular model. Live myScheme discovery and the demo claim workflow can run without it. Then build and start the stack:

```sh
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --build
docker compose --env-file deploy/.env -f deploy/compose.yaml logs -f
```

Open `http://<your-vps-ip>:8080` (or the port set by `APP_PORT`). For a public site, put a TLS reverse proxy such as Caddy or Nginx in front of the app and allow inbound HTTPS in the VPS firewall. Do not publish port 8001 or enable the H2 console. The default H2 database is suitable for a single app instance; back up the `bureauzero-data` Docker volume regularly. For multi-instance deployments, use a managed PostgreSQL database instead of H2.

To update after pushing code:

```sh
git pull
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --build
```

## Deploy on Railway

For a hosted hackathon demo, Railway can run the dashboard and Playwright worker as separate services. Follow [railway/README.md](railway/README.md) to create the two services, configure their private URLs, attach persistent storage for receipts, and generate the dashboard’s public URL. The Railway project configuration is in [.railway/railway.ts](.railway/railway.ts).
