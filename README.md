# Amazon Product Intelligence API

Spring Boot backend that crawls Amazon product pages with Playwright, stores price history in Neon PostgreSQL, and exposes REST APIs for a dashboard and admin panel.

**Live app:** https://amazoncrawler.netlify.app  
**Live API:** https://amazoncrawlerbackend.onrender.com  
**Frontend repo:** https://github.com/HarshMishra3007/amazonCrawlerFrontend  
**Backend repo:** https://github.com/HarshMishra3007/amazonCrawlerBackend  

**Project overview (architecture + production notes):** [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)

## Prerequisites

- Java 17+
- Maven 3.9+
- [Neon](https://neon.tech) PostgreSQL database

## Local development

### 1. Configure environment

Copy `.env.example` to `.env` and fill in your Neon JDBC credentials:

```bash
cp .env.example .env
```

Neon gives a URL like `postgresql://user:pass@host/neondb?sslmode=require`. Convert to JDBC:

```
DB_URL=jdbc:postgresql://host/neondb?sslmode=require&channel_binding=require
DB_USER=your_user
DB_PASSWORD=your_password
```

### 2. Install Playwright + run

```bash
./install-playwright.sh   # one-time Chromium install
./run.sh                  # loads .env and starts the API
```

API runs at `http://localhost:8080`.

## Production deployment (Render / Docker)

The app ships with a **Dockerfile** based on Microsoft's Playwright Java image — Chromium and system dependencies are included in the container. No separate browser install step on the host.

### Deploy to Render

1. Push this repo to GitHub.
2. Create a **New Web Service** on [Render](https://render.com) → connect repo.
3. Render detects `render.yaml` automatically (Docker runtime).
4. Set these **secret** environment variables in the Render dashboard:

| Variable | Value |
|---|---|
| `DB_URL` | Neon JDBC URL (`jdbc:postgresql://...?sslmode=require`) |
| `DB_USER` | Neon username |
| `DB_PASSWORD` | Neon password |
| `ADMIN_USERNAME` | Admin login (change from default) |
| `ADMIN_PASSWORD` | Strong password |
| `CORS_ALLOWED_ORIGINS` | `https://amazoncrawler.netlify.app,http://localhost:5173` |

5. Deploy. Render health-checks `/actuator/health`.

`SPRING_PROFILES_ACTIVE=prod` is set automatically in the Dockerfile.

### Deploy with Docker (any host)

```bash
docker build -t product-intelligence-api .
docker run -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://..." \
  -e DB_USER="..." \
  -e DB_PASSWORD="..." \
  -e ADMIN_USERNAME="admin" \
  -e ADMIN_PASSWORD="..." \
  -e CORS_ALLOWED_ORIGINS="https://your-frontend.com" \
  product-intelligence-api
```

### Production notes

- **Neon pooler URL** (`-pooler` in hostname) is recommended for serverless/cloud hosts.
- **Memory**: allocate at least **512 MB–1 GB** — Playwright/Chromium needs headroom.
- **CORS**: set `CORS_ALLOWED_ORIGINS` to your deployed frontend origin.
- Flyway runs migrations automatically on first startup.

## Seed product IDs

```bash
curl -u admin:admin -X POST http://localhost:8080/api/admin/products \
  -H "Content-Type: application/json" \
  -d '{"asin":"B0BDHWDR12"}'
```

Add a competitor linked to product id `1`:

```bash
curl -u admin:admin -X POST http://localhost:8080/api/admin/competitors \
  -H "Content-Type: application/json" \
  -d '{"asin":"B0C1H26C3W","ownProductId":1}'
```

## Trigger a manual crawl

```bash
curl -u admin:admin -X POST http://localhost:8080/api/admin/crawl
curl -u admin:admin -X POST http://localhost:8080/api/admin/crawl/1
curl -u admin:admin http://localhost:8080/api/admin/crawl/status
```

## Environment variables

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | Yes | Neon JDBC URL with `?sslmode=require` |
| `DB_USER` | Yes | Neon database user |
| `DB_PASSWORD` | Yes | Neon database password |
| `ADMIN_USERNAME` | No | Basic auth username (default: `admin`) |
| `ADMIN_PASSWORD` | No | Basic auth password (default: `admin`) |
| `CORS_ALLOWED_ORIGINS` | Prod | Comma-separated frontend origins |
| `CRAWL_CRON` | No | Scheduled crawl cron (default: every 6 hours) |
| `CRAWLER_DELAY_MS` | No | Delay between crawls (default: `2500`) |
| `CRAWLER_TIMEOUT_MS` | No | Page load timeout (default: `30000`) |
| `AMAZON_BASE_URL` | No | Amazon domain |
| `CRAWLER_PROXY_URL` | No | Optional HTTP proxy |
| `PORT` | No | Server port (default: `8080`, set by Render) |
| `SPRING_PROFILES_ACTIVE` | No | Set to `prod` in Docker/Render |

## API overview

### Public

- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/products/{id}/price-history`
- `GET /api/products/{id}/competitors`
- `GET /api/products/{id}/detail`
- `GET /actuator/health`

### Admin (Basic Auth)

- `POST /api/admin/products` / `bulk`
- `PUT /api/admin/products/{id}/asin`
- `DELETE /api/admin/products/{id}`
- `POST /api/admin/competitors` / `bulk`
- `PUT /api/admin/competitors/{linkId}`
- `DELETE /api/admin/competitors/{linkId}`
- `POST /api/admin/crawl`
- `POST /api/admin/crawl/{productId}`
- `GET /api/admin/crawl/status`

## Crawler architecture

- **PlaywrightAmazonCrawler** — headless Chromium via Playwright
- **PageParser** — CSS selector extraction with fallbacks
- **CrawlService** — batch orchestration, serial crawl lock, delay between products
- **ProductCrawlExecutor** — transactional snapshot persistence
- In production (`prod` profile), Chromium runs with `--no-sandbox` flags required for containers

## Amazon blocking and production crawl timeouts

Amazon may show CAPTCHA pages. The crawler marks products as `FAILED` with reason `BLOCKED` and retries on the next scheduled cycle.

### Intermittent timeouts in production

On Render/cloud hosts, crawls may fail with:

`Timeout exceeded waiting for #productTitle`

**Reasons:**

1. **Anti-bot traffic** — Cloud datacenter IPs are often flagged; Amazon serves CAPTCHA or pages without product title selectors.
2. **Resource limits** — Playwright/Chromium needs memory; Starter-tier instances can be slow.
3. **Scraping fragility** — No official Amazon API; DOM changes and blocking are expected.
4. **Retries** — Up to 3 attempts with backoff (`CRAWLER_RETRY_*`); not retried on explicit `BLOCKED`.

Tune `CRAWLER_TIMEOUT_MS` (e.g. `60000`–`90000`). For higher reliability: proxy (`CRAWLER_PROXY_URL`), more RAM (EC2), or Amazon SP-API.
