# Device Management API

A RESTful API built with **Spring Boot 3** for managing device resources.  
The system supports full lifecycle operations — creation, updates, filtering, and deletion — with strict business rule enforcement.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Build tool | Gradle 8.x |
| Framework | Spring Boot 3.5.14 |
| Web | Spring Web (MVC) |
| Persistence | Spring Data JPA + Hibernate |
| Database (prod) | PostgreSQL 16 |
| Database (dev/test) | H2 (file-based / in-memory) |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI 2.8.17 (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5 + Mockito |
| Coverage | JaCoCo 0.8.12 |
| Security scanning | OWASP Dependency-Check |
| Containerisation | Docker + Docker Compose |

---

## Domain Model

A Device contains:

| Field | Type | Notes |
|---|---|---|
| `id` | Long | Auto-generated, immutable |
| `name` | String | Required |
| `brand` | String | Required |
| `state` | Enum | `AVAILABLE`, `IN_USE`, `INACTIVE` |
| `createdAt` | LocalDateTime | System-generated, immutable |
| `version` | Long | Managed by JPA for optimistic locking |

---

## Business Rules

- `createdAt` is set by the system on creation and **cannot be updated**
- `name` and `brand` **cannot be updated** if the device is `IN_USE`
- Devices in `IN_USE` state **cannot be deleted**
- `brand` and `state` query filters are **mutually exclusive** — only one may be used per request
- Concurrent updates are protected via **optimistic locking** — include `version` in PUT/PATCH to detect stale updates

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/device` | Create a new device |
| `GET` | `/api/v1/device` | Get all devices |
| `GET` | `/api/v1/device?brand=Apple` | Filter devices by brand |
| `GET` | `/api/v1/device?state=AVAILABLE` | Filter devices by state |
| `GET` | `/api/v1/device/{id}` | Get device by ID |
| `PUT` | `/api/v1/device/{id}` | Fully update a device |
| `PATCH` | `/api/v1/device/{id}` | Partially update a device |
| `DELETE` | `/api/v1/device/{id}` | Delete a device |

### Example — Create (POST)

```json
{
  "name": "iPhone 15",
  "brand": "Apple",
  "state": "AVAILABLE"
}
```

### Example — Full update (PUT)

Include `version` from the previous GET response to enable optimistic locking.  
If the record was modified by another request since you fetched it, you will receive **409 CONCURRENT_MODIFICATION**.

```json
{
  "name": "iPhone 15 Pro",
  "brand": "Apple",
  "state": "IN_USE",
  "version": 2
}
```

### Example — Partial update (PATCH)

All fields are optional. Only provided fields are changed.  
Include `version` to opt into optimistic locking on a partial update.

```json
{ "state": "INACTIVE", "version": 2 }
```

---

## Optimistic Locking

Every device response includes a `version` field. This is used to detect concurrent modifications:

1. `GET /api/v1/device/1` → `{ ..., "version": 2 }`
2. `PUT /api/v1/device/1` with `{ ..., "version": 2 }` → succeeds, response contains `"version": 3`
3. If another client already updated the same device → **409 CONCURRENT_MODIFICATION**

Omitting `version` from PUT/PATCH skips the check — the update is applied unconditionally.

---

## Project Profiles

| Profile | Purpose | Database | Schema management |
|---|---|---|---|
| *(default)* | Production / Docker | PostgreSQL | Flyway migrations (`validate`) |
| `dev` | Local development | H2 file-based (`./data/devicedb`) | `ddl-auto=update` (persists between restarts) |
| `test` | Automated tests | H2 in-memory | `ddl-auto=create-drop` (fresh DB per run) |

---

## Prerequisites

- Java 21+
- Gradle 8+ (or use the wrapper: `./gradlew`)
- Docker + Docker Compose (for PostgreSQL or full-stack runs)

---

## Running Locally

### Option A — H2 dev profile (no PostgreSQL needed)

This is the easiest way to run locally without Docker or a PostgreSQL installation.

```bash
  ./gradlew bootRun --args='--spring.profiles.active=dev'
```

The app starts on **http://localhost:8080** and stores data in `./data/devicedb.mv.db`.  
Data **persists between restarts** — the file is only recreated if you delete the `data/` folder.

> **First run after adding a new entity field:** If you see a column-not-found error, delete the stale file and restart:
> ```bash
> rm -rf data/
> ./gradlew bootRun --args='--spring.profiles.active=dev'
> ```

#### H2 Console

Available only with the `dev` profile at **http://localhost:8080/h2-console**

| Field | Value |
|---|---|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:file:./data/devicedb` |
| User Name | `sa` |
| Password | `password` |

> ⚠️ The console pre-fills `jdbc:h2:~/test` by default. You **must** clear it and type `jdbc:h2:file:./data/devicedb` exactly — any other URL connects to a different empty database.

> ⚠️ The H2 console is **disabled** in the production profile.

### Option B — PostgreSQL with Docker (default profile)

```bash
  # 1. Start PostgreSQL
  docker run -d \
    --name postgres-local \
    -e POSTGRES_DB=devicedb \
    -e POSTGRES_USER=sa \
    -e POSTGRES_PASSWORD=password \
    -p 5432:5432 \
    postgres:16-alpine

  # 2. Run the app
  ./gradlew bootRun
```

Flyway migrations run automatically on startup. To stop the database:

```bash
  docker stop postgres-local && docker rm postgres-local
```

---

## Running with Docker Compose (Full Stack — Recommended)

### Start

```bash
  docker compose up --build
```

This will: build the Spring Boot image, start PostgreSQL with a persistent volume, wait for the DB healthcheck, run Flyway migrations, and expose the app at **http://localhost:8080**.

### Stop

```bash
  docker compose down
```

Remove the database volume (destroys all data):

```bash
  docker compose down -v
```

### Custom DB password

```bash
  DB_PASSWORD=mysecurepassword docker compose up --build
```

Or create a `.env` file in the project root (do **not** commit this):

```env
DB_PASSWORD=mysecurepassword
```

### Rancher Desktop

1. **Preferences → Container Engine** → select **`dockerd (moby)`**
2. All `docker` and `docker compose` commands work identically
3. On **Windows**, ensure the project folder is under `C:\Users\` for volume mounts

---

## Building the Docker Image Manually

```bash
  # Build
  docker build -t device-management .

  # Run against an external PostgreSQL
  docker run -d \
    -p 8080:8080 \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/devicedb \
    -e SPRING_DATASOURCE_USERNAME=sa \
    -e DB_PASSWORD=password \
    --name device-management-container \
    device-management:latest
```

> On Linux, replace `host.docker.internal` with your machine's IP, or use `--network host`.

```bash
  # Run with H2
  docker run -d \
    -p 8080:8080 \
    -v $(pwd)/data:/data \
    -v $(pwd)/logs:/logs \
    -e DB_PASSWORD=your_secure_db_pass \
    -e ADMIN_PASSWORD=your_api_pass \
    --name device-management-container \
    device-management:latest
```

> If you are on Windows, ensure the path `$(pwd)/data` is within a directory that Rancher Desktop has permission to share (usually your `C:\Users` folder).

---

## Running Tests

```bash
  ./gradlew test
```

Tests use the `test` profile automatically (H2 in-memory, fresh schema per test). No external database is needed.

Run tests and generate all reports:

```bash
  ./gradlew clean test jacocoTestReport
```

---

## Reports

After running `./gradlew clean test jacocoTestReport`, the following reports are generated locally:

| Report | Path |
|---|---|
| **Test results** | `build/reports/tests/test/index.html` |
| **JaCoCo coverage** | `build/reports/jacoco/test/html/index.html` |
| **Build problems** | `build/reports/problems/problems-report.html` |
| **OWASP CVE scan** | `build/reports/dependency-check-report.html` |

Open any of them directly in your browser:

```bash
  # macOS
  open build/reports/tests/test/index.html
  open build/reports/jacoco/test/html/index.html

  # Linux
  xdg-open build/reports/tests/test/index.html

  # Windows (PowerShell)
  start build/reports/tests/test/index.html
```

---

## API Documentation (Swagger UI)

Once the application is running:

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

---

## Health & Monitoring (Actuator)

Spring Boot Actuator is included and exposes the following endpoints:

| Endpoint | URL | Description |
|---|---|---|
| Health | http://localhost:8080/actuator/health | App + DB connectivity status |
| Info | http://localhost:8080/actuator/info | Application name and version |

The `health` endpoint is also used by Docker's `HEALTHCHECK` directive to determine when the container is ready to serve traffic. During `docker compose up`, the app container waits for this endpoint before accepting requests.

Example response:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## Exception Handling

All errors return a structured `ApiError` body:

```json
{
  "code": "DEVICE_IN_USE",
  "message": "Device 3 cannot be modified while IN_USE",
  "status": 409,
  "path": "/api/v1/device/3",
  "timestamp": "2025-01-01T12:00:00"
}
```

| Status | Code | Trigger |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Missing/invalid request fields |
| 400 | `INVALID_STATE` | Unknown enum value for state |
| 400 | `FILTER_CONFLICT` | Both `brand` and `state` filters used together |
| 404 | `DEVICE_NOT_FOUND` | Device ID does not exist |
| 409 | `DEVICE_IN_USE` | Update/delete blocked by business rule |
| 409 | `CONCURRENT_MODIFICATION` | Version mismatch — record was updated by another request |
| 500 | `INTERNAL_ERROR` | Unexpected server error |

---

## CVE & Dependency Security

```bash
  ./gradlew dependencyCheckAnalyze
```

The build fails if any dependency has a CVSS score ≥ 7. Report: `build/reports/dependency-check-report.html`.

Suppress confirmed false positives in `owasp-suppressions.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <suppress>
        <notes>commons-lang3 via swagger-core-jakarta — not reachable in this application</notes>
        <gav regex="true">^org\.apache\.commons:commons-lang3:.*$</gav>
    </suppress>
</suppressions>
```

---

## Key Design Decisions

- **DTO-based API** — `DeviceRequest` / `DevicePatchRequest` / `DeviceResponse` keep the API contract separate from the persistence model; entities are never exposed directly
- **Typed exception hierarchy** — `DeviceException` base class + `ErrorCode` enum drives both HTTP status mapping and the `ApiError` response body
- **`@Transactional(readOnly = true)` by default** — write methods individually override with `@Transactional`
- **Flyway migrations** — schema is version-controlled; `ddl-auto=validate` in production catches schema drift without silently altering tables
- **Optimistic locking** — `@Version` on `Device` prevents lost updates; clients receive `version` in every response and submit it back on mutation requests
- **Non-root Docker user** — container runs as `appuser`, reducing attack surface
- **Container-aware JVM flags** — `-XX:+UseContainerSupport` and `-XX:MaxRAMPercentage=75.0` respect container memory limits

---

## Future Improvements

The items below are out of scope for this implementation but represent the natural next steps for a production service.

### Pagination and Sorting *(high value, low effort)*

The current `GET /api/v1/device` returns all records unbounded. For large datasets this needs `Pageable`:

```java
Page<Device> findAll(Pageable pageable);
// Usage: GET /api/v1/device?page=0&size=20&sort=brand,asc
```

### Authentication — Spring Security + JWT *(required before any public deployment)*

All write endpoints should be protected. Read endpoints can remain public or role-gated:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/api/v1/device/**").permitAll()
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .anyRequest().authenticated())
```

### Caching — Redis *(high value for read-heavy workloads)*

```java
@Cacheable(value = "devices", key = "#id")
public Device getById(Long id) { ... }

@CacheEvict(value = "devices", key = "#id")
public void deleteDevice(Long id) { ... }
```

### Audit Logging *(compliance requirement in many environments)*

Track who created and last modified each record using Spring Data's auditing:

```java
@CreatedBy   private String createdBy;
@LastModifiedBy private String lastModifiedBy;
@LastModifiedDate private LocalDateTime updatedAt;
```

### Actuator Hardening *(recommended before production)*

Move the management port to an internal-only network interface so metrics and health details are not publicly accessible:

```properties
management.server.port=9090
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
```

### Event-Driven State Changes — Kafka *(for microservices / audit trail)*

Publish a domain event on every state change so downstream services (audit log, notifications, analytics) can react without coupling to this service:

```java
public record DeviceStateChangedEvent(Long deviceId, DeviceState from, DeviceState to, Instant at) {}
```

---

## Author

Device Management - built for technical assessment and production readiness demonstration.